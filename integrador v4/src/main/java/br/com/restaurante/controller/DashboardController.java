package br.com.restaurante.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import br.com.restaurante.dao.ItemPedidoDAO;
import br.com.restaurante.dao.MesaDAO;
import br.com.restaurante.dao.PedidoDAO;
import br.com.restaurante.model.*;
import br.com.restaurante.utils.Conexao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

/**
 * ================================================================
 * DASHBOARD CONTROLLER - Painel Gerencial (visão geral do restaurante)
 * ================================================================
 *
 * PROPÓSITO:
 * Monta a tela inicial de quem tem perfil GERENTE: um resumo rápido
 * da situação atual do restaurante (mesas, pedidos abertos, faturamento
 * do dia, e — desde a v4 — os gráficos de receita semanal e vendas por
 * categoria que aparecem no protótipo Figma) para dar visibilidade sem
 * precisar navegar por várias telas.
 *
 * FUNCIONALIDADES:
 * 1. Contar mesas por status (livre / ocupada / reservada)
 * 2. Listar pedidos em aberto (e mostrar só os 8 mais recentes)
 * 3. Calcular o faturamento do dia (pedidos já entregues hoje)
 * 4. Calcular o ticket médio de hoje (faturamento ÷ nº de pedidos entregues hoje)
 * 5. Calcular a receita dos últimos 7 dias, dia a dia (gráfico de linha)
 * 6. Calcular as vendas por setor nos últimos 7 dias (gráfico de rosca)
 *
 * ROTA MAPEADA: /app/dashboard
 * GET → única ação: monta o painel e encaminha para dashboard.jsp
 *
 * TABELAS ENVOLVIDAS:
 * - mesa           (para contagem por status)
 * - pedido         (para pedidos abertos, faturamento e ticket médio)
 * - item_pedido    (para somar preco_unitario * quantidade)
 * - cardapio / categoria_item (para saber o SETOR de cada item vendido,
 *                               usado no gráfico "Vendas por Categoria")
 *
 * PERMISSÕES:
 * ✅ Acesso EXCLUSIVO ao perfil GERENTE. Qualquer outro perfil
 *    (FUNCIONARIO ou USUARIO) é redirecionado para /app/mesas —
 *    a checagem é feita logo na primeira linha do doGet().
 *
 * ⚠️ NOTA SOBRE OS GRÁFICOS (v4):
 * O protótipo Figma mostra um gráfico "Receita da Semana" e uma rosca
 * "Vendas por Categoria". A primeira versão redesenhada deste sistema
 * (JSP) tinha deixado essa parte de fora porque o Controller ainda não
 * calculava esses dados — em vez de desenhar um gráfico com números
 * inventados na tela, preferimos esperar e implementar as queries de
 * verdade aqui. É isso que os métodos buscarReceitaUltimos7Dias() e
 * buscarVendasPorSetor() fazem: tudo vem do banco, nada é fixo/mockado.
 * Os gráficos em si são desenhados como SVG gerado no próprio
 * dashboard.jsp (sem biblioteca JS de gráficos), a partir desses dados.
 *
 * @author Sistema Integrador
 * @version 4.0
 * @see MesaDAO
 * @see PedidoDAO
 */
@WebServlet("/app/dashboard")
public class DashboardController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /* ================================================================
       MÉTODO GET - Monta o painel gerencial
       ================================================================

       Único ponto de entrada do controller. Não há roteamento por
       "acao" — a rota /app/dashboard sempre faz a mesma coisa: montar
       o resumo e exibir dashboard.jsp.

       STEP 1 — Checagem de permissão (só GERENTE)
       STEP 2 — Buscar mesas e contar por status
       STEP 3 — Buscar pedidos abertos
       STEP 4 — Calcular faturamento de hoje
       STEP 4B — Calcular receita dos últimos 7 dias (gráfico de linha)
       STEP 4C — Calcular vendas por setor nos últimos 7 dias (rosca)
       STEP 4D — Calcular ticket médio de hoje
       STEP 5 — Selecionar os 8 pedidos mais recentes para exibição
       STEP 6 — Publicar atributos e encaminhar para a JSP
    */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        System.out.println("\n========== DASHBOARD CONTROLLER GET ==========");

        // ========== STEP 1: VERIFICAR PERMISSÃO (só GERENTE) ==========
        Usuario u = (Usuario) req.getSession().getAttribute("usuarioLogado");
        System.out.println("👤 Usuário logado: " + (u != null ? u.getNome() + " (" + u.getPerfil() + ")" : "nenhum"));

        if (!"GERENTE".equals(u.getPerfil())) {
            System.err.println("❌ Acesso negado: usuário não é GERENTE — redirecionando para /app/mesas");
            res.sendRedirect(req.getContextPath() + "/app/mesas");
            System.out.println("================================================\n");
            return;
        }
        System.out.println("✅ Permissão OK — usuário é GERENTE");

        try (Connection conn = Conexao.getConnection()) {

            // ========== STEP 2: BUSCAR MESAS E CONTAR POR STATUS ==========
            System.out.println("⏳ Buscando mesas...");
            List<Mesa> mesas = new MesaDAO(conn).listar();
            long livres     = mesas.stream().filter(m -> "livre".equals(m.getStatus())).count();
            long ocupadas   = mesas.stream().filter(m -> "ocupada".equals(m.getStatus())).count();
            long reservadas = mesas.stream().filter(m -> "reservada".equals(m.getStatus())).count();
            System.out.println("✅ Mesas: " + mesas.size() + " total | "
                    + livres + " livres | " + ocupadas + " ocupadas | " + reservadas + " reservadas");

            // ========== STEP 3: BUSCAR PEDIDOS ABERTOS ==========
            System.out.println("⏳ Buscando pedidos abertos...");
            List<Pedido> abertos = new PedidoDAO(conn).listarAbertos();
            System.out.println("✅ " + abertos.size() + " pedido(s) em aberto");

            // ========== STEP 4: CALCULAR FATURAMENTO DE HOJE ==========
            System.out.println("⏳ Calculando faturamento de hoje...");
            BigDecimal totalHoje = buscarTotalHoje(conn);
            System.out.println("✅ Faturamento de hoje: R$ " + totalHoje);

            // ========== STEP 4B: RECEITA DOS ÚLTIMOS 7 DIAS (gráfico de linha) ==========
            System.out.println("⏳ Calculando receita dos últimos 7 dias...");
            Map<String, BigDecimal> receitaPorDia = buscarReceitaUltimos7Dias(conn);
            System.out.println("✅ Receita por dia: " + receitaPorDia);

            // ========== STEP 4C: VENDAS POR SETOR (rosca "Vendas por Categoria") ==========
            System.out.println("⏳ Calculando vendas por setor (últimos 7 dias)...");
            Map<String, BigDecimal> vendasPorSetor = buscarVendasPorSetor(conn);
            System.out.println("✅ Vendas por setor: " + vendasPorSetor);

            // ========== STEP 4D: TICKET MÉDIO DE HOJE ==========
            System.out.println("⏳ Calculando ticket médio de hoje...");
            int qtdPedidosHoje = buscarQtdPedidosEntreguesHoje(conn);
            BigDecimal ticketMedioHoje = qtdPedidosHoje > 0
                    ? totalHoje.divide(BigDecimal.valueOf(qtdPedidosHoje), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            System.out.println("✅ Ticket médio: R$ " + ticketMedioHoje + " (" + qtdPedidosHoje + " pedido(s) hoje)");

            // ========== STEP 5: LIMITAR ÚLTIMOS PEDIDOS EXIBIDOS (máx. 8) ==========
            List<Pedido> ultimos = abertos.size() > 8 ? abertos.subList(0, 8) : abertos;
            System.out.println("📋 Exibindo " + ultimos.size() + " pedido(s) recente(s) no painel");

            // PedidoDAO.listarAbertos() NÃO traz os itens de cada pedido
            // por padrão (só os dados do próprio pedido/mesa) — é o
            // mesmo comportamento usado em PedidoController, que sempre
            // carrega os itens manualmente logo depois de buscar a
            // lista. Sem este passo, p.getItens() viria vazio no
            // dashboard.jsp e a coluna "Itens"/"Total" da tabela
            // "Últimos Pedidos" apareceria zerada mesmo para pedidos
            // com itens de verdade. Carregamos só para os (no máximo 8)
            // pedidos que de fato vão aparecer na tela — não para a
            // lista `abertos` inteira, que pode ser bem maior.
            ItemPedidoDAO ipDao = new ItemPedidoDAO(conn);
            for (Pedido p : ultimos) {
                p.setItens(ipDao.listarPorPedido(p.getIdPedido()));
            }

            // ========== STEP 6: PUBLICAR ATRIBUTOS E ENCAMINHAR PARA A JSP ==========
            req.setAttribute("totalMesas", mesas.size());
            req.setAttribute("mesasLivres", livres);
            req.setAttribute("mesasOcupadas", ocupadas);
            req.setAttribute("mesasReservadas", reservadas);
            req.setAttribute("pedidosAbertos", abertos.size());
            req.setAttribute("totalHoje", totalHoje);
            req.setAttribute("ultimosPedidos", ultimos);
            req.setAttribute("receitaPorDia", receitaPorDia);
            req.setAttribute("vendasPorSetor", vendasPorSetor);
            req.setAttribute("ticketMedioHoje", ticketMedioHoje);
            req.setAttribute("qtdPedidosHoje", qtdPedidosHoje);
            req.setAttribute("paginaAtiva", "dashboard");

            System.out.println("➡️ Encaminhando para dashboard.jsp");
            req.getRequestDispatcher("/WEB-INF/views/dashboard/dashboard.jsp").forward(req, res);

        } catch (Exception e) {
            // ========== TRATAMENTO DE ERRO ==========
            System.err.println("❌ ERRO ao carregar dashboard:");
            System.err.println("   Tipo: " + e.getClass().getName());
            System.err.println("   Mensagem: " + e.getMessage());
            e.printStackTrace();
            req.setAttribute("erro", "Erro ao carregar dashboard.");
            req.getRequestDispatcher("/WEB-INF/views/error/500.jsp").forward(req, res);
        }
        System.out.println("================================================\n");
    }

    /* ================================================================
       HELPER: CALCULAR FATURAMENTO DO DIA
       ================================================================
       Soma preco_unitario * quantidade de todos os itens de pedidos:
       - já ENTREGUES (status='entregue')
       - abertos HOJE (DATE(data_abertura) = CURDATE())
       - com o item ainda ATIVO (ip.ativo=1)
    */
    private BigDecimal buscarTotalHoje(Connection conn) throws Exception {
        String sql = "SELECT COALESCE(SUM(ip.preco_unitario*ip.quantidade),0) AS total " +
                     "FROM item_pedido ip INNER JOIN pedido p ON ip.pedido_id=p.id_pedido " +
                     "WHERE p.status='entregue' AND DATE(p.data_abertura)=CURDATE() AND ip.ativo=1";

        System.out.println("   SQL: " + sql.replaceAll("\\s+", " "));

        try (PreparedStatement s = conn.prepareStatement(sql); ResultSet r = s.executeQuery()) {
            BigDecimal total = r.next() ? r.getBigDecimal("total") : BigDecimal.ZERO;
            System.out.println("   Resultado: R$ " + total);
            return total;
        }
    }

    /* ================================================================
       HELPER: RECEITA DOS ÚLTIMOS 7 DIAS, DIA A DIA
       ================================================================
       Alimenta o gráfico de linha "Receita da Semana" do Painel.

       ESTRATÉGIA — "sem buracos no gráfico":
       Primeiro pré-popula um LinkedHashMap com os 7 dias corridos
       (hoje e os 6 anteriores), NA ORDEM CRONOLÓGICA CORRETA, todos
       com valor ZERO. Só depois roda UMA ÚNICA query agregada
       (GROUP BY DATE(...)) e sobrescreve os dias em que efetivamente
       houve venda. Assim, um dia sem nenhum pedido entregue aparece
       como uma barra/ponto em R$0 no gráfico, em vez de simplesmente
       não aparecer — o que deixaria a linha "pulando" dias e o eixo X
       desalinhado.

       Usamos LinkedHashMap (não HashMap) porque a ORDEM de inserção
       importa aqui: é essa ordem que o dashboard.jsp usa para
       desenhar os pontos do gráfico da esquerda para a direita.

       Os rótulos ("Seg","Ter"...) vêm de DayOfWeek.getValue(), que
       retorna 1=SEGUNDA...7=DOMINGO (padrão ISO-8601 usado pelo
       java.time). Por isso o array _DIAS abaixo é indexado começando
       em "Dom" na posição 0: getValue()%7 mapeia DOMINGO(7)->0,
       SEGUNDA(1)->1, ..., SÁBADO(6)->6 — bate exatamente com a ordem
       do array.

       MESMO CRITÉRIO do faturamento de hoje: só pedidos com
       status='entregue' e itens ativos contam como receita.
    */
    private static final String[] DIAS_SEMANA = {"Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"};

    private Map<String, BigDecimal> buscarReceitaUltimos7Dias(Connection conn) throws Exception {
        Map<String, BigDecimal> resultado = new LinkedHashMap<>();

        LocalDate hoje = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate dia = hoje.minusDays(i);
            resultado.put(rotuloDia(dia), BigDecimal.ZERO);
        }

        String sql = "SELECT DATE(p.data_abertura) AS dia, " +
                     "COALESCE(SUM(ip.preco_unitario*ip.quantidade),0) AS total " +
                     "FROM pedido p INNER JOIN item_pedido ip ON ip.pedido_id=p.id_pedido " +
                     "WHERE p.status='entregue' AND ip.ativo=1 AND p.data_abertura >= ? " +
                     "GROUP BY DATE(p.data_abertura)";

        System.out.println("   SQL: " + sql.replaceAll("\\s+", " "));

        try (PreparedStatement s = conn.prepareStatement(sql)) {
            s.setDate(1, java.sql.Date.valueOf(hoje.minusDays(6)));
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    LocalDate dia = r.getDate("dia").toLocalDate();
                    resultado.put(rotuloDia(dia), r.getBigDecimal("total"));
                }
            }
        }
        return resultado;
    }

    /** Converte uma LocalDate no rótulo de 3 letras usado no gráfico (ex: "Ter"). */
    private String rotuloDia(LocalDate dia) {
        DayOfWeek dow = dia.getDayOfWeek(); // MONDAY=1 ... SUNDAY=7
        return DIAS_SEMANA[dow.getValue() % 7];
    }

    /* ================================================================
       HELPER: VENDAS POR SETOR NOS ÚLTIMOS 7 DIAS
       ================================================================
       Alimenta o gráfico de rosca "Vendas por Categoria" do Painel.

       "Setor" aqui é o mesmo enum já usado em categoria_item.setor
       ('cozinha'/'bebida'/'sobremesa') — traduzido para os rótulos
       exibidos ("Pratos"/"Bebidas"/"Sobremesas") para bater com o
       texto do protótipo Figma. O JOIN percorre
       item_pedido → cardapio → categoria_item para descobrir a que
       setor cada item vendido pertence.

       Pré-popula as 3 chaves com ZERO (mesma lógica de "sem buracos"
       do gráfico de linha) para a rosca sempre mostrar as 3 fatias,
       mesmo que uma categoria não tenha vendido nada no período —
       nesse caso ela simplesmente não ocupa espaço visual na rosca,
       mas ainda aparece na legenda com 0%.
    */
    private Map<String, BigDecimal> buscarVendasPorSetor(Connection conn) throws Exception {
        Map<String, BigDecimal> resultado = new LinkedHashMap<>();
        resultado.put("Pratos", BigDecimal.ZERO);
        resultado.put("Bebidas", BigDecimal.ZERO);
        resultado.put("Sobremesas", BigDecimal.ZERO);

        String sql = "SELECT ci.setor AS setor, " +
                     "COALESCE(SUM(ip.preco_unitario*ip.quantidade),0) AS total " +
                     "FROM item_pedido ip " +
                     "INNER JOIN pedido p ON ip.pedido_id=p.id_pedido " +
                     "INNER JOIN cardapio c ON ip.cardapio_id=c.id_cardapio " +
                     "INNER JOIN categoria_item ci ON c.categoria_id=ci.id_categoria " +
                     "WHERE p.status='entregue' AND ip.ativo=1 AND p.data_abertura >= ? " +
                     "GROUP BY ci.setor";

        System.out.println("   SQL: " + sql.replaceAll("\\s+", " "));

        try (PreparedStatement s = conn.prepareStatement(sql)) {
            s.setDate(1, java.sql.Date.valueOf(LocalDate.now().minusDays(6)));
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    String setor = r.getString("setor");
                    String label = "cozinha".equals(setor) ? "Pratos"
                                  : "bebida".equals(setor)   ? "Bebidas"
                                  :                            "Sobremesas";
                    resultado.put(label, r.getBigDecimal("total"));
                }
            }
        }
        return resultado;
    }

    /* ================================================================
       HELPER: QUANTIDADE DE PEDIDOS ENTREGUES HOJE
       ================================================================
       Usado exclusivamente para calcular o Ticket Médio (STEP 4D):
       ticketMedio = totalHoje ÷ qtdPedidosHoje.

       IMPORTANTE: esta contagem é DIFERENTE de "pedidosAbertos"
       (que conta pedidos ainda em andamento, qualquer que seja o
       dia). Aqui contamos pedidos já FINALIZADOS (entregue) e
       abertos hoje — é o mesmo filtro usado em buscarTotalHoje(),
       garantindo que o numerador e o denominador do ticket médio
       estejam sempre falando do mesmo conjunto de pedidos.
    */
    private int buscarQtdPedidosEntreguesHoje(Connection conn) throws Exception {
        String sql = "SELECT COUNT(*) AS qtd FROM pedido " +
                     "WHERE status='entregue' AND DATE(data_abertura)=CURDATE()";

        System.out.println("   SQL: " + sql.replaceAll("\\s+", " "));

        try (PreparedStatement s = conn.prepareStatement(sql); ResultSet r = s.executeQuery()) {
            int qtd = r.next() ? r.getInt("qtd") : 0;
            return qtd;
        }
    }
}

/* ================================================================
   RESUMO DO CONTROLLER
   ================================================================

   ROTA MAPEADA:
   1. GET /app/dashboard → monta o painel gerencial completo

   PERMISSÕES:
   ✅ Acesso exclusivo ao perfil GERENTE
   ✅ Qualquer outro perfil é redirecionado para /app/mesas

   DADOS EXIBIDOS NO PAINEL (v4):
   - totalMesas / mesasLivres / mesasOcupadas / mesasReservadas
   - pedidosAbertos (contagem total) / ultimosPedidos (até 8)
   - totalHoje (faturamento do dia, só pedidos já entregues)
   - ticketMedioHoje + qtdPedidosHoje (novo na v4)
   - receitaPorDia — Map<"Seg".."Dom", valor> dos últimos 7 dias (novo na v4)
   - vendasPorSetor — Map<"Pratos"/"Bebidas"/"Sobremesas", valor> dos
     últimos 7 dias (novo na v4)

   REGRA DE NEGÓCIO IMPORTANTE:
   ✅ Toda métrica de faturamento (hoje, semana, por setor, ticket
      médio) considera SOMENTE pedidos com status='entregue' — pedidos
      ainda abertos/em preparo/prontos NÃO entram em nenhuma soma,
      mesmo que tenham sido abertos no período considerado.

   DEPENDÊNCIAS:
   - MesaDAO: acesso à tabela mesa
   - PedidoDAO: acesso à tabela pedido (pedidos abertos)
   - Queries diretas (PreparedStatement) para os agregados —
     não passam pelos DAOs porque são específicos desta tela
   - Conexao: gerenciamento de conexões
   ================================================================ */
