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
import br.com.restaurante.dao.PagamentoDAO;
import br.com.restaurante.dao.PedidoDAO;
import br.com.restaurante.model.FilaPreparo;
import br.com.restaurante.model.Cardapio;
import br.com.restaurante.model.ItemPedido;
import br.com.restaurante.model.Mesa;
import br.com.restaurante.model.Pagamento;
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
 * PEDIDO CONTROLLER - Gestão de Pedidos (Mesa e Delivery interno)
 * ================================================================
 *
 * PROPÓSITO:
 * Controller principal usado por GERENTE e FUNCIONARIO para criar
 * pedidos (de mesa ou delivery lançado manualmente), acompanhar o
 * andamento (avançar status) e cancelar pedidos. Também é aqui que
 * o pagamento é registrado, no momento em que o pedido é marcado
 * como "entregue".
 *
 * FUNCIONALIDADES:
 * 1. Listar pedidos em aberto (com itens carregados)
 * 2. Exibir formulário de novo pedido
 * 3. Exibir detalhe de um pedido (itens + pagamentos já registrados)
 * 4. Criar um novo pedido (mesa ou delivery)
 * 5. Avançar o status do pedido (aberto → em_preparo → pronto → entregue)
 * 6. Cancelar um pedido (soft delete em cascata)
 *
 * ROTA MAPEADA: /app/pedidos
 * GET  (sem acao)       → listar()
 * GET  acao=novo        → exibirFormulario()
 * GET  acao=detalhe&id  → exibirDetalhe()
 * POST acao=criar         → criar()
 * POST acao=avancarStatus → avancarStatus()
 * POST acao=cancelar      → cancelar()
 *
 * TABELAS ENVOLVIDAS:
 * - pedido        (criação, avanço de status, cancelamento)
 * - item_pedido   (itens do pedido, cancelados em cascata)
 * - fila_preparo  (criada junto com o pedido; avançada/desativada)
 * - mesa          (para montar o formulário com mesas livres)
 * - cardapio      (para validar itens e pegar preço atual)
 * - pagamento     (registrado ao marcar o pedido como "entregue")
 *
 * PERMISSÕES (v3 — FUNCIONARIO unificado):
 * ✅ GERENTE e FUNCIONARIO têm acesso total a todas as ações deste
 *    controller (checagem única em temPermissao(), sem distinção
 *    pela coluna "funcao" — atendente e cozinha usam a mesma tela).
 * ❌ USUARIO (cliente) NÃO usa este controller — pedidos de cliente
 *    passam por ClienteController (delivery/reserva), que tem regras
 *    próprias (ex: preço sempre vindo do cardápio, sem opção de
 *    "urgente").
 *
 * FLUXO DE CRIAÇÃO (criar):
 * 1. Lê tipo (mesa/delivery), operador, urgente, observação e a
 *    lista paralela de itemId/quantidade
 * 2. Se "operador" não vier do form, usa o login do usuário logado
 * 3. Exige pelo menos 1 item — senão volta para o formulário
 * 4. Transação (autoCommit=false):
 *    a) cria o Pedido "casca" (vincula mesaId se for tipo=mesa)
 *    b) para cada item: valida existência/disponibilidade no
 *       cardápio, usa o PREÇO ATUAL (nunca o do form), rastreia o
 *       maior tempo de preparo + setor correspondente
 *    c) se nenhum item sobrou → rollback e erro
 *    d) insere os itens em lote
 *    e) cria a entrada na fila de preparo, com peso calculado por
 *       pedido.calcularPeso() (considera a flag "urgente" — ver
 *       model Pedido, esse é o diferencial em relação ao delivery
 *       feito pelo cliente, que é sempre peso=1)
 *    f) commit
 *
 * FLUXO DE AVANÇAR STATUS (avancarStatus):
 * A máquina de estados é: aberto → em_preparo → pronto → entregue
 * (definida inline via switch — qualquer outro status não avança).
 * 1. Busca o pedido; se não existir, aborta
 * 2. Calcula o próximo status conforme o atual
 * 3. Atualiza o status do pedido
 * 4. Se o novo status for "em_preparo" → também inicia o registro
 *    na fila de preparo (marca operador + data_inicio_preparo)
 * 5. Se o novo status for "entregue":
 *    a) conclui o registro na fila de preparo
 *    b) SE o formulário enviou uma forma de pagamento, registra um
 *       Pagamento — o valor é o informado no form, OU (se vazio/zero)
 *       o total calculado automaticamente somando os itens do pedido
 *       (calcularTotalPedido) — isso permite tanto pagamento parcial
 *       /split quanto o caso simples de "pagou tudo, calcula sozinho"
 * 6. Tudo dentro da mesma transação — qualquer falha desfaz tudo
 *
 * FLUXO DE CANCELAMENTO (cancelar):
 * 1. Desativa o pedido (soft delete, ativo=0)
 * 2. Cancela em cascata todos os itens do pedido
 * 3. Desativa a entrada correspondente na fila de preparo (se existir)
 * 4. Tudo dentro de uma transação (rollback total em caso de erro)
 *
 * EXEMPLO DE USO:
 * ```
 * // Listar pedidos abertos:
 * GET /app/pedidos
 *
 * // Formulário de novo pedido (pré-selecionando uma mesa):
 * GET /app/pedidos?acao=novo&mesaId=3
 *
 * // Detalhe de um pedido:
 * GET /app/pedidos?acao=detalhe&id=45
 *
 * // Criar pedido de mesa:
 * POST /app/pedidos
 * acao=criar&tipo=mesa&mesaId=3&urgente=on&itemId=1&quantidade=2
 *
 * // Avançar status (com pagamento, ao entregar):
 * POST /app/pedidos
 * acao=avancarStatus&id=45&formaPagamento=pix&valorPagamento=120,00
 *
 * // Cancelar:
 * POST /app/pedidos
 * acao=cancelar&id=45
 * ```
 *
 * @author Sistema Integrador
 * @version 3.0 - FUNCIONARIO unificado
 * @see PedidoDAO
 * @see ItemPedidoDAO
 * @see FilaPreparoDAO
 * @see PagamentoDAO
 * @see MesaDAO
 * @see CardapioDAO
 */
@WebServlet("/app/pedidos")
public class PedidoController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /* ================================================================
       MÉTODO GET - Roteador de Páginas
       ================================================================

       acao=novo    → exibirFormulario()
       acao=detalhe → exibirDetalhe()
       (sem acao)   → listar()

       Toda a rota exige GERENTE ou FUNCIONARIO (checado no início).
    */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("\n========== PEDIDO CONTROLLER GET ==========");

        // ========== VERIFICAR PERMISSÃO (GERENTE ou FUNCIONARIO) ==========
        if (!temPermissao(request)) {
            System.err.println("❌ Acesso negado: usuário não é GERENTE nem FUNCIONARIO");
            response.sendRedirect(request.getContextPath() + "/app/cardapio");
            System.out.println("============================================\n");
            return;
        }
        System.out.println("✅ Permissão OK");

        String acao = request.getParameter("acao");
        System.out.println("📍 Ação solicitada: " + (acao != null ? acao : "(listar)"));

        switch (acao != null ? acao : "") {
            case "novo"    -> {
                System.out.println("🔀 Roteando para: exibirFormulario()");
                exibirFormulario(request, response);
            }
            case "detalhe" -> {
                System.out.println("🔀 Roteando para: exibirDetalhe()");
                exibirDetalhe(request, response);
            }
            default        -> {
                System.out.println("🔀 Roteando para: listar()");
                listar(request, response);
            }
        }
        System.out.println("============================================\n");
    }

    /* ================================================================
       MÉTODO POST - Roteador de Ações
       ================================================================

       acao=criar         → criar()
       acao=avancarStatus → avancarStatus()
       acao=cancelar      → cancelar()

       Toda a rota exige GERENTE ou FUNCIONARIO (checado no início).
       USUARIO (cliente) nunca chega aqui — ele usa ClienteController.
    */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("\n========== PEDIDO CONTROLLER POST ==========");

        // ========== VERIFICAR PERMISSÃO (GERENTE ou FUNCIONARIO) ==========
        if (!temPermissao(request)) {
            System.err.println("❌ Acesso negado: usuário não é GERENTE nem FUNCIONARIO");
            response.sendRedirect(request.getContextPath() + "/app/cardapio");
            System.out.println("=============================================\n");
            return;
        }

        String acao = request.getParameter("acao");
        System.out.println("📍 Ação solicitada: " + acao);

        switch (acao != null ? acao : "") {
            case "criar"         -> {
                System.out.println("🔀 Roteando para: criar()");
                criar(request, response);
            }
            case "avancarStatus" -> {
                System.out.println("🔀 Roteando para: avancarStatus()");
                avancarStatus(request, response);
            }
            case "cancelar"      -> {
                System.out.println("🔀 Roteando para: cancelar()");
                cancelar(request, response);
            }
            default -> {
                System.err.println("❌ Ação POST desconhecida: " + acao);
                response.sendRedirect(request.getContextPath() + "/app/pedidos");
            }
        }
        System.out.println("=============================================\n");
    }

    /* ================================================================
       LISTAR PEDIDOS ABERTOS
       ================================================================

       URL: GET /app/pedidos (sem acao)

       Busca todos os pedidos em aberto e carrega os itens de cada um
       (necessário para a tela exibir o conteúdo de cada pedido sem
       precisar de uma segunda navegação).
    */
    private void listar(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("📋 Iniciando listagem de pedidos abertos");

        try (Connection conn = Conexao.getConnection()) {
            List<Pedido> pedidos = new PedidoDAO(conn).listarAbertos();
            System.out.println("✅ " + pedidos.size() + " pedido(s) em aberto");

            ItemPedidoDAO ipDao = new ItemPedidoDAO(conn);
            for (Pedido p : pedidos) p.setItens(ipDao.listarPorPedido(p.getIdPedido()));
            System.out.println("✅ itens carregados para todos os pedidos");

            request.setAttribute("pedidos",    pedidos);
            request.setAttribute("paginaAtiva","pedidos");

            // ---- Recupera mensagem de sucesso (fluxo POST-REDIRECT-GET) ----
            String msg = (String) request.getSession().getAttribute("msgSucesso");
            if (msg != null) {
                System.out.println("💬 Mensagem de sucesso encontrada: " + msg);
                request.setAttribute("msgSucesso", msg);
                request.getSession().removeAttribute("msgSucesso");
            }
            request.getRequestDispatcher("/WEB-INF/views/pedido/pedidos.jsp")
                   .forward(request, response);
        } catch (Exception e) {
            System.err.println("❌ ERRO ao listar pedidos: " + e.getMessage());
            e.printStackTrace();
            request.getRequestDispatcher("/WEB-INF/views/error/500.jsp")
                   .forward(request, response);
        }
    }

    /* ================================================================
       EXIBIR FORMULÁRIO DE NOVO PEDIDO
       ================================================================

       URL: GET /app/pedidos?acao=novo[&mesaId=X]

       Fluxo:
       1. Busca as mesas LIVRES (para o funcionário escolher onde
          lançar um pedido de tipo=mesa)
       2. Se veio "mesaId" na URL (ex: veio de um clique na tela de
          detalhe de uma mesa específica) e essa mesa NÃO estiver na
          lista de livres (porque já está ocupada, por exemplo, ou
          porque o funcionário quer lançar um pedido adicional nela),
          ela é adicionada manualmente no topo da lista — garante que
          a mesa pré-selecionada sempre apareça no formulário mesmo
          que não esteja tecnicamente "livre"
       3. Busca todo o cardápio para montar a lista de itens do pedido
    */
    private void exibirFormulario(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("📝 Iniciando exibição do formulário de novo pedido");

        try (Connection conn = Conexao.getConnection()) {
            List<Mesa> mesas = new MesaDAO(conn).listarLivres();
            System.out.println("✅ " + mesas.size() + " mesa(s) livre(s) encontrada(s)");

            String mesaIdParam = request.getParameter("mesaId");
            if (mesaIdParam != null) {
                Mesa m = new MesaDAO(conn).buscarPorId(parseId(mesaIdParam));
                if (m != null && !mesas.contains(m)) {
                    System.out.println("➕ Mesa pré-selecionada (id=" + mesaIdParam
                            + ") adicionada manualmente à lista (não estava livre)");
                    mesas.add(0, m);
                }
                request.setAttribute("mesaIdSelecionada", parseId(mesaIdParam));
            }

            List<Cardapio> itens = new CardapioDAO(conn).listar();
            System.out.println("✅ " + itens.size() + " item(ns) de cardápio carregado(s)");

            request.setAttribute("mesas",      mesas);
            request.setAttribute("itens",      itens);
            request.setAttribute("paginaAtiva","pedidos");
            request.getRequestDispatcher("/WEB-INF/views/pedido/novo_pedido.jsp")
                   .forward(request, response);
        } catch (Exception e) {
            System.err.println("❌ ERRO ao exibir formulário: " + e.getMessage());
            e.printStackTrace();
            request.getRequestDispatcher("/WEB-INF/views/error/500.jsp")
                   .forward(request, response);
        }
    }

    /* ================================================================
       EXIBIR DETALHE DE UM PEDIDO
       ================================================================

       URL: GET /app/pedidos?acao=detalhe&id=X

       Carrega o pedido, seus itens, TODOS os pagamentos já registrados
       e o total já pago — útil para a tela mostrar quanto ainda falta
       receber (especialmente em cenários de pagamento parcial/split).
    */
    private void exibirDetalhe(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int id = parseId(request.getParameter("id"));
        System.out.println("🔍 Exibindo detalhe do pedido id=" + id);

        if (id <= 0) {
            System.err.println("❌ id de pedido inválido");
            response.sendRedirect(request.getContextPath() + "/app/pedidos");
            return;
        }

        try (Connection conn = Conexao.getConnection()) {
            Pedido pedido = new PedidoDAO(conn).buscarPorId(id);
            if (pedido == null) {
                System.err.println("⚠️ Pedido id=" + id + " não encontrado");
                response.sendRedirect(request.getContextPath() + "/app/pedidos");
                return;
            }
            System.out.println("✅ Pedido encontrado: status=" + pedido.getStatus());

            pedido.setItens(new ItemPedidoDAO(conn).listarPorPedido(id));

            List<Pagamento> pagamentos = new PagamentoDAO(conn).listarPorPedido(id);
            BigDecimal totalPago = new PagamentoDAO(conn).somarPorPedido(id);
            System.out.println("✅ " + pagamentos.size() + " pagamento(s) registrado(s), total pago: R$ " + totalPago);

            request.setAttribute("pedido",     pedido);
            request.setAttribute("pagamentos", pagamentos);
            request.setAttribute("totalPago",  totalPago);
            request.setAttribute("paginaAtiva","pedidos");
            request.getRequestDispatcher("/WEB-INF/views/pedido/detalhe_pedido.jsp")
                   .forward(request, response);
        } catch (Exception e) {
            System.err.println("❌ ERRO ao exibir detalhe do pedido id=" + id + ": " + e.getMessage());
            e.printStackTrace();
            request.getRequestDispatcher("/WEB-INF/views/error/500.jsp")
                   .forward(request, response);
        }
    }

    /* ================================================================
       CRIAR NOVO PEDIDO
       ================================================================

       URL: POST /app/pedidos (acao=criar)

       Parâmetros:
       - operador     (opcional — fallback = login do usuário logado)
       - tipo         (mesa | delivery)
       - mesaId       (obrigatório se tipo=mesa)
       - urgente      ("on" se marcado no form)
       - observacao   (opcional)
       - itemId[] / quantidade[]  (arrays paralelos do carrinho)

       Fluxo idêntico em espírito ao confirmarDelivery() do
       ClienteController, mas com duas diferenças importantes:
       1. Aqui existe a flag "urgente", que influencia o peso de
          prioridade calculado por pedido.calcularPeso() — pedidos
          lançados por funcionário podem furar a fila
       2. O tipo pode ser "mesa" (com vínculo a uma mesa específica)
          além de "delivery"

       Passo a passo:
       STEP 1 — Ler parâmetros e resolver operador (fallback = login)
       STEP 2 — Validar carrinho não vazio
       STEP 3 — Transação manual (3 tabelas: pedido, item_pedido,
                fila_preparo — todas precisam ficar consistentes)
       STEP 4 — Criar o Pedido, vinculando mesaId se for tipo=mesa
       STEP 5 — Montar os itens válidos, usando sempre o preço ATUAL
                do cardápio, e rastrear tempo/setor para a fila
       STEP 6 — Se não sobrou item válido → rollback e erro
       STEP 7 — Inserir itens em lote
       STEP 8 — Criar a entrada na fila com peso calculado pelo
                próprio Pedido (considera "urgente")
       STEP 9 — Commit e mensagem de sucesso
    */
    private void criar(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        System.out.println("🧾 Iniciando criação de pedido");

        Usuario usuario   = (Usuario) request.getSession().getAttribute("usuarioLogado");
        String operador   = request.getParameter("operador");
        String tipo       = request.getParameter("tipo");
        String mesaIdStr  = request.getParameter("mesaId");
        boolean urgente   = "on".equals(request.getParameter("urgente"));
        String observacao = request.getParameter("observacao");
        if (operador == null || operador.isBlank()) operador = usuario.getLogin();

        System.out.println("📋 Dados recebidos:");
        System.out.println("   - Tipo: " + tipo);
        System.out.println("   - Mesa: " + mesaIdStr);
        System.out.println("   - Urgente: " + urgente);
        System.out.println("   - Operador: " + operador);

        String[] itemIds = request.getParameterValues("itemId");
        String[] qtds    = request.getParameterValues("quantidade");

        // ========== STEP 2: VALIDAR CARRINHO NÃO VAZIO ==========
        if (itemIds == null || itemIds.length == 0) {
            System.err.println("❌ Carrinho vazio — nenhum itemId recebido");
            request.getSession().setAttribute("msgSucesso", "Adicione pelo menos um item.");
            response.sendRedirect(request.getContextPath() + "/app/pedidos?acao=novo");
            return;
        }

        try (Connection conn = Conexao.getConnection()) {
            // ========== STEP 3: INICIAR TRANSAÇÃO MANUAL ==========
            conn.setAutoCommit(false);
            try {
                // ========== STEP 4: CRIAR O PEDIDO ==========
                Pedido pedido = new Pedido();
                pedido.setTipo(tipo);
                pedido.setUrgente(urgente);
                pedido.setIdentificadorOperador(operador);
                pedido.setObservacao(observacao);
                pedido.setStatus("aberto");
                pedido.setAtivo(true);
                if ("mesa".equals(tipo) && mesaIdStr != null)
                    pedido.setMesaId(parseId(mesaIdStr));

                new PedidoDAO(conn).inserir(pedido);
                int pedidoId = pedido.getIdPedido();
                System.out.println("✅ Pedido criado com id=" + pedidoId);

                // ========== STEP 5: MONTAR OS ITENS DO PEDIDO ==========
                List<ItemPedido> itensPedido = new ArrayList<>();
                int tempoMax = 0; String setorPrincipal = "cozinha";
                CardapioDAO icDao = new CardapioDAO(conn);

                for (int i = 0; i < itemIds.length; i++) {
                    int itemId = parseId(itemIds[i]);
                    int qtd = (qtds != null && i < qtds.length) ? parseId(qtds[i]) : 1;
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
                    ip.setPrecoUnitario(ic.getPreco()); // sempre o preço ATUAL do cardápio
                    ip.setStatus("pendente");
                    ip.setAtivo(true);
                    itensPedido.add(ip);

                    if (ic.getTempoPreparoMin() > tempoMax) {
                        tempoMax = ic.getTempoPreparoMin();
                        setorPrincipal = ic.getCategoria() != null ? ic.getCategoria().getSetor() : "cozinha";
                    }
                }

                // ========== STEP 6: VALIDAR SE SOBROU ALGUM ITEM VÁLIDO ==========
                if (itensPedido.isEmpty()) {
                    System.err.println("❌ Nenhum item válido após filtragem — desfazendo transação");
                    conn.rollback();
                    request.getSession().setAttribute("msgSucesso", "Nenhum item válido.");
                    response.sendRedirect(request.getContextPath() + "/app/pedidos?acao=novo");
                    return;
                }

                // ========== STEP 7: INSERIR ITENS EM LOTE ==========
                System.out.println("⏳ Inserindo " + itensPedido.size() + " item(ns) em lote...");
                new ItemPedidoDAO(conn).inserirLote(itensPedido, pedidoId);

                // ========== STEP 8: ENTRAR NA FILA DE PREPARO ==========
                // pedido.calcularPeso() considera a flag "urgente" — diferente
                // do delivery do cliente, que é sempre peso fixo = 1
                FilaPreparo fila = new FilaPreparo();
                fila.setPedidoId(pedidoId);
                fila.setPesoPrioridade(pedido.calcularPeso());
                fila.setTempoEstimadoMin(tempoMax);
                fila.setSetor(setorPrincipal);
                fila.setAtivo(true);
                new FilaPreparoDAO(conn).inserir(fila);
                System.out.println("✅ Pedido adicionado à fila (setor=" + setorPrincipal
                        + ", peso=" + fila.getPesoPrioridade() + ")");

                // ========== STEP 9: COMMIT ==========
                conn.commit();
                System.out.println("✅ PEDIDO #" + pedidoId + " CRIADO COM SUCESSO!");
                request.getSession().setAttribute("msgSucesso",
                    "Pedido #" + pedidoId + " criado com sucesso!");

            } catch (Exception e) {
                conn.rollback();
                System.err.println("❌ ERRO durante a transação — rollback executado:");
                System.err.println("   " + e.getMessage());
                e.printStackTrace();
                request.getSession().setAttribute("msgSucesso", "Erro ao criar o pedido.");
            }
        } catch (Exception e) {
            System.err.println("❌ ERRO ao obter conexão: " + e.getMessage());
            e.printStackTrace();
        }
        response.sendRedirect(request.getContextPath() + "/app/pedidos");
    }

    /* ================================================================
       AVANÇAR STATUS DO PEDIDO
       ================================================================

       URL: POST /app/pedidos (acao=avancarStatus)

       Parâmetros:
       - id:             id do pedido
       - operador:       quem está avançando (fallback = login da sessão)
       - formaPagamento: opcional — só usado quando o pedido chega a "entregue"
       - valorPagamento: opcional — idem

       MÁQUINA DE ESTADOS (definida no switch abaixo):
       aberto → em_preparo → pronto → entregue
       (qualquer status fora dessa sequência não avança — "proximo"
       fica null e a transação é abortada sem fazer nada)

       Fluxo:
       1. Busca o pedido atual; se não existir, rollback e sai
       2. Calcula o próximo status
       3. Atualiza o status do pedido
       4. Se virou "em_preparo": também inicia a entrada da fila de
          preparo correspondente (grava operador + data_inicio_preparo)
       5. Se virou "entregue":
          a) conclui a entrada da fila (data_conclusao)
          b) se veio forma de pagamento no form, registra o Pagamento:
             - valor: usa o informado no form (aceita vírgula decimal);
               se vier vazio/inválido/zero, calcula automaticamente o
               total do pedido somando os subtotais dos itens
               (calcularTotalPedido) — suporta tanto pagamento
               integral automático quanto valores parciais informados
               manualmente (split de conta)
       6. Commit; rollback total em qualquer exceção
    */
    private void avancarStatus(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int id = parseId(request.getParameter("id"));
        System.out.println("⏩ Avançando status do pedido id=" + id);

        if (id <= 0) {
            System.err.println("❌ id de pedido inválido");
            response.sendRedirect(request.getContextPath() + "/app/pedidos");
            return;
        }

        Usuario usuario = (Usuario) request.getSession().getAttribute("usuarioLogado");
        String operador = request.getParameter("operador");
        if (operador == null || operador.isBlank()) operador = usuario.getLogin();

        String formaParam = request.getParameter("formaPagamento");
        String valorParam = request.getParameter("valorPagamento");

        try (Connection conn = Conexao.getConnection()) {
            conn.setAutoCommit(false);
            try {
                PedidoDAO pedidoDao = new PedidoDAO(conn);
                Pedido pedido = pedidoDao.buscarPorId(id);
                if (pedido == null) {
                    System.err.println("⚠️ Pedido id=" + id + " não encontrado — abortando");
                    conn.rollback();
                    return;
                }
                System.out.println("📍 Status atual: " + pedido.getStatus());

                // ========== CALCULAR PRÓXIMO STATUS (máquina de estados) ==========
                String proximo = switch (pedido.getStatus()) {
                    case "aberto"     -> "em_preparo";
                    case "em_preparo" -> "pronto";
                    case "pronto"     -> "entregue";
                    default           -> null;
                };
                if (proximo == null) {
                    System.err.println("⚠️ Status '" + pedido.getStatus() + "' não tem próximo passo — abortando");
                    conn.rollback();
                    return;
                }
                System.out.println("➡️ Próximo status: " + proximo);

                pedidoDao.atualizarStatus(id, proximo);

                // ---- Se entrou em preparo, inicia a entrada correspondente na fila ----
                if ("em_preparo".equals(proximo)) {
                    FilaPreparo fila = new FilaPreparoDAO(conn).buscarPorPedido(id);
                    if (fila != null) {
                        new FilaPreparoDAO(conn).iniciarPreparo(fila.getIdFila(), operador);
                        System.out.println("✅ Fila iniciada para o pedido #" + id);
                    }
                }

                // ---- Se foi entregue: conclui a fila e registra pagamento (se houver) ----
                if ("entregue".equals(proximo)) {
                    FilaPreparo fila = new FilaPreparoDAO(conn).buscarPorPedido(id);
                    if (fila != null) {
                        new FilaPreparoDAO(conn).concluir(fila.getIdFila());
                        System.out.println("✅ Fila concluída para o pedido #" + id);
                    }

                    if (formaParam != null && !formaParam.isBlank()) {
                        BigDecimal valor = BigDecimal.ZERO;
                        try { valor = new BigDecimal(valorParam.replace(",", ".")); } catch (Exception ignored) {}

                        // ---- Se não veio valor válido/positivo, calcula o total automaticamente ----
                        BigDecimal valorFinal = valor.compareTo(BigDecimal.ZERO) > 0
                                     ? valor : calcularTotalPedido(conn, id);

                        Pagamento pag = new Pagamento();
                        pag.setPedidoId(id);
                        pag.setFormaPagamento(formaParam);
                        pag.setValor(valorFinal);
                        pag.setIdentificadorOperador(operador);
                        new PagamentoDAO(conn).registrar(pag);
                        System.out.println("💰 Pagamento registrado: " + formaParam + " — R$ " + valorFinal);
                    } else {
                        System.out.println("ℹ️ Nenhuma forma de pagamento informada — entrega registrada sem pagamento");
                    }
                }

                conn.commit();
                System.out.println("✅ Pedido #" + id + " → " + proximo.replace("_", " "));
                request.getSession().setAttribute("msgSucesso",
                    "Pedido #" + id + " → " + proximo.replace("_", " "));

            } catch (Exception e) {
                conn.rollback();
                System.err.println("❌ ERRO ao avançar status — rollback executado:");
                System.err.println("   " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("❌ ERRO ao obter conexão: " + e.getMessage());
            e.printStackTrace();
        }
        response.sendRedirect(request.getContextPath() + "/app/pedidos");
    }

    /* ================================================================
       CANCELAR PEDIDO
       ================================================================

       URL: POST /app/pedidos (acao=cancelar)

       Fluxo (transação manual — 3 tabelas em cascata):
       1. Desativa o pedido (soft delete — PedidoDAO.desativar)
       2. Cancela todos os itens do pedido (ItemPedidoDAO.cancelarItensDoPedido)
       3. Se existir entrada na fila de preparo para este pedido,
          desativa ela também
       4. Commit; rollback total em qualquer falha, garantindo que o
          cancelamento nunca fique "pela metade" (ex: pedido cancelado
          mas fila ainda ativa)

       IMPORTANTE: assim como em Cardapio, aqui é sempre soft delete
       (ativo=0) — nunca DELETE físico, preservando o histórico.
    */
    private void cancelar(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int id = parseId(request.getParameter("id"));
        System.out.println("🚫 Iniciando cancelamento do pedido id=" + id);

        if (id <= 0) {
            System.err.println("❌ id de pedido inválido");
            response.sendRedirect(request.getContextPath() + "/app/pedidos");
            return;
        }

        try (Connection conn = Conexao.getConnection()) {
            conn.setAutoCommit(false);
            try {
                new PedidoDAO(conn).desativar(id);
                System.out.println("✅ Pedido id=" + id + " desativado (ativo=0)");

                new ItemPedidoDAO(conn).cancelarItensDoPedido(id);
                System.out.println("✅ Itens do pedido id=" + id + " cancelados");

                FilaPreparo fila = new FilaPreparoDAO(conn).buscarPorPedido(id);
                if (fila != null) {
                    new FilaPreparoDAO(conn).desativar(fila.getIdFila());
                    System.out.println("✅ Entrada na fila desativada (idFila=" + fila.getIdFila() + ")");
                }

                conn.commit();
                System.out.println("✅ Pedido #" + id + " cancelado com sucesso");
                request.getSession().setAttribute("msgSucesso", "Pedido #" + id + " cancelado.");
            } catch (Exception e) {
                conn.rollback();
                System.err.println("❌ ERRO ao cancelar — rollback executado:");
                System.err.println("   " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("❌ ERRO ao obter conexão: " + e.getMessage());
            e.printStackTrace();
        }
        response.sendRedirect(request.getContextPath() + "/app/pedidos");
    }

    /* ================================================================
       HELPER: CALCULAR TOTAL DO PEDIDO
       ================================================================

       Soma o subtotal (preço unitário × quantidade, calculado no
       próprio model ItemPedido.getSubtotal()) de todos os itens do
       pedido. Usado como valor padrão do pagamento quando o funcionário
       não informa manualmente um valor (ex: pagamento integral simples,
       sem split).
    */
    private BigDecimal calcularTotalPedido(Connection conn, int pedidoId) throws Exception {
        return new ItemPedidoDAO(conn).listarPorPedido(pedidoId).stream()
               .map(ItemPedido::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ── HELPERS ─────────────────────────────────────────────────────

    /**
     * v3 — FUNCIONARIO unificado: verifica apenas se o usuário logado
     * é GERENTE ou qualquer FUNCIONARIO, sem checar a coluna "funcao".
     */
    private boolean temPermissao(HttpServletRequest request) {
        Usuario u = (Usuario) request.getSession().getAttribute("usuarioLogado");
        return u != null && ("GERENTE".equals(u.getPerfil()) || "FUNCIONARIO".equals(u.getPerfil()));
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

   ROTA ÚNICA: /app/pedidos (roteada por método HTTP + parâmetro "acao")

   AÇÕES MAPEADAS:
   1. GET  (sem acao)          → listar()
   2. GET  acao=novo           → exibirFormulario()
   3. GET  acao=detalhe&id=X   → exibirDetalhe()
   4. POST acao=criar          → criar()
   5. POST acao=avancarStatus  → avancarStatus()
   6. POST acao=cancelar       → cancelar()

   PERMISSÕES:
   ✅ GERENTE ou FUNCIONARIO (unificado, sem checar "funcao")
   ❌ USUARIO (cliente) não acessa — usa ClienteController

   MÁQUINA DE ESTADOS DO PEDIDO:
   aberto → em_preparo → pronto → entregue
   (avancarStatus() só avança um passo por vez; cancelar() pode
   interromper o fluxo em qualquer ponto, marcando ativo=0)

   TRANSAÇÕES:
   ✅ criar(), avancarStatus() e cancelar() usam transação manual
      (autoCommit=false) — cada um mexe em múltiplas tabelas que
      precisam ficar consistentes entre si

   REGRAS DE NEGÓCIO IMPORTANTES:
   ✅ Preço do item sempre vem do cardápio no momento da criação,
      nunca do formulário
   ✅ Peso de prioridade na fila considera a flag "urgente"
      (diferente do delivery do cliente, que é sempre peso=1)
   ✅ Pagamento só é registrado quando o pedido chega a "entregue",
      e o valor pode ser manual (split) ou calculado automaticamente
   ✅ Cancelamento é sempre soft delete em cascata (pedido, itens e
      fila de preparo), nunca DELETE físico

   DEPENDÊNCIAS:
   - PedidoDAO / ItemPedidoDAO / FilaPreparoDAO / PagamentoDAO /
     MesaDAO / CardapioDAO
   - Pedido / ItemPedido / FilaPreparo / Pagamento / Mesa / Cardapio /
     Usuario: models
   - Conexao: gerenciamento de conexões

   OBSERVAÇÕES:
   - Conexões fecham automaticamente (try-with-resources)
   - Logs detalhados em cada etapa (System.out / System.err)
   - Padrão POST-REDIRECT-GET usado em todas as ações de escrita
   ================================================================ */
