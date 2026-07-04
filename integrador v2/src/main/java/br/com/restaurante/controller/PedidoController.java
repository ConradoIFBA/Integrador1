package br.com.restaurante.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import br.com.restaurante.dao.FilaPreparoDAO;
import br.com.restaurante.dao.HistoricoMesaDAO;
import br.com.restaurante.dao.ItemCardapioDAO;
import br.com.restaurante.dao.ItemPedidoDAO;
import br.com.restaurante.dao.LogOperacaoDAO;
import br.com.restaurante.dao.MesaDAO;
import br.com.restaurante.dao.PedidoDAO;
import br.com.restaurante.model.FilaPreparo;
import br.com.restaurante.model.HistoricoMesa;
import br.com.restaurante.model.ItemCardapio;
import br.com.restaurante.model.ItemPedido;
import br.com.restaurante.model.LogOperacao;
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
 * Rota: /app/pedidos
 * Acesso: GERENTE e FUNCIONARIO atendente
 *
 * GET  sem acao              → lista pedidos abertos
 * GET  acao=novo             → formulário de novo pedido
 * GET  acao=detalhe&id=X     → detalhes de um pedido
 * POST acao=criar            → cria pedido (transação atômica)
 * POST acao=avancarStatus    → aberto→em_preparo→pronto→entregue
 * POST acao=cancelar         → cancela pedido
 */
@WebServlet("/app/pedidos")
public class PedidoController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // ----------------------------------------------------------------
    // GET
    // ----------------------------------------------------------------

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!temPermissao(request)) {
            response.sendRedirect(request.getContextPath() + "/app/dashboard");
            return;
        }

        String acao = request.getParameter("acao");

        switch (acao != null ? acao : "") {
            case "novo"    -> exibirFormulario(request, response);
            case "detalhe" -> exibirDetalhe(request, response);
            default        -> listar(request, response);
        }
    }

    // ----------------------------------------------------------------
    // POST
    // ----------------------------------------------------------------

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!temPermissao(request)) {
            response.sendRedirect(request.getContextPath() + "/app/dashboard");
            return;
        }

        String acao = request.getParameter("acao");

        switch (acao != null ? acao : "") {
            case "criar"        -> criar(request, response);
            case "avancarStatus"-> avancarStatus(request, response);
            case "cancelar"     -> cancelar(request, response);
            default -> response.sendRedirect(
                    request.getContextPath() + "/app/pedidos");
        }
    }

    // ----------------------------------------------------------------
    // LISTAR — pedidos abertos / em preparo / prontos
    // ----------------------------------------------------------------

    private void listar(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try (Connection conn = Conexao.getConnection()) {

            List<Pedido> pedidos = new PedidoDAO(conn).listarAbertos();

            // Carrega itens de cada pedido para mostrar o total
            ItemPedidoDAO ipDao = new ItemPedidoDAO(conn);
            for (Pedido p : pedidos) {
                p.setItens(ipDao.listarPorPedido(p.getIdPedido()));
            }

            request.setAttribute("pedidos",    pedidos);
            request.setAttribute("paginaAtiva","pedidos");

            String msg = (String) request.getSession().getAttribute("msgSucesso");
            if (msg != null) {
                request.setAttribute("msgSucesso", msg);
                request.getSession().removeAttribute("msgSucesso");
            }

            request.getRequestDispatcher("/WEB-INF/views/pedido/pedidos.jsp")
                   .forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
            request.getRequestDispatcher("/WEB-INF/views/error/500.jsp")
                   .forward(request, response);
        }
    }

    // ----------------------------------------------------------------
    // FORMULÁRIO — novo pedido
    // ----------------------------------------------------------------

    private void exibirFormulario(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try (Connection conn = Conexao.getConnection()) {

            // Mesas livres para selecionar (ou delivery)
            List<Mesa> mesas = new MesaDAO(conn).listarLivres();

            // Pré-seleciona mesa se veio de /app/mesas
            String mesaIdParam = request.getParameter("mesaId");
            if (mesaIdParam != null) {
                // Também aceita mesas ocupadas quando vindo do histórico
                Mesa mesaOcupada = new MesaDAO(conn).buscarPorId(parseId(mesaIdParam));
                if (mesaOcupada != null && !mesas.contains(mesaOcupada)) {
                    mesas.add(0, mesaOcupada);
                }
                request.setAttribute("mesaIdSelecionada", parseId(mesaIdParam));
            }

            // Itens do cardápio disponíveis agrupados
            List<ItemCardapio> itens = new ItemCardapioDAO(conn).listar();

            request.setAttribute("mesas",      mesas);
            request.setAttribute("itens",      itens);
            request.setAttribute("paginaAtiva","pedidos");

            request.getRequestDispatcher("/WEB-INF/views/pedido/novo_pedido.jsp")
                   .forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
            request.getRequestDispatcher("/WEB-INF/views/error/500.jsp")
                   .forward(request, response);
        }
    }

    // ----------------------------------------------------------------
    // DETALHE — itens de um pedido específico
    // ----------------------------------------------------------------

    private void exibirDetalhe(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int id = parseId(request.getParameter("id"));
        if (id <= 0) {
            response.sendRedirect(request.getContextPath() + "/app/pedidos");
            return;
        }

        try (Connection conn = Conexao.getConnection()) {

            Pedido pedido = new PedidoDAO(conn).buscarPorId(id);
            if (pedido == null) {
                response.sendRedirect(request.getContextPath() + "/app/pedidos");
                return;
            }

            pedido.setItens(new ItemPedidoDAO(conn).listarPorPedido(id));

            request.setAttribute("pedido",     pedido);
            request.setAttribute("paginaAtiva","pedidos");

            request.getRequestDispatcher("/WEB-INF/views/pedido/detalhe_pedido.jsp")
                   .forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
            request.getRequestDispatcher("/WEB-INF/views/error/500.jsp")
                   .forward(request, response);
        }
    }

    // ----------------------------------------------------------------
    // CRIAR — transação atômica
    // pedido + item_pedido + fila_preparo + historico_mesa + log_operacao
    // ----------------------------------------------------------------

    private void criar(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Usuario usuario   = (Usuario) request.getSession().getAttribute("usuarioLogado");
        String operador   = request.getParameter("operador");
        String tipo       = request.getParameter("tipo");       // mesa | delivery
        String mesaIdStr  = request.getParameter("mesaId");
        boolean urgente   = "on".equals(request.getParameter("urgente"));
        String observacao = request.getParameter("observacao");

        if (operador == null || operador.isBlank()) operador = usuario.getLogin();

        // Lê os itens marcados: item_X=quantidades
        String[] itemIds = request.getParameterValues("itemId");
        String[] qtds    = request.getParameterValues("quantidade");

        if (itemIds == null || itemIds.length == 0) {
            request.getSession().setAttribute("msgSucesso",
                "Erro: adicione pelo menos um item ao pedido.");
            response.sendRedirect(request.getContextPath() + "/app/pedidos?acao=novo");
            return;
        }

        try (Connection conn = Conexao.getConnection()) {
            conn.setAutoCommit(false);

            try {
                PedidoDAO     pedidoDao = new PedidoDAO(conn);
                ItemPedidoDAO ipDao     = new ItemPedidoDAO(conn);
                ItemCardapioDAO icDao   = new ItemCardapioDAO(conn);
                FilaPreparoDAO  filaDao = new FilaPreparoDAO(conn);
                LogOperacaoDAO  logDao  = new LogOperacaoDAO(conn);

                // 1. Cabeçalho do pedido
                Pedido pedido = new Pedido();
                pedido.setTipo(tipo);
                pedido.setUrgente(urgente);
                pedido.setIdentificadorOperador(operador);
                pedido.setObservacao(observacao);
                pedido.setStatus("aberto");
                pedido.setAtivo(true);

                if ("mesa".equals(tipo) && mesaIdStr != null) {
                    pedido.setMesaId(parseId(mesaIdStr));
                }

                pedidoDao.inserir(pedido);
                int pedidoId = pedido.getIdPedido();

                // 2. Itens do pedido
                List<ItemPedido> itensPedido = new ArrayList<>();
                int tempoMaximo = 0;
                String setorPrincipal = "cozinha";

                for (int i = 0; i < itemIds.length; i++) {
                    int itemId = parseId(itemIds[i]);
                    int qtd    = (qtds != null && i < qtds.length)
                                  ? parseId(qtds[i]) : 1;
                    if (qtd <= 0) qtd = 1;

                    ItemCardapio ic = icDao.buscarPorId(itemId);
                    if (ic == null || !ic.isDisponivel()) continue;

                    ItemPedido ip = new ItemPedido();
                    ip.setPedidoId(pedidoId);
                    ip.setItemCardapioId(itemId);
                    ip.setQuantidade(qtd);
                    ip.setPrecoUnitario(ic.getPreco());
                    ip.setStatus("pendente");
                    ip.setAtivo(true);

                    itensPedido.add(ip);

                    // Calcula o maior tempo e setor principal
                    if (ic.getTempoPreparoMin() > tempoMaximo) {
                        tempoMaximo    = ic.getTempoPreparoMin();
                        setorPrincipal = ic.getCategoria() != null
                                         ? ic.getCategoria().getSetor() : "cozinha";
                    }
                }

                if (itensPedido.isEmpty()) {
                    conn.rollback();
                    request.getSession().setAttribute("msgSucesso",
                        "Erro: nenhum item válido selecionado.");
                    response.sendRedirect(
                        request.getContextPath() + "/app/pedidos?acao=novo");
                    return;
                }

                ipDao.inserirLote(itensPedido, pedidoId);

                // 3. Fila de preparo
                FilaPreparo fila = new FilaPreparo();
                fila.setPedidoId(pedidoId);
                fila.setPesoPrioridade(pedido.calcularPeso());
                fila.setTempoEstimadoMin(tempoMaximo);
                fila.setSetor(setorPrincipal);
                fila.setAtivo(true);
                filaDao.inserir(fila);

                // 4. Histórico da mesa (se for pedido de mesa)
                if ("mesa".equals(tipo) && pedido.getMesaId() != null) {
                    new HistoricoMesaDAO(conn).registrar(
                        new HistoricoMesa(pedido.getMesaId(),
                            "Pedido #" + pedidoId + " aberto por " + operador));
                }

                // 5. Log de operação
                logDao.registrar(new LogOperacao(
                    usuario.getPerfil(),
                    usuario.getFuncao(),
                    operador,
                    operador + " abriu pedido #" + pedidoId
                    + ("mesa".equals(tipo) ? " para mesa " + mesaIdStr : " (delivery)")
                ));

                conn.commit();

                request.getSession().setAttribute("msgSucesso",
                    "Pedido #" + pedidoId + " criado com sucesso!");

            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
                request.getSession().setAttribute("msgSucesso",
                    "Erro ao criar o pedido. Tente novamente.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        response.sendRedirect(request.getContextPath() + "/app/pedidos");
    }

    // ----------------------------------------------------------------
    // AVANÇAR STATUS
    // aberto → em_preparo → pronto → entregue
    // ----------------------------------------------------------------

    private void avancarStatus(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int id = parseId(request.getParameter("id"));
        if (id <= 0) {
            response.sendRedirect(request.getContextPath() + "/app/pedidos");
            return;
        }

        Usuario usuario  = (Usuario) request.getSession().getAttribute("usuarioLogado");
        String operador  = request.getParameter("operador");
        if (operador == null || operador.isBlank()) operador = usuario.getLogin();

        try (Connection conn = Conexao.getConnection()) {
            conn.setAutoCommit(false);
            try {
                PedidoDAO pedidoDao = new PedidoDAO(conn);
                Pedido pedido = pedidoDao.buscarPorId(id);
                if (pedido == null) { conn.rollback(); return; }

                // Determina próximo status
                String proximo = switch (pedido.getStatus()) {
                    case "aberto"     -> "em_preparo";
                    case "em_preparo" -> "pronto";
                    case "pronto"     -> "entregue";
                    default           -> null;
                };

                if (proximo == null) { conn.rollback(); return; }

                pedidoDao.atualizarStatus(id, proximo);

                // Se entregue e era mesa, atualiza histórico
                if ("entregue".equals(proximo) && pedido.getMesaId() != null) {
                    new HistoricoMesaDAO(conn).registrar(
                        new HistoricoMesa(pedido.getMesaId(),
                            "Pedido #" + id + " entregue por " + operador));
                }

                // Se avançou para em_preparo, atualiza fila
                if ("em_preparo".equals(proximo)) {
                    FilaPreparo fila = new FilaPreparoDAO(conn).buscarPorPedido(id);
                    if (fila != null) {
                        new FilaPreparoDAO(conn).iniciarPreparo(fila.getIdFila(), operador);
                    }
                }

                // Se concluído, fecha fila
                if ("entregue".equals(proximo)) {
                    FilaPreparo fila = new FilaPreparoDAO(conn).buscarPorPedido(id);
                    if (fila != null) {
                        new FilaPreparoDAO(conn).concluir(fila.getIdFila());
                    }
                }

                // Log
                new LogOperacaoDAO(conn).registrar(new LogOperacao(
                    usuario.getPerfil(), usuario.getFuncao(), operador,
                    operador + " avançou pedido #" + id + " para " + proximo
                ));

                conn.commit();
                request.getSession().setAttribute("msgSucesso",
                    "Pedido #" + id + " → " + proximo.replace("_", " "));

            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        response.sendRedirect(request.getContextPath() + "/app/pedidos");
    }

    // ----------------------------------------------------------------
    // CANCELAR
    // ----------------------------------------------------------------

    private void cancelar(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int id = parseId(request.getParameter("id"));
        if (id <= 0) {
            response.sendRedirect(request.getContextPath() + "/app/pedidos");
            return;
        }

        Usuario usuario = (Usuario) request.getSession().getAttribute("usuarioLogado");
        String operador = request.getParameter("operador");
        if (operador == null || operador.isBlank()) operador = usuario.getLogin();

        try (Connection conn = Conexao.getConnection()) {
            conn.setAutoCommit(false);
            try {
                PedidoDAO pedidoDao = new PedidoDAO(conn);
                Pedido pedido = pedidoDao.buscarPorId(id);
                if (pedido == null) { conn.rollback(); return; }

                // Cancela o pedido e todos os itens pendentes
                pedidoDao.desativar(id);
                new ItemPedidoDAO(conn).cancelarItensDoPedido(id);

                // Remove da fila
                FilaPreparo fila = new FilaPreparoDAO(conn).buscarPorPedido(id);
                if (fila != null) {
                    new FilaPreparoDAO(conn).desativar(fila.getIdFila());
                }

                // Histórico da mesa
                if (pedido.getMesaId() != null) {
                    new HistoricoMesaDAO(conn).registrar(
                        new HistoricoMesa(pedido.getMesaId(),
                            "Pedido #" + id + " cancelado por " + operador));
                }

                // Log
                new LogOperacaoDAO(conn).registrar(new LogOperacao(
                    usuario.getPerfil(), usuario.getFuncao(), operador,
                    operador + " cancelou pedido #" + id
                ));

                conn.commit();
                request.getSession().setAttribute("msgSucesso",
                    "Pedido #" + id + " cancelado.");

            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        response.sendRedirect(request.getContextPath() + "/app/pedidos");
    }

    // ----------------------------------------------------------------
    // HELPERS
    // ----------------------------------------------------------------

    private boolean temPermissao(HttpServletRequest request) {
        Usuario u = (Usuario) request.getSession().getAttribute("usuarioLogado");
        return u != null && ("GERENTE".equals(u.getPerfil())
            || ("FUNCIONARIO".equals(u.getPerfil()) && "atendente".equals(u.getFuncao())));
    }

    private int parseId(String v) {
        try { return Integer.parseInt(v); } catch (Exception e) { return -1; }
    }
}
