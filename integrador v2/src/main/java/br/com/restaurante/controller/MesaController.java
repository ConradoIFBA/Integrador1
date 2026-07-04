package br.com.restaurante.controller;
import java.io.IOException;
import java.sql.Connection;
import java.util.List;
import br.com.restaurante.dao.*;
import br.com.restaurante.model.*;
import br.com.restaurante.utils.Conexao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
@WebServlet("/app/mesas")
public class MesaController extends HttpServlet {
    private static final long serialVersionUID=1L;
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)
            throws ServletException,IOException {
        if(!temPermissao(req)){res.sendRedirect(req.getContextPath()+"/app/dashboard");return;}
        if("historico".equals(req.getParameter("acao"))) exibirHistorico(req,res);
        else listarMesas(req,res);
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)
            throws ServletException,IOException {
        if(!temPermissao(req)){res.sendRedirect(req.getContextPath()+"/app/dashboard");return;}
        switch(req.getParameter("acao")!=null?req.getParameter("acao"):""){
            case "abrirMesa"  -> mudarStatus(req,res,"ocupada","Mesa aberta por ");
            case "fecharMesa" -> mudarStatus(req,res,"livre","Mesa fechada por ");
            case "reservar"   -> mudarStatus(req,res,"reservada","Mesa reservada por ");
            default           -> res.sendRedirect(req.getContextPath()+"/app/mesas");
        }
    }
    private void listarMesas(HttpServletRequest req,HttpServletResponse res)
            throws ServletException,IOException {
        try(Connection conn=Conexao.getConnection()){
            List<Mesa> mesas=new MesaDAO(conn).listar();
            req.setAttribute("mesas",mesas);
            req.setAttribute("livres",   mesas.stream().filter(m->"livre".equals(m.getStatus())).count());
            req.setAttribute("ocupadas", mesas.stream().filter(m->"ocupada".equals(m.getStatus())).count());
            req.setAttribute("reservadas",mesas.stream().filter(m->"reservada".equals(m.getStatus())).count());
            req.setAttribute("paginaAtiva","mesas");
            req.getRequestDispatcher("/WEB-INF/views/mesa/mesas.jsp").forward(req,res);
        }catch(Exception e){e.printStackTrace();
            req.getRequestDispatcher("/WEB-INF/views/error/500.jsp").forward(req,res);}
    }
    private void exibirHistorico(HttpServletRequest req,HttpServletResponse res)
            throws ServletException,IOException {
        int id=parseId(req.getParameter("id"));
        if(id<=0){res.sendRedirect(req.getContextPath()+"/app/mesas");return;}
        try(Connection conn=Conexao.getConnection()){
            Mesa mesa=new MesaDAO(conn).buscarPorId(id);
            if(mesa==null){res.sendRedirect(req.getContextPath()+"/app/mesas");return;}
            req.setAttribute("mesa",mesa);
            req.setAttribute("historico",new HistoricoMesaDAO(conn).listarPorMesa(id));
            req.setAttribute("pedidosAbertos",new PedidoDAO(conn).listarPorMesa(id));
            req.setAttribute("paginaAtiva","mesas");
            req.getRequestDispatcher("/WEB-INF/views/mesa/historico_mesa.jsp").forward(req,res);
        }catch(Exception e){e.printStackTrace();
            res.sendRedirect(req.getContextPath()+"/app/mesas");}
    }
    private void mudarStatus(HttpServletRequest req,HttpServletResponse res,
                             String novoStatus,String prefixoLog) throws IOException {
        int id=parseId(req.getParameter("id"));
        if(id<=0){res.sendRedirect(req.getContextPath()+"/app/mesas");return;}
        String operador=req.getParameter("operador");
        if(operador==null||operador.isBlank())
            operador=((Usuario)req.getSession().getAttribute("usuarioLogado")).getLogin();
        try(Connection conn=Conexao.getConnection()){
            conn.setAutoCommit(false);
            try{
                new MesaDAO(conn).atualizarStatus(id,novoStatus);
                new HistoricoMesaDAO(conn).registrar(new HistoricoMesa(id,prefixoLog+operador));
                conn.commit();
            }catch(Exception e){conn.rollback();e.printStackTrace();}
        }catch(Exception e){e.printStackTrace();}
        res.sendRedirect(req.getContextPath()+"/app/mesas");
    }
    private boolean temPermissao(HttpServletRequest req){
        Usuario u=(Usuario)req.getSession().getAttribute("usuarioLogado");
        return u!=null&&("GERENTE".equals(u.getPerfil())
            ||("FUNCIONARIO".equals(u.getPerfil())&&"atendente".equals(u.getFuncao())));
    }
    private int parseId(String v){try{return Integer.parseInt(v);}catch(Exception e){return -1;}}
}
