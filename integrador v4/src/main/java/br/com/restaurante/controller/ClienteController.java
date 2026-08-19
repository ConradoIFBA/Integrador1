package br.com.restaurante.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import br.com.restaurante.dao.FilaPreparoDAO;
import br.com.restaurante.dao.CardapioDAO;
import br.com.restaurante.dao.ItemPedidoDAO;
import br.com.restaurante.dao.MesaDAO;
import br.com.restaurante.dao.PedidoDAO;
import br.com.restaurante.model.FilaPreparo;
import br.com.restaurante.model.Cardapio;
import br.com.restaurante.model.ItemPedido;
import br.com.restaurante.model.Mesa;
import br.com.restaurante.model.Pedido;
import br.com.restaurante.model.Usuario;
import br.com.restaurante.utils.Conexao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * ================================================================
 * CLIENTE CONTROLLER - Área exclusiva do perfil USUARIO (cliente)
 * ================================================================
 *
 * PROPÓSITO:
 * Concentra tudo que o cliente final (perfil USUARIO) pode fazer
 * pelo app: pedir delivery, reservar mesa e acompanhar seus pedidos.
 * GERENTE e FUNCIONARIO não usam este controller (eles usam
 * PedidoController e MesaController, com fluxos próprios).
 *
 * FUNCIONALIDADES:
 * 1. Exibir e confirmar pedido de delivery
 * 2. Exibir mesas livres e confirmar reserva
 * 3. Listar os pedidos já feitos pelo cliente logado
 *
 * ROTAS MAPEADAS:
 *   GET  /app/cliente/delivery      → formulário de pedido delivery
 *   POST /app/cliente/delivery      → confirma pedido delivery
 *   GET  /app/cliente/reserva       → lista mesas livres para reservar
 *   POST /app/cliente/reserva       → confirma reserva
 *   GET  /app/cliente/meus-pedidos  → pedidos do cliente logado
 *
 * TABELAS ENVOLVIDAS:
 * - pedido        (tipo='delivery', identificador_operador = nome do cliente)
 * - item_pedido    (itens escolhidos no delivery)
 * - cardapio       (para validar disponibilidade e pegar o preço atual)
 * - fila_preparo   (todo pedido novo entra na fila de preparo)
 * - mesa           (para reserva)
 *
 * PERMISSÕES:
 * ✅ TODAS as rotas deste controller exigem perfil USUARIO — a
 *    checagem é feita logo no início de doGet()/doPost() via
 *    isCliente(). Qualquer outro perfil é redirecionado para o login.
 *
 * IMPORTANTE — IDENTIFICAÇÃO DO CLIENTE:
 * Como não existe uma tabela de "cliente" separada, os pedidos e
 * reservas feitos pelo app são identificados pelo NOME do usuário
 * logado (usuario.getNome()), gravado na coluna
 * identificador_operador (pedido) ou operador (mesa). É esse nome
 * que depois é usado para filtrar "Meus Pedidos".
 *
 * FLUXO DE DELIVERY (confirmarDelivery):
 * 1. Lê observação + listas paralelas de itemId/quantidade do form
 * 2. Valida que pelo menos um item foi enviado
 * 3. Abre transação (autoCommit=false)
 * 4. Cria o Pedido (tipo=delivery, status=aberto)
 * 5. Para cada item: busca no cardápio, valida disponibilidade,
 *    monta o ItemPedido com o preço ATUAL do cardápio (nunca confia
 *    em preço vindo do formulário) e rastreia o maior tempo de
 *    preparo + setor correspondente (usado na fila)
 * 6. Se nenhum item válido sobrou → rollback e erro
 * 7. Insere os itens em lote
 * 8. Cria a entrada na fila de preparo (prioridade normal = 1)
 * 9. Commit e mensagem de sucesso
 *
 * FLUXO DE RESERVA (confirmarReserva):
 * 1. Busca a mesa pelo id
 * 2. Só permite reservar se a mesa estiver "livre"
 * 3. Atualiza o status da mesa para "reservada", registrando o nome
 *    do cliente como operador
 *
 * FLUXO DE MEUS PEDIDOS (exibirMeusPedidos):
 * 1. Busca todos os pedidos cujo identificador_operador == nome do
 *    cliente logado
 * 2. Para cada pedido, carrega os itens (para exibir detalhe)
 *
 * EXEMPLO DE USO:
 * ```
 * // Ver formulário de delivery:
 * GET /app/cliente/delivery
 *
 * // Confirmar pedido delivery:
 * POST /app/cliente/delivery
 * observacao=sem cebola&itemId=3&quantidade=2&itemId=7&quantidade=1
 *
 * // Ver mesas livres:
 * GET /app/cliente/reserva
 *
 * // Confirmar reserva:
 * POST /app/cliente/reserva
 * id=4
 *
 * // Ver meus pedidos:
 * GET /app/cliente/meus-pedidos
 * ```
 *
 * @author Sistema Integrador
 * @version 3.0
 * @see PedidoDAO
 * @see ItemPedidoDAO
 * @see FilaPreparoDAO
 * @see MesaDAO
 * @see CardapioDAO
 */
@WebServlet({
    "/app/cliente/delivery",
    "/app/cliente/reserva",
    "/app/cliente/meus-pedidos",
    "/app/cliente/mesa"
})
public class ClienteController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // ── GET ─────────────────────────────────────────────────────────

    /* ================================================================
       MÉTODO GET - Roteador de Páginas
       ================================================================

       Todas as três rotas GET exigem perfil USUARIO. Se o usuário
       logado não for cliente (ou não estiver logado), é mandado
       direto para a tela de login.

       /app/cliente/delivery      → exibirDelivery()
       /app/cliente/reserva       → exibirReserva()
       /app/cliente/meus-pedidos  → exibirMeusPedidos()
    */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("\n========== CLIENTE CONTROLLER GET ==========");
        System.out.println("📍 Rota acessada: " + request.getServletPath());

        // ========== VERIFICAR PERMISSÃO (só USUARIO/cliente) ==========
        if (!isCliente(request)) {
            System.err.println("❌ Acesso negado: usuário não é cliente (USUARIO) ou não está logado");
            response.sendRedirect(request.getContextPath() + "/auth/login");
            System.out.println("==============================================\n");
            return;
        }

        switch (request.getServletPath()) {
            case "/app/cliente/delivery"     -> {
                System.out.println("🔀 Roteando para: exibirDelivery()");
                exibirDelivery(request, response);
            }
            case "/app/cliente/reserva"      -> {
                System.out.println("🔀 Roteando para: exibirReserva()");
                exibirReserva(request, response);
            }
            case "/app/cliente/meus-pedidos" -> {
                System.out.println("🔀 Roteando para: exibirMeusPedidos()");
                exibirMeusPedidos(request, response);
            }
            case "/app/cliente/mesa" -> {
                System.out.println("🔀 Roteando para: exibirMesa()");
                exibirMesa(request, response);
            }
            default -> {
                System.err.println("❌ Rota GET desconhecida: " + request.getServletPath());
                response.sendError(404);
            }
        }
        System.out.println("==============================================\n");
    }

    // ── POST ────────────────────────────────────────────────────────

    /* ================================================================
       MÉTODO POST - Roteador de Ações
       ================================================================

       Assim como no GET, toda ação POST exige perfil USUARIO.

       /app/cliente/delivery → confirmarDelivery()
       /app/cliente/reserva  → confirmarReserva()

       (não há POST para /app/cliente/meus-pedidos — é só leitura)
    */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("\n========== CLIENTE CONTROLLER POST ==========");
        System.out.println("📍 Rota acessada: " + request.getServletPath());

        // ========== VERIFICAR PERMISSÃO (só USUARIO/cliente) ==========
        if (!isCliente(request)) {
            System.err.println("❌ Acesso negado: usuário não é cliente (USUARIO) ou não está logado");
            response.sendRedirect(request.getContextPath() + "/auth/login");
            System.out.println("===============================================\n");
            return;
        }

        switch (request.getServletPath()) {
            case "/app/cliente/delivery" -> {
                System.out.println("🔀 Roteando para: confirmarDelivery()");
                confirmarDelivery(request, response);
            }
            case "/app/cliente/reserva"  -> {
                System.out.println("🔀 Roteando para: confirmarReserva()");
                confirmarReserva(request, response);
            }
            case "/app/cliente/mesa" -> {
                // Esta rota tem DUAS ações POST possíveis (chamar garçom
                // ou confirmar pedido direto) — diferente das outras
                // rotas deste controller (que têm só uma ação POST
                // cada), então precisa de um roteamento interno extra
                // pelo parâmetro "acao", no mesmo padrão já usado em
                // CardapioController/PedidoController/MesaController.
                String acaoMesa = request.getParameter("acao");
                System.out.println("🔀 Sub-ação de /app/cliente/mesa: " + acaoMesa);
                if ("chamarGarcom".equals(acaoMesa)) {
                    chamarGarcom(request, response);
                } else if ("pedidoDireto".equals(acaoMesa)) {
                    confirmarPedidoDireto(request, response);
                } else {
                    System.err.println("❌ Sub-ação desconhecida em /app/cliente/mesa: " + acaoMesa);
                    response.sendRedirect(request.getContextPath() + "/app/cliente/mesa");
                }
            }
            default -> {
                System.err.println("❌ Rota POST desconhecida: " + request.getServletPath());
                response.sendError(404);
            }
        }
        System.out.println("===============================================\n");
    }

    // ── DELIVERY: exibir formulário ──────────────────────────────────

    /* ================================================================
       EXIBIR FORMULÁRIO DE DELIVERY
       ================================================================

       URL: GET /app/cliente/delivery

       Carrega todo o cardápio (a JSP filtra visualmente por
       disponibilidade/categoria) e encaminha para delivery.jsp.
    */
    private void exibirDelivery(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("📝 Exibindo formulário de delivery");

        try (Connection conn = Conexao.getConnection()) {
            System.out.println("⏳ Buscando itens do cardápio...");
            List<Cardapio> itens = new CardapioDAO(conn).listar();
            System.out.println("✅ " + itens.size() + " item(ns) carregado(s)");

            request.setAttribute("itens",      itens);
            request.setAttribute("paginaAtiva","delivery");
            request.getRequestDispatcher("/WEB-INF/views/cliente/delivery.jsp")
                   .forward(request, response);
        } catch (Exception e) {
            System.err.println("❌ ERRO ao exibir delivery: " + e.getMessage());
            e.printStackTrace();
            request.getRequestDispatcher("/WEB-INF/views/error/500.jsp")
                   .forward(request, response);
        }
    }

    // ── DELIVERY: confirmar pedido ───────────────────────────────────

    /* ================================================================
       CONFIRMAR PEDIDO DE DELIVERY
       ================================================================

       URL: POST /app/cliente/delivery

       Parâmetros:
       - observacao   (opcional, texto livre)
       - itemId[]     (array paralelo — um id de item por linha do carrinho)
       - quantidade[] (array paralelo — quantidade correspondente a cada itemId)

       Fluxo detalhado:
       STEP 1 — Validação inicial: precisa de pelo menos 1 item.
       STEP 2 — Transação manual (autoCommit=false) porque a operação
                envolve 3 tabelas (pedido, item_pedido, fila_preparo)
                que precisam ser consistentes entre si.
       STEP 3 — Cria o Pedido "casca" (sem itens ainda) para obter o
                pedidoId gerado pelo banco.
       STEP 4 — Para cada linha do carrinho:
                a) busca o item no cardápio pelo id
                b) ignora silenciosamente itens inexistentes ou
                   marcados como indisponíveis (proteção contra
                   carrinho desatualizado / manipulação do form)
                c) usa o PREÇO ATUAL do cardápio (nunca o que veio
                   do form) — evita que o cliente manipule o preço
                d) rastreia o maior tempo de preparo entre os itens
                   e o setor correspondente, para dimensionar a fila
       STEP 5 — Se depois do filtro não sobrou nenhum item válido,
                desfaz tudo (rollback) e avisa o cliente.
       STEP 6 — Insere todos os itens válidos em lote.
       STEP 7 — Cria a entrada na fila de preparo com prioridade
                normal (peso=1 — delivery não tem urgência configurável
                pelo cliente, diferente do pedido de mesa feito por
                funcionário).
       STEP 8 — Commit e mensagem de sucesso.
       STEP 9 — Em caso de qualquer exceção durante a transação:
                rollback + mensagem de erro genérica.

       Redireciona sempre para "Meus Pedidos" ao final (sucesso ou erro).
    */
    private void confirmarDelivery(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        System.out.println("🛵 Iniciando confirmação de pedido delivery");

        Usuario usuario   = (Usuario) request.getSession().getAttribute("usuarioLogado");
        String observacao = request.getParameter("observacao");
        String[] itemIds  = request.getParameterValues("itemId");
        String[] qtds     = request.getParameterValues("quantidade");

        System.out.println("📋 Dados recebidos:");
        System.out.println("   - Cliente: " + usuario.getNome());
        System.out.println("   - Observação: " + observacao);
        System.out.println("   - Qtd. de linhas no carrinho: " + (itemIds != null ? itemIds.length : 0));

        // ========== STEP 1: VALIDAR CARRINHO NÃO VAZIO ==========
        if (itemIds == null || itemIds.length == 0) {
            System.err.println("❌ Carrinho vazio — nenhum itemId recebido");
            request.getSession().setAttribute("msgErro", "Adicione pelo menos um item.");
            response.sendRedirect(request.getContextPath() + "/app/cliente/delivery");
            return;
        }

        try (Connection conn = Conexao.getConnection()) {
            // ========== STEP 2: INICIAR TRANSAÇÃO MANUAL ==========
            conn.setAutoCommit(false);
            try {
                // ========== STEP 3: CRIAR O PEDIDO (CASCA) ==========
                System.out.println("⏳ Criando pedido (tipo=delivery)...");
                Pedido pedido = new Pedido();
                pedido.setTipo("delivery");
                pedido.setUrgente(false);
                pedido.setIdentificadorOperador(usuario.getNome()); // cliente identificado pelo nome
                pedido.setObservacao(observacao);
                pedido.setStatus("aberto");
                pedido.setAtivo(true);

                new PedidoDAO(conn).inserir(pedido);
                int pedidoId = pedido.getIdPedido();
                System.out.println("✅ Pedido criado com id=" + pedidoId);

                // ========== STEP 4: MONTAR OS ITENS DO PEDIDO ==========
                List<ItemPedido> itensPedido = new ArrayList<>();
                int tempoMax = 0;
                String setor = "cozinha"; // valor padrão caso nenhum item defina categoria
                CardapioDAO icDao = new CardapioDAO(conn);

                for (int i = 0; i < itemIds.length; i++) {
                    int itemId = parseId(itemIds[i]);
                    int qtd    = (qtds != null && i < qtds.length) ? parseId(qtds[i]) : 1;
                    if (qtd <= 0) qtd = 1; // proteção contra quantidade inválida/negativa

                    // ---- Busca o item no cardápio e valida disponibilidade ----
                    Cardapio ic = icDao.buscarPorId(itemId);
                    if (ic == null || !ic.isDisponivel()) {
                        System.out.println("⚠️ Item id=" + itemId + " ignorado (inexistente ou indisponível)");
                        continue;
                    }

                    ItemPedido ip = new ItemPedido();
                    ip.setPedidoId(pedidoId);
                    ip.setItemCardapioId(itemId);
                    ip.setQuantidade(qtd);
                    ip.setPrecoUnitario(ic.getPreco()); // sempre o preço ATUAL do cardápio
                    ip.setStatus("pendente");
                    ip.setAtivo(true);
                    itensPedido.add(ip);

                    // ---- Rastreia o item mais demorado para dimensionar a fila ----
                    if (ic.getTempoPreparoMin() > tempoMax) {
                        tempoMax = ic.getTempoPreparoMin();
                        setor    = ic.getCategoria() != null
                                   ? ic.getCategoria().getSetor() : "cozinha";
                    }
                }

                // ========== STEP 5: VALIDAR SE SOBROU ALGUM ITEM VÁLIDO ==========
                if (itensPedido.isEmpty()) {
                    System.err.println("❌ Nenhum item válido após filtragem — desfazendo transação");
                    conn.rollback();
                    request.getSession().setAttribute("msgErro", "Nenhum item válido.");
                    response.sendRedirect(request.getContextPath() + "/app/cliente/delivery");
                    return;
                }

                // ========== STEP 6: INSERIR ITENS EM LOTE ==========
                System.out.println("⏳ Inserindo " + itensPedido.size() + " item(ns) em lote...");
                new ItemPedidoDAO(conn).inserirLote(itensPedido, pedidoId);

                // ========== STEP 7: ENTRAR NA FILA DE PREPARO ==========
                // Prioridade normal (peso=1) — delivery de cliente não tem opção de "urgente"
                System.out.println("⏳ Adicionando pedido à fila de preparo (setor=" + setor + ")...");
                FilaPreparo fila = new FilaPreparo();
                fila.setPedidoId(pedidoId);
                fila.setPesoPrioridade(1);
                fila.setTempoEstimadoMin(tempoMax);
                fila.setSetor(setor);
                fila.setAtivo(true);
                new FilaPreparoDAO(conn).inserir(fila);

                // ========== STEP 8: COMMIT ==========
                conn.commit();
                System.out.println("✅ PEDIDO #" + pedidoId + " CONFIRMADO COM SUCESSO!");
                request.getSession().setAttribute("msgSucesso",
                    "Pedido #" + pedidoId + " realizado! Acompanhe em Meus Pedidos.");

            } catch (Exception e) {
                // ========== STEP 9: ROLLBACK EM CASO DE ERRO NA TRANSAÇÃO ==========
                conn.rollback();
                System.err.println("❌ ERRO durante a transação — rollback executado:");
                System.err.println("   " + e.getMessage());
                e.printStackTrace();
                request.getSession().setAttribute("msgErro", "Erro ao realizar o pedido.");
            }
        } catch (Exception e) {
            System.err.println("❌ ERRO ao obter conexão: " + e.getMessage());
            e.printStackTrace();
        }

        response.sendRedirect(request.getContextPath() + "/app/cliente/meus-pedidos");
    }

    // ── RESERVA: exibir mesas livres ────────────────────────────────

    /* ================================================================
       EXIBIR MESAS PARA RESERVA
       ================================================================

       URL: GET /app/cliente/reserva

       Fluxo:
       1. Lista TODAS as mesas (a JSP decide visualmente quais estão
          livres/ocupadas/reservadas — o filtro de "só livres" fica
          a cargo da view, aqui trazemos tudo para dar contexto)
       2. Recupera e limpa mensagens de sucesso/erro da sessão
          (padrão POST-REDIRECT-GET)
    */
    private void exibirReserva(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("📝 Exibindo mesas disponíveis para reserva");

        try (Connection conn = Conexao.getConnection()) {
            List<Mesa> mesas = new MesaDAO(conn).listar();
            System.out.println("✅ " + mesas.size() + " mesa(s) carregada(s)");

            // ---- Recupera mensagens (sucesso/erro) da sessão ----
            String msg = (String) request.getSession().getAttribute("msgSucesso");
            if (msg != null) {
                System.out.println("💬 Mensagem de sucesso encontrada: " + msg);
                request.setAttribute("msgSucesso", msg);
                request.getSession().removeAttribute("msgSucesso");
            }
            String erro = (String) request.getSession().getAttribute("msgErro");
            if (erro != null) {
                System.out.println("💬 Mensagem de erro encontrada: " + erro);
                request.setAttribute("msgErro", erro);
                request.getSession().removeAttribute("msgErro");
            }

            request.setAttribute("mesas",      mesas);
            request.setAttribute("paginaAtiva","reserva");
            request.getRequestDispatcher("/WEB-INF/views/cliente/reserva.jsp")
                   .forward(request, response);

        } catch (Exception e) {
            System.err.println("❌ ERRO ao exibir reserva: " + e.getMessage());
            e.printStackTrace();
            request.getRequestDispatcher("/WEB-INF/views/error/500.jsp")
                   .forward(request, response);
        }
    }

    // ── RESERVA: confirmar ───────────────────────────────────────────

    /* ================================================================
       CONFIRMAR RESERVA DE MESA
       ================================================================

       URL: POST /app/cliente/reserva

       Parâmetro:
       - id: id da mesa a reservar

       Fluxo:
       1. Valida o id recebido
       2. Busca a mesa e confere se o status atual é "livre" — só é
          possível reservar mesas livres (evita reservar mesa já
          ocupada ou já reservada por outro cliente)
       3. Se OK, atualiza o status para "reservada", registrando o
          NOME do cliente logado como operador (rastreabilidade)
       4. Mensagens de sucesso/erro são setadas na sessão e consumidas
          na próxima exibição de exibirReserva()
    */
    private void confirmarReserva(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int id = parseId(request.getParameter("id"));
        System.out.println("🪑 Iniciando confirmação de reserva — mesa id=" + id);

        if (id <= 0) {
            System.err.println("❌ id de mesa inválido");
            response.sendRedirect(request.getContextPath() + "/app/cliente/reserva");
            return;
        }

        Usuario usuario = (Usuario) request.getSession().getAttribute("usuarioLogado");

        try (Connection conn = Conexao.getConnection()) {
            Mesa mesa = new MesaDAO(conn).buscarPorId(id);

            // ---- Só permite reservar mesa que está livre ----
            if (mesa == null || !"livre".equals(mesa.getStatus())) {
                System.err.println("❌ Mesa indisponível para reserva (status atual: "
                        + (mesa != null ? mesa.getStatus() : "não encontrada") + ")");
                request.getSession().setAttribute("msgErro",
                    "Esta mesa não está disponível para reserva.");
                response.sendRedirect(request.getContextPath() + "/app/cliente/reserva");
                return;
            }

            new MesaDAO(conn).atualizarStatus(id, "reservada", usuario.getNome());
            System.out.println("✅ Mesa " + mesa.getNumero() + " reservada por " + usuario.getNome());
            request.getSession().setAttribute("msgSucesso",
                "Mesa " + mesa.getNumero() + " reservada com sucesso!");
        } catch (Exception e) {
            System.err.println("❌ ERRO ao reservar mesa: " + e.getMessage());
            e.printStackTrace();
            request.getSession().setAttribute("msgErro", "Erro ao reservar. Tente novamente.");
        }

        response.sendRedirect(request.getContextPath() + "/app/cliente/reserva");
    }

    // ── MEUS PEDIDOS ────────────────────────────────────────────────

    /* ================================================================
       LISTAR PEDIDOS DO CLIENTE LOGADO
       ================================================================

       URL: GET /app/cliente/meus-pedidos

       Fluxo:
       1. Busca todos os pedidos cujo identificador_operador seja
          igual ao nome do cliente logado (é assim que o sistema
          associa pedidos ao cliente, já que não há tabela própria
          de clientes — ver observação no cabeçalho da classe)
       2. Para cada pedido, carrega os itens (necessário para exibir
          o detalhamento — produtos, quantidades, subtotal — na tela)
       3. Recupera e limpa mensagem de sucesso da sessão
    */
    private void exibirMeusPedidos(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Usuario usuario = (Usuario) request.getSession().getAttribute("usuarioLogado");
        System.out.println("📋 Buscando pedidos do cliente: " + usuario.getNome());

        try (Connection conn = Conexao.getConnection()) {
            // ---- Filtra pedidos pelo nome do cliente (identificador_operador = nome) ----
            List<Pedido> pedidos = new PedidoDAO(conn)
                    .listarPorOperador(usuario.getNome());
            System.out.println("✅ " + pedidos.size() + " pedido(s) encontrado(s)");

            // ---- Carrega os itens de cada pedido individualmente ----
            ItemPedidoDAO ipDao = new ItemPedidoDAO(conn);
            for (Pedido p : pedidos) {
                p.setItens(ipDao.listarPorPedido(p.getIdPedido()));
            }

            // ---- Recupera mensagem de sucesso (fluxo POST-REDIRECT-GET) ----
            String msg = (String) request.getSession().getAttribute("msgSucesso");
            if (msg != null) {
                System.out.println("💬 Mensagem de sucesso encontrada: " + msg);
                request.setAttribute("msgSucesso", msg);
                request.getSession().removeAttribute("msgSucesso");
            }

            request.setAttribute("pedidos",    pedidos);
            request.setAttribute("paginaAtiva","meus-pedidos");
            request.getRequestDispatcher("/WEB-INF/views/cliente/meus_pedidos.jsp")
                   .forward(request, response);

        } catch (Exception e) {
            System.err.println("❌ ERRO ao buscar meus pedidos: " + e.getMessage());
            e.printStackTrace();
            request.getRequestDispatcher("/WEB-INF/views/error/500.jsp")
                   .forward(request, response);
        }
    }

    // ── MESA: "Chamar Garçom" ou "Pedido Direto" ─────────────────────

    /* ================================================================
       EXIBIR TELA "ESTOU NA MESA"
       ================================================================

       URL: GET /app/cliente/mesa

       Carrega DUAS listas de uma vez — todas as mesas (para o
       seletor) E todo o cardápio (para a seção de pedido direto, que
       fica escondida via JS até o cliente escolher essa opção) — para
       a JSP conseguir alternar entre "escolher mesa" e "montar
       pedido" inteiramente no navegador, sem precisar de uma segunda
       ida ao servidor no meio do fluxo.
    */
    private void exibirMesa(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("🍽️ Exibindo tela 'Estou na Mesa'");

        try (Connection conn = Conexao.getConnection()) {
            List<Mesa> mesas = new MesaDAO(conn).listar();
            List<Cardapio> itens = new CardapioDAO(conn).listar().stream()
                    .filter(Cardapio::isDisponivel)
                    .toList();

            System.out.println("✅ " + mesas.size() + " mesa(s) e " + itens.size() + " item(ns) carregados");

            request.setAttribute("mesas", mesas);
            request.setAttribute("itens", itens);
            request.setAttribute("paginaAtiva", "mesa");
            request.setAttribute("msgSucesso", request.getSession().getAttribute("msgSucesso"));
            request.setAttribute("msgErro",    request.getSession().getAttribute("msgErro"));
            request.getSession().removeAttribute("msgSucesso");
            request.getSession().removeAttribute("msgErro");

            request.getRequestDispatcher("/WEB-INF/views/cliente/mesa.jsp").forward(request, response);

        } catch (Exception e) {
            System.err.println("❌ ERRO ao carregar tela de mesa: " + e.getMessage());
            e.printStackTrace();
            request.getRequestDispatcher("/WEB-INF/views/error/500.jsp").forward(request, response);
        }
    }

    /* ================================================================
       CHAMAR GARÇOM
       ================================================================

       URL: POST /app/cliente/mesa (acao=chamarGarcom&mesaId=X)

       Só sinaliza a mesa (MesaDAO.chamarGarcom) — não cria pedido
       nenhum. Quem vê e "atende" essa chamada é o funcionário, na
       tela de Mesas (ver MesaController.atenderChamado()).
    */
    private void chamarGarcom(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int mesaId = parseId(request.getParameter("mesaId"));
        System.out.println("🔔 Cliente chamando garçom — mesa id=" + mesaId);

        if (mesaId <= 0) {
            request.getSession().setAttribute("msgErro", "Mesa inválida.");
            response.sendRedirect(request.getContextPath() + "/app/cliente/mesa");
            return;
        }

        try (Connection conn = Conexao.getConnection()) {
            new MesaDAO(conn).chamarGarcom(mesaId);
            System.out.println("✅ Garçom chamado para a mesa id=" + mesaId);
            request.getSession().setAttribute("msgSucesso", "Garçom chamado! Alguém já está a caminho.");
        } catch (Exception e) {
            System.err.println("❌ ERRO ao chamar garçom: " + e.getMessage());
            e.printStackTrace();
            request.getSession().setAttribute("msgErro", "Erro ao chamar o garçom. Tente novamente.");
        }

        response.sendRedirect(request.getContextPath() + "/app/cliente/mesa");
    }

    /* ================================================================
       CONFIRMAR PEDIDO DIRETO (cliente pede sozinho, sentado na mesa)
       ================================================================

       URL: POST /app/cliente/mesa (acao=pedidoDireto)
       mesaId=7&observacao=...&itemId=3&quantidade=2&itemId=7&quantidade=1

       Praticamente idêntico a confirmarDelivery() — mesma criação de
       pedido + itens + entrada na fila, dentro de uma única transação
       — com DUAS diferenças que justificam ser um método separado em
       vez de reaproveitar confirmarDelivery() com um parâmetro a mais:

       1. tipo='mesa' e mesa_id=X (em vez de tipo='delivery' sem mesa)
       2. Se a mesa ainda estiver 'livre' (cliente sentou e pediu
          direto sem um funcionário ter "aberto" a mesa antes), este
          método abre a mesa automaticamente, usando o NOME do
          cliente como operador — assim a mesa aparece 'ocupada' no
          painel do funcionário mesmo sem nenhum garçom ter tocado
          nela. Se a mesa já estava ocupada (outro pedido anterior
          da mesma mesa), não mexe no status dela.
    */
    private void confirmarPedidoDireto(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        System.out.println("📝 Iniciando confirmação de pedido direto (mesa)");

        Usuario  usuario    = (Usuario) request.getSession().getAttribute("usuarioLogado");
        int      mesaId     = parseId(request.getParameter("mesaId"));
        String   observacao = request.getParameter("observacao");
        String[] itemIds    = request.getParameterValues("itemId");
        String[] qtds       = request.getParameterValues("quantidade");

        System.out.println("📋 Dados recebidos:");
        System.out.println("   - Cliente: " + usuario.getNome());
        System.out.println("   - mesaId: " + mesaId);
        System.out.println("   - Qtd. de linhas no carrinho: " + (itemIds != null ? itemIds.length : 0));

        // ========== STEP 1: VALIDAR MESA E CARRINHO ==========
        if (mesaId <= 0) {
            System.err.println("❌ Mesa inválida");
            request.getSession().setAttribute("msgErro", "Selecione uma mesa válida.");
            response.sendRedirect(request.getContextPath() + "/app/cliente/mesa");
            return;
        }
        if (itemIds == null || itemIds.length == 0) {
            System.err.println("❌ Carrinho vazio — nenhum itemId recebido");
            request.getSession().setAttribute("msgErro", "Adicione pelo menos um item.");
            response.sendRedirect(request.getContextPath() + "/app/cliente/mesa");
            return;
        }

        try (Connection conn = Conexao.getConnection()) {
            conn.setAutoCommit(false);
            try {
                MesaDAO mesaDao = new MesaDAO(conn);
                Mesa mesa = mesaDao.buscarPorId(mesaId);
                if (mesa == null) {
                    throw new IllegalArgumentException("Mesa não encontrada.");
                }

                // ---- Abre a mesa automaticamente se ainda estava livre ----
                if (mesa.isLivre()) {
                    System.out.println("⏳ Mesa estava livre — abrindo automaticamente com operador=" + usuario.getNome());
                    mesaDao.atualizarStatus(mesaId, "ocupada", usuario.getNome());
                }

                // ========== CRIAR O PEDIDO (CASCA) ==========
                System.out.println("⏳ Criando pedido (tipo=mesa, mesaId=" + mesaId + ")...");
                Pedido pedido = new Pedido();
                pedido.setTipo("mesa");
                pedido.setMesaId(mesaId);
                pedido.setUrgente(false);
                pedido.setIdentificadorOperador(usuario.getNome()); // cliente identificado pelo próprio nome
                pedido.setObservacao(observacao);
                pedido.setStatus("aberto");
                pedido.setAtivo(true);

                new PedidoDAO(conn).inserir(pedido);
                int pedidoId = pedido.getIdPedido();
                System.out.println("✅ Pedido criado com id=" + pedidoId);

                // ========== MONTAR OS ITENS DO PEDIDO ==========
                List<ItemPedido> itensPedido = new ArrayList<>();
                int tempoMax = 0;
                String setor = "cozinha";
                CardapioDAO icDao = new CardapioDAO(conn);

                for (int i = 0; i < itemIds.length; i++) {
                    int itemId = parseId(itemIds[i]);
                    int qtd    = (qtds != null && i < qtds.length) ? parseId(qtds[i]) : 1;
                    if (qtd <= 0) qtd = 1;

                    Cardapio ic = icDao.buscarPorId(itemId);
                    if (ic == null || !ic.isDisponivel()) {
                        System.out.println("⚠️ Item id=" + itemId + " ignorado (inexistente ou indisponível)");
                        continue;
                    }

                    ItemPedido ip = new ItemPedido();
                    ip.setPedidoId(pedidoId);
                    ip.setItemCardapioId(itemId);
                    ip.setQuantidade(qtd);
                    ip.setPrecoUnitario(ic.getPreco());
                    ip.setStatus("pendente");
                    ip.setAtivo(true);
                    itensPedido.add(ip);

                    if (ic.getTempoPreparoMin() > tempoMax) {
                        tempoMax = ic.getTempoPreparoMin();
                        setor    = ic.getCategoria() != null
                                   ? ic.getCategoria().getSetor() : "cozinha";
                    }
                }

                if (itensPedido.isEmpty()) {
                    System.err.println("❌ Nenhum item válido após filtragem — desfazendo transação");
                    conn.rollback();
                    request.getSession().setAttribute("msgErro", "Nenhum item válido.");
                    response.sendRedirect(request.getContextPath() + "/app/cliente/mesa");
                    return;
                }

                // ========== INSERIR ITENS EM LOTE ==========
                System.out.println("⏳ Inserindo " + itensPedido.size() + " item(ns) em lote...");
                new ItemPedidoDAO(conn).inserirLote(itensPedido, pedidoId);

                // ========== ENTRAR NA FILA DE PREPARO ==========
                // Mesma prioridade normal (peso=1) do delivery — o
                // pequeno "favorecimento de mesa sobre delivery"
                // mencionado nos requisitos do projeto ainda não está
                // implementado no cálculo de posição da fila (ver
                // observação já registrada na análise de requisitos).
                System.out.println("⏳ Adicionando pedido à fila de preparo (setor=" + setor + ")...");
                FilaPreparo fila = new FilaPreparo();
                fila.setPedidoId(pedidoId);
                fila.setPesoPrioridade(1);
                fila.setTempoEstimadoMin(tempoMax);
                fila.setSetor(setor);
                fila.setAtivo(true);
                new FilaPreparoDAO(conn).inserir(fila);

                conn.commit();
                System.out.println("✅ PEDIDO DIRETO #" + pedidoId + " CONFIRMADO COM SUCESSO!");
                request.getSession().setAttribute("msgSucesso",
                    "Pedido #" + pedidoId + " enviado para a cozinha! Acompanhe em Meus Pedidos.");

            } catch (Exception e) {
                conn.rollback();
                System.err.println("❌ ERRO durante a transação — rollback executado:");
                System.err.println("   " + e.getMessage());
                e.printStackTrace();
                request.getSession().setAttribute("msgErro", "Erro ao realizar o pedido.");
            }
        } catch (Exception e) {
            System.err.println("❌ ERRO ao obter conexão: " + e.getMessage());
            e.printStackTrace();
            request.getSession().setAttribute("msgErro", "Erro ao realizar o pedido.");
        }

        response.sendRedirect(request.getContextPath() + "/app/cliente/meus-pedidos");
    }

    // ── HELPERS ─────────────────────────────────────────────────────

    /**
     * Verifica se o usuário logado na sessão tem perfil USUARIO
     * (cliente do app). Usado para proteger TODAS as rotas deste
     * controller — tanto GET quanto POST.
     */
    private boolean isCliente(HttpServletRequest request) {
        Usuario u = (Usuario) request.getSession().getAttribute("usuarioLogado");
        return u != null && "USUARIO".equals(u.getPerfil());
    }

    /**
     * Converte uma string de parâmetro para int de forma segura.
     * Retorna -1 caso a conversão falhe.
     */
    private int parseId(String v) {
        try { return Integer.parseInt(v); } catch (Exception e) { return -1; }
    }
}

/* ================================================================
   RESUMO DO CONTROLLER
   ================================================================

   ROTAS MAPEADAS:
   1. GET  /app/cliente/delivery      → exibirDelivery()
   2. POST /app/cliente/delivery      → confirmarDelivery()
   3. GET  /app/cliente/reserva       → exibirReserva()
   4. POST /app/cliente/reserva       → confirmarReserva()
   5. GET  /app/cliente/meus-pedidos  → exibirMeusPedidos()

   PERMISSÕES:
   ✅ TODAS as rotas exigem perfil USUARIO — checado no início de
      doGet() e doPost(), redirecionando para /auth/login caso
      contrário

   IDENTIFICAÇÃO DO CLIENTE:
   ✅ Não existe tabela "cliente" — pedidos usam
      identificador_operador = usuario.getNome() e mesas usam
      operador = usuario.getNome()
   ✅ "Meus Pedidos" filtra por esse mesmo nome

   TRANSAÇÕES:
   ✅ confirmarDelivery() usa transação manual (autoCommit=false)
      porque grava em pedido + item_pedido + fila_preparo de forma
      atômica — qualquer falha causa rollback completo

   REGRAS DE NEGÓCIO IMPORTANTES:
   ✅ Preço do item sempre vem do cardápio no momento da compra,
      nunca do formulário (evita manipulação de preço pelo cliente)
   ✅ Itens indisponíveis são silenciosamente ignorados no carrinho
   ✅ Só é possível reservar mesa com status "livre"
   ✅ Todo pedido novo entra automaticamente na fila de preparo

   DEPENDÊNCIAS:
   - PedidoDAO / ItemPedidoDAO / FilaPreparoDAO / MesaDAO / CardapioDAO
   - Pedido / ItemPedido / FilaPreparo / Mesa / Cardapio / Usuario: models
   - Conexao: gerenciamento de conexões

   OBSERVAÇÕES:
   - Conexões fecham automaticamente (try-with-resources)
   - Logs detalhados em cada etapa (System.out / System.err)
   - Padrão POST-REDIRECT-GET usado em todas as ações de escrita
   ================================================================ */
