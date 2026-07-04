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
@WebServlet("/app/dashboard")
public class DashboardController extends HttpServlet {
    private static final long serialVersionUID=1L;
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)
            throws ServletException,IOException {
        Usuario u=(Usuario)req.getSession().getAttribute("usuarioLogado");
        if(!"GERENTE".equals(u.getPerfil())){res.sendRedirect(req.getContextPath()+"/app/mesas");return;}
        try(Connection conn=Conexao.getConnection()){
            List<Mesa> mesas=new MesaDAO(conn).listar();
            long livres   =mesas.stream().filter(m->"livre".equals(m.getStatus())).count();
            long ocupadas =mesas.stream().filter(m->"ocupada".equals(m.getStatus())).count();
            long reservadas=mesas.stream().filter(m->"reservada".equals(m.getStatus())).count();
            List<Pedido> abertos=new PedidoDAO(conn).listarAbertos();
            BigDecimal totalHoje=buscarTotalHoje(conn);
            List<Pedido> ultimos=abertos.size()>8?abertos.subList(0,8):abertos;
            req.setAttribute("totalMesas",mesas.size());req.setAttribute("mesasLivres",livres);
            req.setAttribute("mesasOcupadas",ocupadas);req.setAttribute("mesasReservadas",reservadas);
            req.setAttribute("pedidosAbertos",abertos.size());req.setAttribute("totalHoje",totalHoje);
            req.setAttribute("ultimosPedidos",ultimos);req.setAttribute("paginaAtiva","dashboard");
            req.getRequestDispatcher("/WEB-INF/views/dashboard/dashboard.jsp").forward(req,res);
        }catch(Exception e){
            e.printStackTrace();req.setAttribute("erro","Erro ao carregar dashboard.");
            req.getRequestDispatcher("/WEB-INF/views/error/500.jsp").forward(req,res);}
    }
    private BigDecimal buscarTotalHoje(Connection conn) throws Exception {
        String sql="SELECT COALESCE(SUM(ip.preco_unitario*ip.quantidade),0) AS total "+
                   "FROM item_pedido ip INNER JOIN pedido p ON ip.pedido_id=p.id_pedido "+
                   "WHERE p.status='entregue' AND DATE(p.data_abertura)=CURDATE() AND ip.ativo=1";
        try(PreparedStatement s=conn.prepareStatement(sql);ResultSet r=s.executeQuery()){
            return r.next()?r.getBigDecimal("total"):BigDecimal.ZERO;}
    }
}
