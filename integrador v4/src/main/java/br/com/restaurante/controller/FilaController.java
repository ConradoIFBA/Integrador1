package br.com.restaurante.controller;

import java.io.IOException;
import java.sql.Connection;
import java.util.List;

import br.com.restaurante.dao.FilaPreparoDAO;
import br.com.restaurante.dao.PedidoDAO;
import br.com.restaurante.model.FilaPreparo;
import br.com.restaurante.model.Usuario;
import br.com.restaurante.utils.Conexao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * ================================================================
 * FILA CONTROLLER - Fila de Preparo (Cozinha / Bebida / Sobremesa)
 * ================================================================
 *
 * PROPÓSITO:
 * Exibe e gerencia a fila de preparo dos pedidos, separada por setor
 * (cozinha, bebida, sobremesa). É a tela usada pela equipe para saber
 * o que precisa ser preparado, assumir um pedido e marcar como pronto.
 *
 * FUNCIONALIDADES:
 * 1. Listar a fila separada por setor (cozinha / bebida / sobremesa)
 * 2. Iniciar o preparo de um pedido (assumir)
 * 3. Concluir o preparo de um pedido (marcar como pronto)
 *
 * ROTA MAPEADA: /app/fila
 * GET  → lista as 3 filas (uma por setor)
 * POST acao=iniciar  → assume o preparo de um pedido
 * POST acao=concluir → marca o preparo como concluído
 *
 * TABELAS ENVOLVIDAS:
 * - fila_preparo  (posição, setor, tempo estimado, quem assumiu/concluiu)
 * - pedido        (status do pedido acompanha o andamento do preparo)
 *
 * PERMISSÕES (v3 — unificado):
 * ✅ Qualquer FUNCIONARIO (atendente OU cozinha) ou GERENTE pode
 *    acessar a fila e operar sobre ela. Antes da v3 havia distinção
 *    pela coluna "funcao" (só quem era "cozinha" mexia na fila);
 *    agora qualquer funcionário pode assumir/concluir, dando mais
 *    flexibilidade operacional (ex: atendente ajudar em pico de
 *    movimento).
 * ❌ USUARIO (cliente) não tem acesso — é redirecionado para /app/cardapio.
 *
 * FLUXO DE LISTAGEM (GET):
 * 1. Verifica permissão (GERENTE ou FUNCIONARIO)
 * 2. Busca a fila de cada setor separadamente (3 queries: cozinha,
 *    bebida, sobremesa) — permite que a JSP monte 3 colunas/painéis
 * 3. Recupera e limpa mensagem de sucesso da sessão
 *
 * FLUXO DE INICIAR PREPARO (POST acao=iniciar):
 * 1. Lê idFila, idPedido e operador (quem está assumindo)
 * 2. Se "operador" não foi informado no form, usa o login do usuário
 *    logado como fallback (garante que sempre fique registrado quem
 *    assumiu, mesmo que a UI não tenha campo de texto livre)
 * 3. Transação: marca a fila como "em preparo" (com operador e hora
 *    de início) e atualiza o status do pedido para "em_preparo"
 * 4. Commit e mensagem de sucesso
 *
 * FLUXO DE CONCLUIR PREPARO (POST acao=concluir):
 * 1. Mesma lógica de operador com fallback
 * 2. Transação: marca a fila como concluída (data_conclusao) e
 *    atualiza o status do pedido para "pronto" (pronto para entrega,
 *    ainda não entregue ao cliente)
 * 3. Commit e mensagem de sucesso
 *
 * EXEMPLO DE USO:
 * ```
 * // Ver as filas:
 * GET /app/fila
 *
 * // Assumir um pedido:
 * POST /app/fila
 * acao=iniciar&idFila=12&idPedido=45&operador=joao
 *
 * // Concluir um pedido:
 * POST /app/fila
 * acao=concluir&idFila=12&idPedido=45&operador=joao
 * ```
 *
 * @author Sistema Integrador
 * @version 3.0 - FUNCIONARIO unificado (sem distinção por "funcao")
 * @see FilaPreparoDAO
 * @see PedidoDAO
 */
@WebServlet("/app/fila")
public class FilaController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /* ================================================================
       MÉTODO GET - Exibir as 3 filas de preparo
       ================================================================

       URL: GET /app/fila
       Acesso: GERENTE ou FUNCIONARIO

       Busca a fila de cada setor em consultas separadas, permitindo
       que a JSP monte painéis independentes por setor (útil para
       exibir em telas dedicadas por área da cozinha, por exemplo).
    */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("\n========== FILA CONTROLLER GET ==========");

        // ========== VERIFICAR PERMISSÃO (GERENTE ou FUNCIONARIO) ==========
        if (!temPermissao(request)) {
            System.err.println("❌ Acesso negado: usuário não é GERENTE nem FUNCIONARIO");
            response.sendRedirect(request.getContextPath() + "/app/cardapio");
            System.out.println("===========================================\n");
            return;
        }
        System.out.println("✅ Permissão OK");

        try (Connection conn = Conexao.getConnection()) {
            FilaPreparoDAO dao = new FilaPreparoDAO(conn);

            System.out.println("⏳ Buscando fila do setor: cozinha...");
            List<FilaPreparo> filaCozinha = dao.listarFila("cozinha");
            System.out.println("✅ " + filaCozinha.size() + " pedido(s) na fila da cozinha");

            System.out.println("⏳ Buscando fila do setor: bebida...");
            List<FilaPreparo> filaBebida = dao.listarFila("bebida");
            System.out.println("✅ " + filaBebida.size() + " pedido(s) na fila de bebidas");

            System.out.println("⏳ Buscando fila do setor: sobremesa...");
            List<FilaPreparo> filaSobremesa = dao.listarFila("sobremesa");
            System.out.println("✅ " + filaSobremesa.size() + " pedido(s) na fila de sobremesas");

            request.setAttribute("filaCozinha",   filaCozinha);
            request.setAttribute("filaBebida",    filaBebida);
            request.setAttribute("filaSobremesa", filaSobremesa);
            request.setAttribute("paginaAtiva",   "fila");

            // ---- Recupera mensagem de sucesso (fluxo POST-REDIRECT-GET) ----
            String msg = (String) request.getSession().getAttribute("msgSucesso");
            if (msg != null) {
                System.out.println("💬 Mensagem de sucesso encontrada: " + msg);
                request.setAttribute("msgSucesso", msg);
                request.getSession().removeAttribute("msgSucesso");
            }

            System.out.println("➡️ Encaminhando para fila.jsp");
            request.getRequestDispatcher("/WEB-INF/views/fila/fila.jsp")
                   .forward(request, response);

        } catch (Exception e) {
            // ========== TRATAMENTO DE ERRO ==========
            System.err.println("❌ ERRO ao carregar filas:");
            System.err.println("   Tipo: " + e.getClass().getName());
            System.err.println("   Mensagem: " + e.getMessage());
            e.printStackTrace();
            request.getRequestDispatcher("/WEB-INF/views/error/500.jsp")
                   .forward(request, response);
        }
        System.out.println("===========================================\n");
    }

    /* ================================================================
       MÉTODO POST - Roteador de Ações da Fila
       ================================================================

       acao=iniciar  → iniciarPreparo()
       acao=concluir → concluirPreparo()
    */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("\n========== FILA CONTROLLER POST ==========");

        // ========== VERIFICAR PERMISSÃO (GERENTE ou FUNCIONARIO) ==========
        if (!temPermissao(request)) {
            System.err.println("❌ Acesso negado: usuário não é GERENTE nem FUNCIONARIO");
            response.sendRedirect(request.getContextPath() + "/app/cardapio");
            System.out.println("============================================\n");
            return;
        }

        String acao = request.getParameter("acao");
        System.out.println("📍 Ação solicitada: " + acao);

        switch (acao != null ? acao : "") {
            case "iniciar"  -> {
                System.out.println("🔀 Roteando para: iniciarPreparo()");
                iniciarPreparo(request, response);
            }
            case "concluir" -> {
                System.out.println("🔀 Roteando para: concluirPreparo()");
                concluirPreparo(request, response);
            }
            default -> {
                System.err.println("❌ Ação POST desconhecida: " + acao);
                response.sendRedirect(request.getContextPath() + "/app/fila");
            }
        }
        System.out.println("============================================\n");
    }

    /* ================================================================
       AÇÃO 1: INICIAR PREPARO (ASSUMIR PEDIDO)
       ================================================================

       URL: POST /app/fila (acao=iniciar)

       Parâmetros:
       - idFila:   id da entrada na fila_preparo
       - idPedido: id do pedido correspondente
       - operador: quem está assumindo (opcional — fallback = login
                   do usuário logado)

       Fluxo (transação manual — 2 tabelas precisam ficar consistentes):
       1. Resolve o operador (parâmetro do form ou login da sessão)
       2. FilaPreparoDAO.iniciarPreparo() → marca data_inicio_preparo
          e grava o operador na fila
       3. PedidoDAO.atualizarStatus() → avança o pedido para "em_preparo"
       4. Commit; se qualquer passo falhar, rollback total (nunca fica
          com a fila "iniciada" e o pedido ainda "aberto", ou vice-versa)
    */
    private void iniciarPreparo(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int    idFila   = parseId(request.getParameter("idFila"));
        int    idPedido = parseId(request.getParameter("idPedido"));
        String operador = request.getParameter("operador");
        Usuario usuario = (Usuario) request.getSession().getAttribute("usuarioLogado");

        // ---- Fallback: se o form não mandar operador, usa o login da sessão ----
        if (operador == null || operador.isBlank()) operador = usuario.getLogin();

        System.out.println("👨‍🍳 Iniciando preparo — idFila=" + idFila
                + " | idPedido=" + idPedido + " | operador=" + operador);

        try (Connection conn = Conexao.getConnection()) {
            conn.setAutoCommit(false);
            try {
                new FilaPreparoDAO(conn).iniciarPreparo(idFila, operador);
                System.out.println("✅ Fila id=" + idFila + " marcada como em preparo");

                new PedidoDAO(conn).atualizarStatus(idPedido, "em_preparo");
                System.out.println("✅ Pedido #" + idPedido + " → status 'em_preparo'");

                conn.commit();
                System.out.println("✅ " + operador + " assumiu o pedido #" + idPedido + " com sucesso");
                request.getSession().setAttribute("msgSucesso",
                    operador + " assumiu o pedido #" + idPedido + ".");
            } catch (Exception e) {
                conn.rollback();
                System.err.println("❌ ERRO ao iniciar preparo — rollback executado:");
                System.err.println("   " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("❌ ERRO ao obter conexão: " + e.getMessage());
            e.printStackTrace();
        }
        response.sendRedirect(request.getContextPath() + "/app/fila");
    }

    /* ================================================================
       AÇÃO 2: CONCLUIR PREPARO (MARCAR COMO PRONTO)
       ================================================================

       URL: POST /app/fila (acao=concluir)

       Parâmetros:
       - idFila:   id da entrada na fila_preparo
       - idPedido: id do pedido correspondente
       - operador: quem está concluindo (opcional — fallback = login
                   do usuário logado)

       Fluxo (transação manual):
       1. Resolve o operador (mesma lógica de fallback do iniciarPreparo)
       2. FilaPreparoDAO.concluir() → grava data_conclusao na fila
       3. PedidoDAO.atualizarStatus() → avança o pedido para "pronto"
          (pronto para ser entregue/retirado, ainda não é "entregue")
       4. Commit; rollback total em caso de qualquer falha
    */
    private void concluirPreparo(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int    idFila   = parseId(request.getParameter("idFila"));
        int    idPedido = parseId(request.getParameter("idPedido"));
        String operador = request.getParameter("operador");
        Usuario usuario = (Usuario) request.getSession().getAttribute("usuarioLogado");

        // ---- Fallback: se o form não mandar operador, usa o login da sessão ----
        if (operador == null || operador.isBlank()) operador = usuario.getLogin();

        System.out.println("✅ Concluindo preparo — idFila=" + idFila
                + " | idPedido=" + idPedido + " | operador=" + operador);

        try (Connection conn = Conexao.getConnection()) {
            conn.setAutoCommit(false);
            try {
                new FilaPreparoDAO(conn).concluir(idFila);
                System.out.println("✅ Fila id=" + idFila + " marcada como concluída");

                new PedidoDAO(conn).atualizarStatus(idPedido, "pronto");
                System.out.println("✅ Pedido #" + idPedido + " → status 'pronto'");

                conn.commit();
                System.out.println("✅ Pedido #" + idPedido + " pronto para entrega!");
                request.getSession().setAttribute("msgSucesso",
                    "Pedido #" + idPedido + " pronto para entrega!");
            } catch (Exception e) {
                conn.rollback();
                System.err.println("❌ ERRO ao concluir preparo — rollback executado:");
                System.err.println("   " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("❌ ERRO ao obter conexão: " + e.getMessage());
            e.printStackTrace();
        }
        response.sendRedirect(request.getContextPath() + "/app/fila");
    }

    // ── HELPERS ─────────────────────────────────────────────────────

    /**
     * v3 — Verifica se o usuário logado é GERENTE ou qualquer
     * FUNCIONARIO (sem distinção pela coluna "funcao"). Antes da v3,
     * só quem tinha funcao='cozinha' acessava a fila; agora qualquer
     * funcionário pode operar, dando mais flexibilidade à equipe.
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

   ROTA ÚNICA: /app/fila (roteada por método HTTP + parâmetro "acao")

   AÇÕES MAPEADAS:
   1. GET  (sem acao)   → doGet() lista as 3 filas (cozinha/bebida/sobremesa)
   2. POST acao=iniciar → iniciarPreparo()  — assume um pedido
   3. POST acao=concluir→ concluirPreparo() — marca como pronto

   PERMISSÕES (v3):
   ✅ GERENTE ou qualquer FUNCIONARIO (unificado, sem checar "funcao")
   ❌ USUARIO (cliente) não acessa — redirecionado para /app/cardapio

   TRANSAÇÕES:
   ✅ iniciarPreparo() e concluirPreparo() usam transação manual
      (autoCommit=false) pois atualizam fila_preparo E pedido juntos —
      qualquer falha em uma das duas operações desfaz ambas (rollback)

   REGRA DE "OPERADOR":
   ✅ Se o formulário não enviar o campo "operador" explicitamente,
      o sistema usa o login do usuário da sessão como fallback —
      garante que sempre fique registrado quem executou a ação

   FLUXO DE STATUS DO PEDIDO (visão desta tela):
   aberto → (iniciar) → em_preparo → (concluir) → pronto → (entregue,
   feito em outro controller — ver PedidoController.avancarStatus)

   DEPENDÊNCIAS:
   - FilaPreparoDAO: acesso à tabela fila_preparo
   - PedidoDAO: atualização de status do pedido
   - FilaPreparo / Usuario: models
   - Conexao: gerenciamento de conexões

   OBSERVAÇÕES:
   - Conexões fecham automaticamente (try-with-resources)
   - Logs detalhados em cada etapa (System.out / System.err)
   - Padrão POST-REDIRECT-GET usado em todas as ações de escrita
   ================================================================ */
