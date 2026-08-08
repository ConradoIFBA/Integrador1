package br.com.restaurante.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.List;
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
 * do dia) para dar visibilidade sem precisar navegar por várias telas.
 *
 * FUNCIONALIDADES:
 * 1. Contar mesas por status (livre / ocupada / reservada)
 * 2. Listar pedidos em aberto (e mostrar só os 8 mais recentes)
 * 3. Calcular o faturamento do dia (pedidos já entregues hoje)
 *
 * ROTA MAPEADA: /app/dashboard
 * GET → única ação: monta o painel e encaminha para dashboard.jsp
 *
 * TABELAS ENVOLVIDAS:
 * - mesa         (para contagem por status)
 * - pedido       (para pedidos abertos e faturamento do dia)
 * - item_pedido  (para somar preco_unitario * quantidade do faturamento)
 *
 * PERMISSÕES:
 * ✅ Acesso EXCLUSIVO ao perfil GERENTE. Qualquer outro perfil
 *    (FUNCIONARIO ou USUARIO) é redirecionado para /app/mesas —
 *    a checagem é feita logo na primeira linha do doGet().
 *
 * FLUXO PRINCIPAL:
 * 1. Confirma que o usuário logado é GERENTE (senão, redireciona)
 * 2. Busca todas as mesas e conta quantas estão em cada status
 *    (livre / ocupada / reservada) usando Stream.filter().count()
 * 3. Busca todos os pedidos com status "em aberto" (ver PedidoDAO)
 * 4. Calcula o total faturado HOJE (pedidos com status='entregue' e
 *    data_abertura = data de hoje) via query direta (buscarTotalHoje)
 * 5. Limita a lista de "últimos pedidos" aos 8 mais recentes, para
 *    não sobrecarregar a tela
 * 6. Publica tudo como atributos de request e encaminha para a JSP
 *
 * QUERY DE FATURAMENTO DO DIA (buscarTotalHoje):
 * Soma preco_unitario * quantidade de item_pedido, filtrando por:
 * - pedido.status = 'entregue'   (só conta o que já foi de fato pago/entregue)
 * - DATE(pedido.data_abertura) = CURDATE()  (só hoje)
 * - item_pedido.ativo = 1        (ignora itens cancelados)
 *
 * EXEMPLO DE USO:
 * ```
 * GET /app/dashboard
 * ```
 *
 * @author Sistema Integrador
 * @version 3.0
 * @see MesaDAO
 * @see PedidoDAO
 */
@WebServlet("/app/dashboard")
public class DashboardController extends HttpServlet {

    private static final long serialVersionUID=1L;

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
       STEP 5 — Selecionar os 8 pedidos mais recentes para exibição
       STEP 6 — Publicar atributos e encaminhar para a JSP
    */
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)
            throws ServletException,IOException {

        System.out.println("\n========== DASHBOARD CONTROLLER GET ==========");

        // ========== STEP 1: VERIFICAR PERMISSÃO (só GERENTE) ==========
        Usuario u=(Usuario)req.getSession().getAttribute("usuarioLogado");
        System.out.println("👤 Usuário logado: " + (u != null ? u.getNome() + " (" + u.getPerfil() + ")" : "nenhum"));

        if(!"GERENTE".equals(u.getPerfil())){
            System.err.println("❌ Acesso negado: usuário não é GERENTE — redirecionando para /app/mesas");
            res.sendRedirect(req.getContextPath()+"/app/mesas");
            System.out.println("================================================\n");
            return;
        }
        System.out.println("✅ Permissão OK — usuário é GERENTE");

        try(Connection conn=Conexao.getConnection()){

            // ========== STEP 2: BUSCAR MESAS E CONTAR POR STATUS ==========
            System.out.println("⏳ Buscando mesas...");
            List<Mesa> mesas=new MesaDAO(conn).listar();
            long livres    = mesas.stream().filter(m->"livre".equals(m.getStatus())).count();
            long ocupadas  = mesas.stream().filter(m->"ocupada".equals(m.getStatus())).count();
            long reservadas= mesas.stream().filter(m->"reservada".equals(m.getStatus())).count();
            System.out.println("✅ Mesas: " + mesas.size() + " total | "
                    + livres + " livres | " + ocupadas + " ocupadas | " + reservadas + " reservadas");

            // ========== STEP 3: BUSCAR PEDIDOS ABERTOS ==========
            System.out.println("⏳ Buscando pedidos abertos...");
            List<Pedido> abertos=new PedidoDAO(conn).listarAbertos();
            System.out.println("✅ " + abertos.size() + " pedido(s) em aberto");

            // ========== STEP 4: CALCULAR FATURAMENTO DE HOJE ==========
            System.out.println("⏳ Calculando faturamento de hoje...");
            BigDecimal totalHoje=buscarTotalHoje(conn);
            System.out.println("✅ Faturamento de hoje: R$ " + totalHoje);

            // ========== STEP 5: LIMITAR ÚLTIMOS PEDIDOS EXIBIDOS (máx. 8) ==========
            List<Pedido> ultimos=abertos.size()>8?abertos.subList(0,8):abertos;
            System.out.println("📋 Exibindo " + ultimos.size() + " pedido(s) recente(s) no painel");

            // ========== STEP 6: PUBLICAR ATRIBUTOS E ENCAMINHAR PARA A JSP ==========
            req.setAttribute("totalMesas",mesas.size());
            req.setAttribute("mesasLivres",livres);
            req.setAttribute("mesasOcupadas",ocupadas);
            req.setAttribute("mesasReservadas",reservadas);
            req.setAttribute("pedidosAbertos",abertos.size());
            req.setAttribute("totalHoje",totalHoje);
            req.setAttribute("ultimosPedidos",ultimos);
            req.setAttribute("paginaAtiva","dashboard");

            System.out.println("➡️ Encaminhando para dashboard.jsp");
            req.getRequestDispatcher("/WEB-INF/views/dashboard/dashboard.jsp").forward(req,res);

        }catch(Exception e){
            // ========== TRATAMENTO DE ERRO ==========
            System.err.println("❌ ERRO ao carregar dashboard:");
            System.err.println("   Tipo: " + e.getClass().getName());
            System.err.println("   Mensagem: " + e.getMessage());
            e.printStackTrace();
            req.setAttribute("erro","Erro ao carregar dashboard.");
            req.getRequestDispatcher("/WEB-INF/views/error/500.jsp").forward(req,res);
        }
        System.out.println("================================================\n");
    }

    /* ================================================================
       HELPER: CALCULAR FATURAMENTO DO DIA
       ================================================================

       Executa uma query direta (não passa pelo DAO de Pedido/ItemPedido
       porque é um agregado simples, específico desta tela) que soma
       preco_unitario * quantidade de todos os itens de pedidos:

       - já ENTREGUES (status='entregue')     → só conta o que de fato
                                                 virou receita
       - abertos HOJE (DATE(data_abertura) = CURDATE())
       - com o item ainda ATIVO (ip.ativo=1)  → ignora itens cancelados
                                                 individualmente dentro
                                                 de um pedido entregue

       Retorna BigDecimal.ZERO se não houver nenhum resultado (o
       COALESCE no SQL já garante isso no nível do banco, mas o código
       Java também trata defensivamente o caso de ResultSet vazio).
    */
    private BigDecimal buscarTotalHoje(Connection conn) throws Exception {
        String sql="SELECT COALESCE(SUM(ip.preco_unitario*ip.quantidade),0) AS total "+
                   "FROM item_pedido ip INNER JOIN pedido p ON ip.pedido_id=p.id_pedido "+
                   "WHERE p.status='entregue' AND DATE(p.data_abertura)=CURDATE() AND ip.ativo=1";

        System.out.println("   SQL: " + sql.replaceAll("\\s+", " "));

        try(PreparedStatement s=conn.prepareStatement(sql);ResultSet r=s.executeQuery()){
            BigDecimal total = r.next()?r.getBigDecimal("total"):BigDecimal.ZERO;
            System.out.println("   Resultado: R$ " + total);
            return total;
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

   DADOS EXIBIDOS NO PAINEL:
   - totalMesas / mesasLivres / mesasOcupadas / mesasReservadas
   - pedidosAbertos (contagem total)
   - ultimosPedidos (até 8 pedidos abertos mais recentes)
   - totalHoje (faturamento do dia, só pedidos já entregues)

   REGRA DE NEGÓCIO IMPORTANTE:
   ✅ Faturamento "de hoje" considera SOMENTE pedidos com
      status='entregue' — pedidos ainda abertos/em preparo/prontos
      NÃO entram na soma, mesmo que tenham sido abertos hoje

   DEPENDÊNCIAS:
   - MesaDAO: acesso à tabela mesa
   - PedidoDAO: acesso à tabela pedido (pedidos abertos)
   - Query direta (PreparedStatement) para o agregado de faturamento
   - Conexao: gerenciamento de conexões

   OBSERVAÇÕES:
   - Conexão fecha automaticamente (try-with-resources)
   - Logs detalhados em cada etapa (System.out / System.err)
   - Contagens de mesa feitas em memória via Stream, após buscar
     todas as mesas de uma vez (dataset pequeno, sem necessidade de
     3 queries agregadas separadas no banco)
   ================================================================ */
