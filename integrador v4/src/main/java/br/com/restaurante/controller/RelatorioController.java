package br.com.restaurante.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.restaurante.dao.ItemPedidoDAO;
import br.com.restaurante.dao.PedidoDAO;
import br.com.restaurante.model.Pedido;
import br.com.restaurante.model.Usuario;
import br.com.restaurante.utils.Conexao;
import br.com.restaurante.utils.RelatorioPDF;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * ================================================================
 * RELATORIO CONTROLLER - Relatórios Gerenciais em PDF
 * ================================================================
 *
 * PROPÓSITO:
 * Gera relatórios em PDF com o faturamento e os pedidos entregues em
 * um período escolhido pelo gerente, incluindo o detalhamento por
 * categoria do cardápio. Acesso restrito ao perfil GERENTE.
 *
 * FUNCIONALIDADES:
 * 1. Exibir a tela de seleção de período
 * 2. Gerar e entregar o PDF do relatório para download
 *
 * ROTA MAPEADA: /app/relatorios
 * GET  → exibe a tela de seleção de período (com datas pré-preenchidas)
 * POST → gera o PDF e envia como download (attachment)
 *
 * TABELAS ENVOLVIDAS:
 * - pedido        (pedidos entregues no período)
 * - item_pedido    (itens de cada pedido + faturamento por categoria)
 * - cardapio       (join para saber a categoria de cada item)
 * - categoria_item (nome da categoria, usado no agrupamento)
 *
 * PERMISSÕES:
 * ✅ Acesso EXCLUSIVO ao perfil GERENTE. Qualquer outro perfil é
 *    redirecionado para /app/dashboard — checagem feita logo no
 *    início de doGet() e doPost() via isGerente().
 *
 * PERÍODOS SUPORTADOS (parâmetro "periodo"):
 *   hoje        → apenas o dia atual
 *   semana      → últimos 7 dias (hoje - 6 até hoje)
 *   mes         → mês corrente (dia 1 até hoje) — também é o DEFAULT
 *   customizado → dataInicio e dataFim informadas manualmente pelo
 *                 gerente (se inválidas ou ausentes, cai no fallback
 *                 do mês corrente)
 *
 * FLUXO GET (exibir tela):
 * 1. Verifica se o usuário é GERENTE
 * 2. Pré-calcula o período padrão (mês corrente: dia 1 até hoje)
 * 3. Publica as datas formatadas (yyyy-MM-dd, formato de <input type=date>)
 *    como valores padrão dos campos do formulário
 * 4. Encaminha para relatorio.jsp
 *
 * FLUXO POST (gerar PDF):
 * 1. Verifica se o usuário é GERENTE
 * 2. Resolve o intervalo de datas (inicio/fim) conforme "periodo":
 *    - "customizado" faz o parse de dataInicio/dataFim e, se a data
 *      de início vier depois da de fim, INVERTE automaticamente
 *      (proteção contra o gerente preencher os campos trocados)
 *    - qualquer erro de parse cai no fallback do mês corrente
 * 3. Busca os pedidos ENTREGUES no período (buscarPedidosEntregues)
 * 4. Carrega os itens de cada pedido encontrado
 * 5. Agrupa o faturamento por categoria no período (agruparPorCategoria)
 * 6. Gera o PDF via RelatorioPDF.gerar(), passando pedidos + agrupamento
 * 7. Envia a resposta como download (Content-Disposition: attachment),
 *    com nome de arquivo baseado no intervalo de datas
 * 8. Em caso de erro: mensagem na sessão e redireciona de volta para
 *    a tela de seleção (sem travar a aplicação)
 *
 * QUERY DE PEDIDOS ENTREGUES (buscarPedidosEntregues):
 * SELECT em pedido + LEFT JOIN mesa (para trazer número/capacidade/
 * status da mesa, quando o pedido for do tipo "mesa"). Filtra por
 * status='entregue', intervalo de datas e ativo=1. Monta cada Pedido
 * manualmente a partir do ResultSet (não usa o DAO genérico aqui
 * porque a query já traz dados extras da mesa em uma única consulta).
 *
 * QUERY DE FATURAMENTO POR CATEGORIA (agruparPorCategoria):
 * SELECT com INNER JOIN entre item_pedido → cardapio → categoria_item
 * → pedido, somando preco_unitario*quantidade agrupado por categoria,
 * filtrando pedidos entregues no período e itens ativos. Resultado
 * ordenado do maior para o menor faturamento (ORDER BY total DESC).
 *
 * EXEMPLO DE USO:
 * ```
 * // Ver tela de seleção:
 * GET /app/relatorios
 *
 * // Gerar PDF do mês corrente:
 * POST /app/relatorios
 * periodo=mes
 *
 * // Gerar PDF de período customizado:
 * POST /app/relatorios
 * periodo=customizado&dataInicio=2026-07-01&dataFim=2026-07-31
 * ```
 *
 * @author Sistema Integrador
 * @version 3.0
 * @see PedidoDAO
 * @see ItemPedidoDAO
 * @see RelatorioPDF
 */
@WebServlet("/app/relatorios")
public class RelatorioController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT_BR   = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── GET — exibe a tela ───────────────────────────────────────────

    /* ================================================================
       MÉTODO GET - Exibir tela de seleção de período
       ================================================================

       URL: GET /app/relatorios
       Acesso: apenas GERENTE

       Pré-preenche o formulário com o intervalo do MÊS CORRENTE
       (dia 1 até hoje), para que o gerente já veja algo sensato sem
       precisar preencher nada — mas ainda pode trocar para outro
       período/datas antes de gerar o PDF.
    */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("\n========== RELATORIO CONTROLLER GET ==========");

        // ========== VERIFICAR PERMISSÃO (só GERENTE) ==========
        if (!isGerente(request)) {
            System.err.println("❌ Acesso negado: usuário não é GERENTE");
            response.sendRedirect(request.getContextPath() + "/app/dashboard");
            System.out.println("=================================================\n");
            return;
        }
        System.out.println("✅ Permissão OK");

        // ---- Pré-preenche as datas com o mês corrente ----
        LocalDate hoje   = LocalDate.now();
        LocalDate inicio = hoje.withDayOfMonth(1);
        System.out.println("📅 Período padrão sugerido: " + inicio.format(FMT_BR) + " a " + hoje.format(FMT_BR));

        request.setAttribute("dataInicioDefault", inicio.format(FMT_INPUT));
        request.setAttribute("dataFimDefault",    hoje.format(FMT_INPUT));
        request.setAttribute("paginaAtiva",       "relatorios");

        System.out.println("➡️ Encaminhando para relatorio.jsp");
        request.getRequestDispatcher("/WEB-INF/views/relatorio/relatorio.jsp")
               .forward(request, response);
        System.out.println("=================================================\n");
    }

    // ── POST — gera e entrega o PDF ──────────────────────────────────

    /* ================================================================
       MÉTODO POST - Gerar e entregar o PDF do relatório
       ================================================================

       URL: POST /app/relatorios
       Acesso: apenas GERENTE

       Parâmetros:
       - periodo:    hoje | semana | mes | customizado
       - dataInicio: (só usado se periodo=customizado) formato yyyy-MM-dd
       - dataFim:    (só usado se periodo=customizado) formato yyyy-MM-dd

       Passo a passo:
       STEP 1 — Resolver o intervalo de datas conforme o período
       STEP 2 — Buscar os pedidos entregues nesse intervalo
       STEP 3 — Carregar os itens de cada pedido
       STEP 4 — Agrupar o faturamento por categoria
       STEP 5 — Gerar o PDF (delegado a RelatorioPDF)
       STEP 6 — Enviar como download (Content-Disposition: attachment)
       STEP 7 — Em caso de erro, mensagem na sessão + redirect de volta
    */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("\n========== RELATORIO CONTROLLER POST ==========");

        // ========== VERIFICAR PERMISSÃO (só GERENTE) ==========
        if (!isGerente(request)) {
            System.err.println("❌ Acesso negado: usuário não é GERENTE");
            response.sendRedirect(request.getContextPath() + "/app/dashboard");
            System.out.println("==================================================\n");
            return;
        }

        String periodo       = request.getParameter("periodo");
        String dataInicioStr = request.getParameter("dataInicio");
        String dataFimStr    = request.getParameter("dataFim");
        System.out.println("📋 Período solicitado: " + periodo);

        // ========== STEP 1: RESOLVER AS DATAS CONFORME O PERÍODO ==========
        LocalDate hoje = LocalDate.now();
        LocalDate inicio, fim;

        switch (periodo != null ? periodo : "mes") {
            case "hoje" -> {
                inicio = hoje;
                fim    = hoje;
                System.out.println("📅 Período: HOJE");
            }
            case "semana" -> {
                inicio = hoje.minusDays(6);
                fim    = hoje;
                System.out.println("📅 Período: últimos 7 dias");
            }
            case "customizado" -> {
                try {
                    inicio = LocalDate.parse(dataInicioStr, FMT_INPUT);
                    fim    = LocalDate.parse(dataFimStr,    FMT_INPUT);
                    // ---- Proteção: se o gerente inverteu as datas, corrige automaticamente ----
                    if (inicio.isAfter(fim)) {
                        System.out.println("⚠️ Datas invertidas — trocando automaticamente");
                        LocalDate tmp = inicio; inicio = fim; fim = tmp;
                    }
                    System.out.println("📅 Período customizado: " + inicio + " a " + fim);
                } catch (Exception e) {
                    // ---- Fallback: datas inválidas/ausentes → mês corrente ----
                    System.err.println("⚠️ Erro ao interpretar datas customizadas — usando mês corrente como fallback");
                    inicio = hoje.withDayOfMonth(1);
                    fim    = hoje;
                }
            }
            default -> { // mes
                inicio = hoje.withDayOfMonth(1);
                fim    = hoje;
                System.out.println("📅 Período: mês corrente");
            }
        }

        String periodoDesc = inicio.format(FMT_BR) + " a " + fim.format(FMT_BR);
        System.out.println("📆 Intervalo final resolvido: " + periodoDesc);

        try (Connection conn = Conexao.getConnection()) {

            // ========== STEP 2: BUSCAR PEDIDOS ENTREGUES NO PERÍODO ==========
            System.out.println("⏳ Buscando pedidos entregues no período...");
            List<Pedido> pedidos = buscarPedidosEntregues(conn, inicio, fim);
            System.out.println("✅ " + pedidos.size() + " pedido(s) entregue(s) encontrado(s)");

            // ========== STEP 3: CARREGAR ITENS DE CADA PEDIDO ==========
            ItemPedidoDAO ipDao = new ItemPedidoDAO(conn);
            for (Pedido p : pedidos) {
                p.setItens(ipDao.listarPorPedido(p.getIdPedido()));
            }
            System.out.println("✅ Itens carregados para todos os pedidos");

            // ========== STEP 4: AGRUPAR FATURAMENTO POR CATEGORIA ==========
            System.out.println("⏳ Agrupando faturamento por categoria...");
            Map<String, BigDecimal> porCategoria = agruparPorCategoria(conn, inicio, fim);
            System.out.println("✅ " + porCategoria.size() + " categoria(s) com faturamento no período");

            // ========== STEP 5: GERAR O PDF ==========
            System.out.println("⏳ Gerando PDF...");
            RelatorioPDF gerador = new RelatorioPDF();
            byte[] pdf = gerador.gerar("Integrador Restaurante", periodoDesc,
                                       pedidos, porCategoria);
            System.out.println("✅ PDF gerado (" + pdf.length + " bytes)");

            // ========== STEP 6: ENVIAR COMO DOWNLOAD ==========
            String nomeArquivo = "relatorio_" + inicio.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                               + "_" + fim.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
            System.out.println("📎 Nome do arquivo: " + nomeArquivo);

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + nomeArquivo + "\"");
            response.setContentLength(pdf.length);
            response.getOutputStream().write(pdf);
            response.getOutputStream().flush();
            System.out.println("✅ PDF enviado como download com sucesso!");

        } catch (Exception e) {
            // ========== STEP 7: TRATAMENTO DE ERRO ==========
            System.err.println("❌ ERRO ao gerar o relatório:");
            System.err.println("   Tipo: " + e.getClass().getName());
            System.err.println("   Mensagem: " + e.getMessage());
            e.printStackTrace();
            request.getSession().setAttribute("msgErro", "Erro ao gerar o relatório: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/app/relatorios");
        }
        System.out.println("==================================================\n");
    }

    // ── Busca pedidos entregues no período ───────────────────────────

    /* ================================================================
       HELPER: BUSCAR PEDIDOS ENTREGUES NO PERÍODO
       ================================================================

       Faz uma query direta (não usa o DAO genérico) porque precisa de
       um LEFT JOIN com mesa para trazer número/capacidade/status da
       mesa junto — informação usada apenas neste relatório, então não
       vale a pena expor esse JOIN no DAO padrão de Pedido.

       Filtros aplicados:
       - status = 'entregue'                      → só o que virou receita
       - DATE(data_abertura) BETWEEN ? AND ?       → dentro do período
       - ativo = 1                                 → ignora cancelados

       Monta cada objeto Pedido manualmente a partir do ResultSet,
       incluindo a Mesa associada (quando o pedido tiver mesa_id).
    */
    private List<Pedido> buscarPedidosEntregues(Connection conn,
                                                 LocalDate inicio,
                                                 LocalDate fim) throws Exception {
        List<Pedido> lista = new ArrayList<>();

        String sql = "SELECT p.*, m.numero AS mesa_numero, " +
                     "       m.capacidade, m.status AS mesa_status " +
                     "FROM pedido p " +
                     "LEFT JOIN mesa m ON p.mesa_id = m.id_mesa " +
                     "WHERE p.status = 'entregue' " +
                     "  AND DATE(p.data_abertura) BETWEEN ? AND ? " +
                     "  AND p.ativo = 1 " +
                     "ORDER BY p.data_abertura ASC";

        System.out.println("   SQL (pedidos entregues): " + sql.replaceAll("\\s+", " "));
        System.out.println("   Parâmetros: inicio=" + inicio + " | fim=" + fim);

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(inicio));
            stmt.setDate(2, java.sql.Date.valueOf(fim));

            try (ResultSet rs = stmt.executeQuery()) {
                // (o objeto "dao" abaixo não é usado dentro do loop — mantido
                // como no código original, provavelmente resquício de uma
                // versão anterior que buscava dados adicionais via DAO)
                PedidoDAO dao = new PedidoDAO(conn);
                while (rs.next()) {
                    Pedido p = new Pedido();
                    p.setIdPedido(rs.getInt("id_pedido"));
                    p.setTipo(rs.getString("tipo"));
                    p.setUrgente(rs.getBoolean("urgente"));
                    p.setIdentificadorOperador(rs.getString("identificador_operador"));
                    p.setStatus(rs.getString("status"));
                    p.setObservacao(rs.getString("observacao"));
                    p.setDataAbertura(rs.getTimestamp("data_abertura").toLocalDateTime());
                    p.setAtivo(rs.getBoolean("ativo"));

                    // ---- Vincula a mesa apenas se mesa_id não for NULL ----
                    int mesaId = rs.getInt("mesa_id");
                    if (!rs.wasNull()) {
                        p.setMesaId(mesaId);
                        br.com.restaurante.model.Mesa m = new br.com.restaurante.model.Mesa();
                        m.setIdMesa(mesaId);
                        m.setNumero(rs.getInt("mesa_numero"));
                        p.setMesa(m);
                    }
                    lista.add(p);
                }
            }
        }
        return lista;
    }

    // ── Agrupa faturamento por categoria no período ──────────────────

    /* ================================================================
       HELPER: AGRUPAR FATURAMENTO POR CATEGORIA
       ================================================================

       Faz o join completo: item_pedido → cardapio → categoria_item
       → pedido, somando preco_unitario*quantidade por categoria,
       dentro do período informado, considerando apenas pedidos
       entregues e itens ativos.

       Retorna um LinkedHashMap (preserva a ordem do ORDER BY total
       DESC vindo do banco) — categoria com maior faturamento primeiro,
       útil para montar gráficos/tabelas já ordenados no PDF.
    */
    private Map<String, BigDecimal> agruparPorCategoria(Connection conn,
                                                          LocalDate inicio,
                                                          LocalDate fim) throws Exception {
        Map<String, BigDecimal> mapa = new LinkedHashMap<>();

        String sql = "SELECT c.nome AS categoria, " +
                     "       SUM(ip.preco_unitario * ip.quantidade) AS total " +
                     "FROM item_pedido ip " +
                     "INNER JOIN cardapio ic ON ip.cardapio_id = ic.id_cardapio " +
                     "INNER JOIN categoria_item c  ON ic.categoria_id    = c.id_categoria " +
                     "INNER JOIN pedido p           ON ip.pedido_id       = p.id_pedido " +
                     "WHERE p.status = 'entregue' " +
                     "  AND DATE(p.data_abertura) BETWEEN ? AND ? " +
                     "  AND ip.ativo = 1 " +
                     "GROUP BY c.nome " +
                     "ORDER BY total DESC";

        System.out.println("   SQL (faturamento por categoria): " + sql.replaceAll("\\s+", " "));
        System.out.println("   Parâmetros: inicio=" + inicio + " | fim=" + fim);

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(inicio));
            stmt.setDate(2, java.sql.Date.valueOf(fim));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String categoria = rs.getString("categoria");
                    BigDecimal total = rs.getBigDecimal("total");
                    System.out.println("   - " + categoria + ": R$ " + total);
                    mapa.put(categoria, total);
                }
            }
        }
        return mapa;
    }

    // ── Helper ───────────────────────────────────────────────────────

    /**
     * Verifica se o usuário logado na sessão tem perfil GERENTE.
     * Usado para proteger tanto a exibição da tela (GET) quanto a
     * geração do PDF (POST) — este relatório é informação sensível
     * de faturamento, restrita à gerência.
     */
    private boolean isGerente(HttpServletRequest request) {
        Usuario u = (Usuario) request.getSession().getAttribute("usuarioLogado");
        return u != null && "GERENTE".equals(u.getPerfil());
    }
}

/* ================================================================
   RESUMO DO CONTROLLER
   ================================================================

   ROTA MAPEADA: /app/relatorios
   1. GET  → exibe tela de seleção de período (relatorio.jsp)
   2. POST → gera e entrega o PDF como download

   PERMISSÕES:
   ✅ Acesso exclusivo ao perfil GERENTE
   ✅ Qualquer outro perfil é redirecionado para /app/dashboard

   PERÍODOS SUPORTADOS:
   - hoje        → dia atual
   - semana      → últimos 7 dias
   - mes         → mês corrente (default)
   - customizado → datas informadas manualmente (com correção
                   automática se estiverem invertidas, e fallback
                   para mês corrente se forem inválidas)

   CONTEÚDO DO RELATÓRIO:
   ✅ Lista de pedidos ENTREGUES no período, com itens detalhados
   ✅ Faturamento agrupado por categoria de cardápio, ordenado do
      maior para o menor

   IMPORTANTE (correção de nomenclatura vs schema v2):
   ⚠️ A query de agruparPorCategoria() foi ajustada para refletir a
      renomeação de item_cardapio → cardapio (e item_cardapio_id →
      cardapio_id, id_item → id_cardapio) feita no banco. Sem esse
      ajuste, o JOIN quebraria contra o schema atual.

   TRATAMENTO DE ERRO:
   ✅ Qualquer falha na geração do PDF é capturada, logada e o
      gerente é redirecionado de volta para a tela de seleção com
      uma mensagem de erro na sessão — nunca trava a aplicação

   DEPENDÊNCIAS:
   - PedidoDAO / ItemPedidoDAO: acesso a pedido e item_pedido
   - Queries diretas (PreparedStatement) para os agregados específicos
     deste relatório (join com mesa e com cardapio/categoria)
   - RelatorioPDF: geração do PDF em si (biblioteca própria do projeto)
   - Conexao: gerenciamento de conexões

   OBSERVAÇÕES:
   - Conexão fecha automaticamente (try-with-resources)
   - Logs detalhados em cada etapa (System.out / System.err)
   ================================================================ */
