package br.com.restaurante.controller;

import java.io.IOException;
import java.sql.Connection;

import org.mindrot.jbcrypt.BCrypt;

import br.com.restaurante.dao.UsuarioDAO;
import br.com.restaurante.model.Usuario;
import br.com.restaurante.utils.Conexao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Gerencia autenticação do sistema.
 *
 * Rotas:
 *   GET  /auth/login  → exibe login.jsp
 *   POST /auth/login  → valida credenciais, cria sessão, redireciona por perfil
 *   GET  /auth/logout → invalida sessão, redireciona para /auth/login
 *
 * Redirecionamento pós-login por perfil:
 *   GERENTE              → /app/dashboard
 *   FUNCIONARIO atendente→ /app/mesas
 *   FUNCIONARIO cozinha  → /app/fila
 *   USUARIO              → /app/cardapio
 *
 * Sessão:
 *   Atributo "usuarioLogado" → objeto Usuario
 *   Timeout: 30 minutos (configurado no web.xml)
 */
@WebServlet({"/auth/login", "/auth/logout"})
public class AuthController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // ----------------------------------------------------------------
    // GET — exibe formulário ou executa logout
    // ----------------------------------------------------------------

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getServletPath();

        switch (path) {
            case "/auth/login"  -> exibirLogin(request, response);
            case "/auth/logout" -> executarLogout(request, response);
            default             -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ----------------------------------------------------------------
    // POST — processa autenticação
    // ----------------------------------------------------------------

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getServletPath();

        if ("/auth/login".equals(path)) {
            processarLogin(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ----------------------------------------------------------------
    // EXIBIR LOGIN
    // ----------------------------------------------------------------

    private void exibirLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Se já está logado, redireciona direto para a área correta
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("usuarioLogado") != null) {
            response.sendRedirect(destino(
                    (Usuario) session.getAttribute("usuarioLogado"), request));
            return;
        }

        request.getRequestDispatcher("/WEB-INF/views/auth/login.jsp")
               .forward(request, response);
    }

    // ----------------------------------------------------------------
    // PROCESSAR LOGIN
    // ----------------------------------------------------------------

    private void processarLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String login = request.getParameter("login");
        String senha = request.getParameter("senha");

        // Validação básica de campos vazios
        if (login == null || login.isBlank() || senha == null || senha.isBlank()) {
            request.setAttribute("erro", "Login e senha são obrigatórios.");
            request.getRequestDispatcher("/WEB-INF/views/auth/login.jsp")
                   .forward(request, response);
            return;
        }

        try (Connection conn = Conexao.getConnection()) {

            Usuario usuario = new UsuarioDAO(conn).buscarPorLogin(login.trim());

            // Usuário não encontrado ou senha incorreta — mesma mensagem genérica
            if (usuario == null || !BCrypt.checkpw(senha, usuario.getSenha())) {
                request.setAttribute("erro", "Login ou senha incorretos.");
                request.getRequestDispatcher("/WEB-INF/views/auth/login.jsp")
                       .forward(request, response);
                return;
            }

            // Cria a sessão com o usuário autenticado
            HttpSession session = request.getSession();
            session.setAttribute("usuarioLogado", usuario);
            session.setMaxInactiveInterval(1800); // 30 minutos

            response.sendRedirect(destino(usuario, request));

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("erro", "Erro no sistema. Tente novamente.");
            request.getRequestDispatcher("/WEB-INF/views/auth/login.jsp")
                   .forward(request, response);
        }
    }

    // ----------------------------------------------------------------
    // LOGOUT
    // ----------------------------------------------------------------

    private void executarLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();

        response.sendRedirect(request.getContextPath() + "/auth/login");
    }

    // ----------------------------------------------------------------
    // DESTINO PÓS-LOGIN — resolve URL pela combinação perfil + funcao
    // ----------------------------------------------------------------

    private String destino(Usuario u, HttpServletRequest request) {
        String base = request.getContextPath();

        return switch (u.getPerfil()) {
            case "GERENTE"     -> base + "/app/dashboard";
            case "FUNCIONARIO" -> "cozinha".equals(u.getFuncao())
                                    ? base + "/app/fila"
                                    : base + "/app/mesas";
            default            -> base + "/app/cardapio"; // USUARIO
        };
    }
}
