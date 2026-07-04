package br.com.restaurante.filter;
import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
@WebFilter("/*")
public class CharsetFilter implements Filter {
    private String encoding;
    @Override public void init(FilterConfig c) throws ServletException {
        encoding=c.getInitParameter("encoding");if(encoding==null)encoding="UTF-8";}
    @Override public void doFilter(ServletRequest req,ServletResponse res,FilterChain chain)
            throws IOException,ServletException {
        if(req.getCharacterEncoding()==null)req.setCharacterEncoding(encoding);
        res.setCharacterEncoding(encoding);chain.doFilter(req,res);}
    @Override public void destroy(){}
}
