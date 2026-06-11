package br.com.restaurante.filter;

import java.io.IOException;
import br.com.restaurante.model.Usuario;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Intercepta todas as requisições para /app/* e verifica
 * se há um usuário autenticado na sessão.
 *
 * Se não houver → redireciona para /auth/login.
 * Se houver     → deixa a requisição seguir normalmente.
 */
@WebFilter("/app/*")
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig config) throws ServletException {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse res,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        HttpSession session = request.getSession(false);
        Usuario usuario = (session != null)
                ? (Usuario) session.getAttribute("usuarioLogado")
                : null;

        if (usuario == null) {
            // Sessão inexistente ou expirada — redireciona para login
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
