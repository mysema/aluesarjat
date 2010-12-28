package fi.aluesarjat.prototype;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

public class AreasServlet extends AbstractSPARQLServlet{

    private static final long serialVersionUID = -1216436366722412316L;
    
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletResponse response = (HttpServletResponse)res;
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setDateHeader("Last-Modified", System.currentTimeMillis());
        
        IOUtils.copy(getClass().getResourceAsStream("/areas.json"), response.getOutputStream());
        
    }

}
