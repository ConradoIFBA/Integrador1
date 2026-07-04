package br.com.restaurante.controller;
import java.io.IOException;
import java.sql.Connection;
import org.mindrot.jbcrypt.BCrypt;
import br.com.restaurante.dao.UsuarioDAO;
import br.com.restaurante.model.Usuario;
import br.com.restaurante.utils.Conexao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
@WebServlet({"/auth/login","/auth/cadastro","/auth/logout"})
public class AuthController extends HttpServlet {
    private static final long serialVersionUID=1L;
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)
            throws ServletException,IOException {
        switch(req.getServletPath()){
            case "/auth/login"    -> exibirLogin(req,res);
            case "/auth/cadastro" -> exibirCadastro(req,res);
            case "/auth/logout"   -> logout(req,res);
            default               -> res.sendError(404);
        }
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)
            throws ServletException,IOException {
        switch(req.getServletPath()){
            case "/auth/login"    -> processarLogin(req,res);
            case "/auth/cadastro" -> processarCadastro(req,res);
            default               -> res.sendError(404);
        }
    }
    private void exibirLogin(HttpServletRequest req,HttpServletResponse res)
            throws ServletException,IOException {
        HttpSession s=req.getSession(false);
        if(s!=null&&s.getAttribute("usuarioLogado")!=null){
            res.sendRedirect(destino((Usuario)s.getAttribute("usuarioLogado"),req));return;}
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req,res);
    }
    private void exibirCadastro(HttpServletRequest req,HttpServletResponse res)
            throws ServletException,IOException {
        HttpSession s=req.getSession(false);
        if(s!=null&&s.getAttribute("usuarioLogado")!=null){
            res.sendRedirect(destino((Usuario)s.getAttribute("usuarioLogado"),req));return;}
        req.getRequestDispatcher("/WEB-INF/views/auth/cadastro.jsp").forward(req,res);
    }
    private void processarLogin(HttpServletRequest req,HttpServletResponse res)
            throws ServletException,IOException {
        String login=req.getParameter("login"),senha=req.getParameter("senha");
        if(login==null||login.isBlank()||senha==null||senha.isBlank()){
            req.setAttribute("erro","Login e senha são obrigatórios.");
            req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req,res);return;}
        try(Connection conn=Conexao.getConnection()){
            Usuario u=new UsuarioDAO(conn).buscarPorLogin(login.trim());
            if(u==null||!BCrypt.checkpw(senha,u.getSenha())){
                req.setAttribute("erro","Login ou senha incorretos.");
                req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req,res);return;}
            HttpSession session=req.getSession();
            session.setAttribute("usuarioLogado",u);session.setMaxInactiveInterval(1800);
            res.sendRedirect(destino(u,req));
        }catch(Exception e){
            e.printStackTrace();req.setAttribute("erro","Erro no sistema. Tente novamente.");
            req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req,res);}
    }
    private void processarCadastro(HttpServletRequest req,HttpServletResponse res)
            throws ServletException,IOException {
        String nome=req.getParameter("nome"),login=req.getParameter("login"),
               senha=req.getParameter("senha"),confirmar=req.getParameter("confirmarSenha");
        if(nome==null||nome.isBlank()){req.setAttribute("erro","Nome obrigatório.");
            req.getRequestDispatcher("/WEB-INF/views/auth/cadastro.jsp").forward(req,res);return;}
        if(login==null||login.length()<3){req.setAttribute("erro","Login deve ter no mínimo 3 caracteres.");
            req.getRequestDispatcher("/WEB-INF/views/auth/cadastro.jsp").forward(req,res);return;}
        if(senha==null||senha.length()<6){req.setAttribute("erro","Senha deve ter no mínimo 6 caracteres.");
            req.getRequestDispatcher("/WEB-INF/views/auth/cadastro.jsp").forward(req,res);return;}
        if(!senha.equals(confirmar)){req.setAttribute("erro","As senhas não coincidem.");
            req.getRequestDispatcher("/WEB-INF/views/auth/cadastro.jsp").forward(req,res);return;}
        try(Connection conn=Conexao.getConnection()){
            UsuarioDAO dao=new UsuarioDAO(conn);
            if(dao.buscarPorLogin(login.trim())!=null){
                req.setAttribute("erro","Login já está em uso.");
                req.getRequestDispatcher("/WEB-INF/views/auth/cadastro.jsp").forward(req,res);return;}
            Usuario u=new Usuario();u.setNome(nome.trim());u.setLogin(login.trim().toLowerCase());
            u.setSenha(BCrypt.hashpw(senha,BCrypt.gensalt()));u.setPerfil("USUARIO");u.setAtivo(true);
            dao.inserir(u);
            req.getSession().setAttribute("sucesso","Cadastro realizado! Faça login.");
            res.sendRedirect(req.getContextPath()+"/auth/login");
        }catch(Exception e){
            e.printStackTrace();req.setAttribute("erro","Erro ao cadastrar.");
            req.getRequestDispatcher("/WEB-INF/views/auth/cadastro.jsp").forward(req,res);}
    }
    private void logout(HttpServletRequest req,HttpServletResponse res) throws IOException {
        HttpSession s=req.getSession(false);if(s!=null)s.invalidate();
        res.sendRedirect(req.getContextPath()+"/auth/login");
    }
    private String destino(Usuario u,HttpServletRequest req){
        String b=req.getContextPath();
        return switch(u.getPerfil()){
            case "GERENTE"     -> b+"/app/dashboard";
            case "FUNCIONARIO" -> "cozinha".equals(u.getFuncao())?b+"/app/fila":b+"/app/mesas";
            default            -> b+"/app/cardapio";
        };
    }
}
