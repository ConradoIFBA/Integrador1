package br.com.restaurante.filter;
import java.io.IOException;
import br.com.restaurante.model.Usuario;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
@WebFilter("/app/*")
public class AuthFilter implements Filter {
    @Override public void init(FilterConfig c) throws ServletException {}
    @Override public void doFilter(ServletRequest req,ServletResponse res,FilterChain chain)
            throws IOException,ServletException {
        HttpServletRequest  request =(HttpServletRequest) req;
        HttpServletResponse response=(HttpServletResponse)res;
        HttpSession session=request.getSession(false);
        Usuario u=(session!=null)?(Usuario)session.getAttribute("usuarioLogado"):null;
        if(u==null){response.sendRedirect(request.getContextPath()+"/auth/login");return;}
        chain.doFilter(req,res);
    }
    @Override public void destroy(){}
}
