package br.com.restaurante.controller;

import java.io.IOException;
import java.sql.Connection;
import java.util.List;

import br.com.restaurante.dao.MesaDAO;
import br.com.restaurante.dao.PedidoDAO;
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
 * MESA CONTROLLER - Gestão de Mesas do Salão
 * ================================================================
 *
 * PROPÓSITO:
 * Controla o ciclo de vida das mesas do restaurante: abrir (ocupar),
 * fechar (liberar) e reservar. Também oferece a visão de detalhe de
 * uma mesa específica, mostrando os pedidos abertos vinculados a ela.
 *
 * FUNCIONALIDADES:
 * 1. Listar todas as mesas com contagem por status
 * 2. Exibir detalhe de uma mesa (com seus pedidos abertos)
 * 3. Abrir mesa (status → ocupada)
 * 4. Fechar mesa (status → livre)
 * 5. Reservar mesa (status → reservada) — inclusive pelo cliente
 *
 * ROTA MAPEADA: /app/mesas
 * GET  (sem acao)          → listarMesas()
 * GET  acao=detalhe&id=X   → exibirDetalhe()
 * POST acao=abrirMesa      → mudarStatus(..., "ocupada")   — GERENTE/FUNCIONARIO
 * POST acao=fecharMesa     → mudarStatus(..., "livre")     — GERENTE/FUNCIONARIO
 * POST acao=reservar       → mudarStatus(..., "reservada") — qualquer perfil (regra especial)
 *
 * TABELAS ENVOLVIDAS:
 * - mesa    (status, operador, data_status — ver integrador_v2.sql,
 *            historico_mesa foi fundida direto nestes dois campos)
 * - pedido  (para listar pedidos abertos de uma mesa no detalhe)
 *
 * PERMISSÕES (v3 — regra especial para USUARIO):
 * ✅ GET: GERENTE e FUNCIONARIO têm acesso total (listar e detalhar).
 *    USUARIO é bloqueado no GET (só usa a área de cliente/reserva).
 * ✅ POST: a lógica é ramificada logo no início:
 *    - Se o perfil for USUARIO → só pode executar acao=reservar
 *      (qualquer outra ação POST de um cliente é redirecionada para
 *      /app/cliente/reserva, sem chegar a alterar nada)
 *    - Se for GERENTE/FUNCIONARIO → pode abrir, fechar ou reservar
 *      normalmente (checagem feita em temPermissaoFuncionario())
 *
 * FLUXO DE MUDANÇA DE STATUS (mudarStatus — método compartilhado):
 * 1. Lê o id da mesa
 * 2. Resolve o "operador" que será gravado no histórico inline da
 *    mesa: se o form não enviar explicitamente, usa o NOME do
 *    usuário logado (cobre tanto funcionário digitando seu nome
 *    quanto cliente reservando pelo próprio nome)
 * 3. Atualiza status + operador + data_status via MesaDAO
 * 4. Redireciona de volta — mas o destino MUDA conforme o perfil:
 *    cliente volta para /app/cliente/reserva, funcionário/gerente
 *    volta para /app/mesas (mesma tela do dashboard operacional)
 *
 * EXEMPLO DE USO:
 * ```
 * // Listar mesas:
 * GET /app/mesas
 *
 * // Detalhe de uma mesa:
 * GET /app/mesas?acao=detalhe&id=3
 *
 * // Abrir mesa (funcionário):
 * POST /app/mesas
 * acao=abrirMesa&id=3&operador=joao
 *
 * // Fechar mesa:
 * POST /app/mesas
 * acao=fecharMesa&id=3
 *
 * // Reservar mesa (cliente OU funcionário):
 * POST /app/mesas
 * acao=reservar&id=3
 * ```
 *
 * @author Sistema Integrador
 * @version 3.0 - Permissões unificadas + reserva liberada ao cliente
 * @see MesaDAO
 * @see PedidoDAO
 */
@WebServlet("/app/mesas")
public class MesaController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /* ================================================================
       MÉTODO GET - Listagem e Detalhe
       ================================================================

       URL: GET /app/mesas               → listarMesas()
       URL: GET /app/mesas?acao=detalhe  → exibirDetalhe()

       Acesso: apenas GERENTE ou FUNCIONARIO (checado logo no início —
       USUARIO nunca vê esta tela, usa /app/cliente/reserva).
    */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("\n========== MESA CONTROLLER GET ==========");

        // ========== VERIFICAR PERMISSÃO (GERENTE ou FUNCIONARIO) ==========
        if (!temPermissaoFuncionario(request)) {
            System.err.println("❌ Acesso negado: usuário não é GERENTE nem FUNCIONARIO");
            response.sendRedirect(request.getContextPath() + "/app/dashboard");
            System.out.println("===========================================\n");
            return;
        }
        System.out.println("✅ Permissão OK");

        if ("detalhe".equals(request.getParameter("acao"))) {
            System.out.println("🔀 Roteando para: exibirDetalhe()");
            exibirDetalhe(request, response);
        } else if ("novo".equals(request.getParameter("acao")) || "editar".equals(request.getParameter("acao"))) {
            // CRUD administrativo de mesa (criar/editar número e
            // capacidade) — mais restrito que o resto desta rota:
            // só GERENTE, mesmo que FUNCIONARIO já tenha passado na
            // checagem geral acima. Mesma lógica de "duas camadas de
            // permissão" já usada em CardapioController para
            // novo/editar item.
            if (!isGerente(request)) {
                System.err.println("❌ Acesso negado: ação de CRUD de mesa exige GERENTE");
                response.sendRedirect(request.getContextPath() + "/app/mesas");
                System.out.println("===========================================\n");
                return;
            }
            System.out.println("🔀 Roteando para: exibirFormulario()");
            exibirFormulario(request, response);
        } else {
            System.out.println("🔀 Roteando para: listarMesas()");
            listarMesas(request, response);
        }
        System.out.println("===========================================\n");
    }

    /* ================================================================
       MÉTODO POST - Roteador de Ações (com ramificação por perfil)
       ================================================================

       REGRA ESPECIAL: diferente da maioria dos outros controllers,
       aqui a checagem de permissão NÃO é uniforme — depende do perfil:

       - USUARIO (cliente):
           só pode disparar acao=reservar (via tela de reserva do
           cliente). Qualquer outra ação é ignorada e o cliente é
           redirecionado de volta para /app/cliente/reserva.

       - GERENTE / FUNCIONARIO:
           passam pela checagem temPermissaoFuncionario() e podem
           executar abrirMesa, fecharMesa ou reservar normalmente.

       Essa ramificação existe porque a MESMA rota (/app/mesas POST)
       atende tanto a tela operacional interna quanto (indiretamente)
       a ação de reserva vinda da área do cliente.
    */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("\n========== MESA CONTROLLER POST ==========");

        String acao = request.getParameter("acao");
        Usuario u   = (Usuario) request.getSession().getAttribute("usuarioLogado");
        System.out.println("📍 Ação solicitada: " + acao + " | perfil: "
                + (u != null ? u.getPerfil() : "não logado"));

        // ========== RAMO 1: USUARIO (cliente) só pode reservar ==========
        if ("USUARIO".equals(u.getPerfil())) {
            if ("reservar".equals(acao)) {
                System.out.println("🔀 Cliente reservando mesa — roteando para mudarStatus(reservada)");
                mudarStatus(request, response, "reservada");
            } else {
                System.err.println("❌ Cliente tentou ação não permitida (" + acao + ") — bloqueado");
                response.sendRedirect(request.getContextPath() + "/app/cliente/reserva");
            }
            System.out.println("============================================\n");
            return;
        }

        // ========== RAMO 2: GERENTE/FUNCIONARIO — checagem padrão ==========
        if (!temPermissaoFuncionario(request)) {
            System.err.println("❌ Acesso negado: usuário não é GERENTE nem FUNCIONARIO");
            response.sendRedirect(request.getContextPath() + "/app/dashboard");
            System.out.println("============================================\n");
            return;
        }

        switch (acao != null ? acao : "") {
            case "abrirMesa"  -> {
                System.out.println("🔀 Roteando para: mudarStatus(ocupada)");
                mudarStatus(request, response, "ocupada");
            }
            case "fecharMesa" -> {
                System.out.println("🔀 Roteando para: mudarStatus(livre)");
                mudarStatus(request, response, "livre");
            }
            case "reservar"   -> {
                System.out.println("🔀 Roteando para: mudarStatus(reservada)");
                mudarStatus(request, response, "reservada");
            }
            case "atenderChamado" -> {
                System.out.println("🔀 Roteando para: atenderChamado()");
                atenderChamado(request, response);
            }
            case "salvar", "excluir" -> {
                // Mesma lógica de "segunda camada de permissão": estas
                // duas ações são CRUD administrativo, mais restrito
                // que abrirMesa/fecharMesa/reservar/atenderChamado
                // (que qualquer FUNCIONARIO também pode disparar).
                if (!isGerente(request)) {
                    System.err.println("❌ Acesso negado: ação de CRUD de mesa exige GERENTE");
                    response.sendRedirect(request.getContextPath() + "/app/mesas");
                    break;
                }
                if ("salvar".equals(acao)) {
                    System.out.println("🔀 Roteando para: salvar()");
                    salvar(request, response);
                } else {
                    System.out.println("🔀 Roteando para: excluir()");
                    excluir(request, response);
                }
            }
            default           -> {
                System.err.println("❌ Ação POST desconhecida: " + acao);
                response.sendRedirect(request.getContextPath() + "/app/mesas");
            }
        }
        System.out.println("============================================\n");
    }

    /* ================================================================
       LISTAR MESAS
       ================================================================

       URL: GET /app/mesas (sem acao)

       Busca todas as mesas e calcula, em memória via Stream, a
       contagem de cada status — usado para exibir os cards de
       resumo (X livres, Y ocupadas, Z reservadas) no topo da tela.
    */
    private void listarMesas(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("📋 Iniciando listagem de mesas");

        try (Connection conn = Conexao.getConnection()) {
            List<Mesa> mesas = new MesaDAO(conn).listar();
            System.out.println("✅ " + mesas.size() + " mesa(s) carregada(s)");

            long livres     = mesas.stream().filter(m -> "livre".equals(m.getStatus())).count();
            long ocupadas   = mesas.stream().filter(m -> "ocupada".equals(m.getStatus())).count();
            long reservadas = mesas.stream().filter(m -> "reservada".equals(m.getStatus())).count();
            System.out.println("📊 Resumo: " + livres + " livres | " + ocupadas + " ocupadas | "
                    + reservadas + " reservadas");

            request.setAttribute("mesas",      mesas);
            request.setAttribute("livres",     livres);
            request.setAttribute("ocupadas",   ocupadas);
            request.setAttribute("reservadas", reservadas);
            request.setAttribute("paginaAtiva","mesas");

            request.getRequestDispatcher("/WEB-INF/views/mesa/mesas.jsp").forward(request, response);
        } catch (Exception e) {
            System.err.println("❌ ERRO ao listar mesas: " + e.getMessage());
            e.printStackTrace();
            request.getRequestDispatcher("/WEB-INF/views/error/500.jsp").forward(request, response);
        }
    }

    /* ================================================================
       EXIBIR DETALHE DE UMA MESA
       ================================================================

       URL: GET /app/mesas?acao=detalhe&id=X

       Mostra os dados da mesa e todos os pedidos ABERTOS vinculados
       a ela — útil para o funcionário conferir rapidamente o que já
       foi pedido antes de abrir/fechar a mesa.
    */
    private void exibirDetalhe(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int id = parseId(request.getParameter("id"));
        System.out.println("🔍 Exibindo detalhe da mesa id=" + id);

        if (id <= 0) {
            System.err.println("❌ id de mesa inválido");
            response.sendRedirect(request.getContextPath() + "/app/mesas");
            return;
        }

        try (Connection conn = Conexao.getConnection()) {
            Mesa mesa = new MesaDAO(conn).buscarPorId(id);
            if (mesa == null) {
                System.err.println("⚠️ Mesa id=" + id + " não encontrada");
                response.sendRedirect(request.getContextPath() + "/app/mesas");
                return;
            }
            System.out.println("✅ Mesa encontrada: número " + mesa.getNumero()
                    + " (status: " + mesa.getStatus() + ")");

            List<Pedido> pedidosAbertos = new PedidoDAO(conn).listarPorMesa(id);
            System.out.println("✅ " + pedidosAbertos.size() + " pedido(s) aberto(s) nesta mesa");

            request.setAttribute("mesa",           mesa);
            request.setAttribute("pedidosAbertos", pedidosAbertos);
            request.setAttribute("paginaAtiva",    "mesas");
            request.getRequestDispatcher("/WEB-INF/views/mesa/detalhe_mesa.jsp").forward(request, response);
        } catch (Exception e) {
            System.err.println("❌ ERRO ao exibir detalhe da mesa id=" + id + ": " + e.getMessage());
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/app/mesas");
        }
    }

    /* ================================================================
       MUDAR STATUS DA MESA (método compartilhado por todas as ações)
       ================================================================

       Usado por abrirMesa (→ocupada), fecharMesa (→livre) e
       reservar (→reservada), tanto vindo do funcionário quanto do
       cliente — por isso a lógica de redirecionamento final depende
       do perfil de quem chamou.

       Fluxo:
       1. Valida o id da mesa
       2. Resolve o "operador":
          - Se veio no form (funcionário digitou um nome/login
            específico), usa esse valor.
          - Caso contrário, usa o NOME do usuário logado — cobre o
            caso do cliente reservando pelo próprio nome, preenchendo
            o mesmo papel que a extinta tabela historico_mesa faria.
       3. Chama MesaDAO.atualizarStatus(id, novoStatus, operador) —
          essa chamada já atualiza tanto o status quanto os campos
          operador/data_status "fundidos" na própria tabela mesa
       4. Redireciona:
          - USUARIO → volta para /app/cliente/reserva (sua própria tela)
          - GERENTE/FUNCIONARIO → volta para /app/mesas
    */
    private void mudarStatus(HttpServletRequest request, HttpServletResponse response,
                             String novoStatus) throws IOException {

        int id = parseId(request.getParameter("id"));
        System.out.println("🔄 Alterando status da mesa id=" + id + " → " + novoStatus);

        if (id <= 0) {
            System.err.println("❌ id de mesa inválido");
            response.sendRedirect(request.getContextPath() + "/app/mesas");
            return;
        }

        // ---- Resolve o operador: form explícito ou nome do usuário logado ----
        String operador = request.getParameter("operador");
        Usuario u = (Usuario) request.getSession().getAttribute("usuarioLogado");
        if (operador == null || operador.isBlank()) {
            operador = u.getNome(); // cliente usa o próprio nome; funcionário também, se não informar
        }
        System.out.println("👤 Operador registrado: " + operador);

        try (Connection conn = Conexao.getConnection()) {
            new MesaDAO(conn).atualizarStatus(id, novoStatus, operador);
            System.out.println("✅ Mesa id=" + id + " atualizada para status='" + novoStatus + "'");
        } catch (Exception e) {
            System.err.println("❌ ERRO ao atualizar status da mesa id=" + id + ": " + e.getMessage());
            e.printStackTrace();
        }

        // ========== REDIRECIONAMENTO CONDICIONAL POR PERFIL ==========
        if ("USUARIO".equals(u.getPerfil())) {
            System.out.println("➡️ Redirecionando cliente para /app/cliente/reserva");
            response.sendRedirect(request.getContextPath() + "/app/cliente/reserva");
        } else {
            System.out.println("➡️ Redirecionando funcionário/gerente para /app/mesas");
            response.sendRedirect(request.getContextPath() + "/app/mesas");
        }
    }

    /* ================================================================
       ATENDER CHAMADO DE GARÇOM
       ================================================================

       URL: POST /app/mesas (acao=atenderChamado&id=X)

       Sempre exige GERENTE/FUNCIONARIO (garantido pelo RAMO 2 do
       doPost() logo acima — clientes nunca chegam até aqui, já são
       barrados antes mesmo do switch). Só limpa o sinalizador
       chamando_garcom da mesa; não mexe em status/operador/pedidos —
       "atender o chamado" é sobre a PESSOA ir até a mesa, não sobre
       nenhuma mudança de estado do sistema em si.
    */
    private void atenderChamado(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int id = parseId(request.getParameter("id"));
        System.out.println("✅ Atendendo chamado de garçom — mesa id=" + id);

        if (id <= 0) {
            System.err.println("❌ id de mesa inválido");
            response.sendRedirect(request.getContextPath() + "/app/mesas");
            return;
        }

        try (Connection conn = Conexao.getConnection()) {
            new MesaDAO(conn).atenderChamado(id);
            System.out.println("✅ Chamado da mesa id=" + id + " marcado como atendido");
        } catch (Exception e) {
            System.err.println("❌ ERRO ao atender chamado da mesa id=" + id + ": " + e.getMessage());
            e.printStackTrace();
        }

        response.sendRedirect(request.getContextPath() + "/app/mesas");
    }

    /* ================================================================
       EXIBIR FORMULÁRIO DE MESA (novo ou editar)
       ================================================================

       URL: GET /app/mesas?acao=novo
       URL: GET /app/mesas?acao=editar&id=X

       Mesmo padrão de CardapioController.exibirFormulario(): se vier
       "id", busca a mesa existente e publica como atributo "mesa"
       (form pré-preenchido = modo edição); se não vier, o form fica
       em branco (modo criação). A própria JSP decide o título/texto
       do botão com base em ${empty mesa}.
    */
    private void exibirFormulario(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String idParam = request.getParameter("id");
        System.out.println("📝 Exibindo formulário de mesa" + (idParam != null ? " (edição id=" + idParam + ")" : " (nova)"));

        try (Connection conn = Conexao.getConnection()) {
            if (idParam != null) {
                Mesa mesa = new MesaDAO(conn).buscarPorId(parseId(idParam));
                if (mesa == null) {
                    System.err.println("⚠️ Mesa id=" + idParam + " não encontrada");
                    response.sendRedirect(request.getContextPath() + "/app/mesas");
                    return;
                }
                request.setAttribute("mesa", mesa);
            }
            request.getRequestDispatcher("/WEB-INF/views/mesa/form_mesa.jsp").forward(request, response);

        } catch (Exception e) {
            System.err.println("❌ ERRO ao exibir formulário de mesa: " + e.getMessage());
            e.printStackTrace();
            request.getRequestDispatcher("/WEB-INF/views/error/500.jsp").forward(request, response);
        }
    }

    /* ================================================================
       SALVAR MESA (criar ou editar — número e capacidade)
       ================================================================

       URL: POST /app/mesas (acao=salvar)
       id (opcional — presente = edição), numero, capacidade

       Mesma validação de UNIQUE tratada explicitamente: em vez de
       deixar o SQLIntegrityConstraintViolationException genérico
       estourar até a tela de erro 500, capturamos e convertemos numa
       mensagem amigável — número de mesa duplicado é um erro de
       ENTRADA do usuário, não uma falha do sistema.
    */
    private void salvar(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int id = parseId(request.getParameter("id"));
        int numero, capacidade;

        System.out.println("💾 Salvando mesa (id=" + id + ")");

        try {
            numero     = Integer.parseInt(request.getParameter("numero"));
            capacidade = Integer.parseInt(request.getParameter("capacidade"));
        } catch (NumberFormatException e) {
            System.err.println("❌ Número/capacidade inválidos");
            request.getSession().setAttribute("msgErro", "Informe número e capacidade válidos.");
            response.sendRedirect(request.getContextPath() + "/app/mesas");
            return;
        }

        if (numero <= 0 || capacidade <= 0) {
            request.getSession().setAttribute("msgErro", "Número e capacidade devem ser maiores que zero.");
            response.sendRedirect(request.getContextPath() + "/app/mesas");
            return;
        }

        try (Connection conn = Conexao.getConnection()) {
            MesaDAO dao = new MesaDAO(conn);

            if (id <= 0) {
                Mesa nova = new Mesa();
                nova.setNumero(numero);
                nova.setCapacidade(capacidade);
                dao.inserir(nova);
                System.out.println("✅ Mesa criada: id=" + nova.getIdMesa() + ", numero=" + numero);
                request.getSession().setAttribute("msgSucesso", "Mesa " + numero + " criada com sucesso!");
            } else {
                Mesa existente = dao.buscarPorId(id);
                if (existente == null) {
                    request.getSession().setAttribute("msgErro", "Mesa não encontrada.");
                    response.sendRedirect(request.getContextPath() + "/app/mesas");
                    return;
                }
                existente.setNumero(numero);
                existente.setCapacidade(capacidade);
                dao.editar(existente);
                System.out.println("✅ Mesa id=" + id + " atualizada: numero=" + numero + ", capacidade=" + capacidade);
                request.getSession().setAttribute("msgSucesso", "Mesa atualizada com sucesso!");
            }

        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            // Violação de UNIQUE(numero) — outra mesa já usa esse número.
            System.err.println("⚠️ Número de mesa duplicado: " + numero);
            request.getSession().setAttribute("msgErro", "Já existe uma mesa com o número " + numero + ".");
        } catch (Exception e) {
            System.err.println("❌ ERRO ao salvar mesa: " + e.getMessage());
            e.printStackTrace();
            request.getSession().setAttribute("msgErro", "Erro ao salvar a mesa.");
        }

        response.sendRedirect(request.getContextPath() + "/app/mesas");
    }

    /* ================================================================
       EXCLUIR MESA (soft delete)
       ================================================================

       URL: POST /app/mesas (acao=excluir&id=X)

       ⚠️ Proteção que vale destacar: NÃO valida se a mesa tem pedidos
       abertos antes de desativar — desativar (ativo=0) só a tira das
       listagens futuras, não afeta pedidos/histórico já vinculados a
       ela (a FK pedido.mesa_id continua apontando para uma linha que
       ainda existe, só com ativo=0). Diferente de "fecharMesa"
       (mudar status para livre), que é uma operação DIFERENTE e já
       tem seu próprio gap conhecido de não validar pedidos abertos
       (ver análise de requisitos anterior, RN03) — este método não
       tenta resolver aquele gap, só evita introduzir um novo.
    */
    private void excluir(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int id = parseId(request.getParameter("id"));
        System.out.println("🗑️ Desativando mesa id=" + id);

        if (id <= 0) {
            request.getSession().setAttribute("msgErro", "Mesa inválida.");
            response.sendRedirect(request.getContextPath() + "/app/mesas");
            return;
        }

        try (Connection conn = Conexao.getConnection()) {
            new MesaDAO(conn).desativar(id);
            System.out.println("✅ Mesa id=" + id + " desativada");
            request.getSession().setAttribute("msgSucesso", "Mesa removida com sucesso.");
        } catch (Exception e) {
            System.err.println("❌ ERRO ao desativar mesa id=" + id + ": " + e.getMessage());
            e.printStackTrace();
            request.getSession().setAttribute("msgErro", "Erro ao remover a mesa.");
        }

        response.sendRedirect(request.getContextPath() + "/app/mesas");
    }

    // ── HELPERS ─────────────────────────────────────────────────────

    /**
     * Verifica se o usuário logado é GERENTE ou FUNCIONARIO.
     * Usado para proteger a listagem/detalhe (GET) e as ações de
     * escrita quando NÃO se trata de um cliente reservando.
     */
    private boolean temPermissaoFuncionario(HttpServletRequest request) {
        Usuario u = (Usuario) request.getSession().getAttribute("usuarioLogado");
        return u != null && ("GERENTE".equals(u.getPerfil()) || "FUNCIONARIO".equals(u.getPerfil()));
    }

    /**
     * Verifica especificamente GERENTE — usado como "segunda camada"
     * de permissão para as ações de CRUD administrativo (novo/
     * editar/salvar/excluir mesa), mais restritas que o resto desta
     * rota (que FUNCIONARIO também acessa).
     */
    private boolean isGerente(HttpServletRequest request) {
        Usuario u = (Usuario) request.getSession().getAttribute("usuarioLogado");
        return u != null && "GERENTE".equals(u.getPerfil());
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

   ROTA ÚNICA: /app/mesas (roteada por método HTTP + parâmetro "acao")

   AÇÕES MAPEADAS:
   1. GET  (sem acao)         → listarMesas()      — GERENTE/FUNCIONARIO
   2. GET  acao=detalhe&id=X  → exibirDetalhe()    — GERENTE/FUNCIONARIO
   3. POST acao=abrirMesa     → mudarStatus(ocupada)   — GERENTE/FUNCIONARIO
   4. POST acao=fecharMesa    → mudarStatus(livre)     — GERENTE/FUNCIONARIO
   5. POST acao=reservar      → mudarStatus(reservada) — QUALQUER perfil

   PERMISSÕES (regra especial):
   ✅ GET sempre exige GERENTE ou FUNCIONARIO
   ✅ POST acao=reservar é a ÚNICA ação liberada para USUARIO (cliente)
   ✅ Demais ações POST exigem GERENTE ou FUNCIONARIO

   CAMPOS "FUNDIDOS" DA EXTINTA historico_mesa:
   ✅ mesa.operador     → quem foi o último a mudar o status
   ✅ mesa.data_status  → quando essa mudança ocorreu
   (atualizados juntos, dentro de MesaDAO.atualizarStatus())

   REDIRECIONAMENTO CONDICIONAL:
   ✅ Após mudarStatus(), o destino depende do perfil de quem chamou:
      cliente → /app/cliente/reserva | funcionário/gerente → /app/mesas

   DEPENDÊNCIAS:
   - MesaDAO: acesso à tabela mesa
   - PedidoDAO: pedidos abertos de uma mesa (no detalhe)
   - Mesa / Pedido / Usuario: models
   - Conexao: gerenciamento de conexões

   OBSERVAÇÕES:
   - Conexões fecham automaticamente (try-with-resources)
   - Logs detalhados em cada etapa (System.out / System.err)
   ================================================================ */
