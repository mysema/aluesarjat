package fi.aluesarjat.prototype;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

public class AreasServlet extends HttpServlet{

    private static final long serialVersionUID = -1216436366722412316L;
    
    private final String areas, areas1, areas2, areas3;
    
    public AreasServlet() {
        try {
            areas = IOUtils.toString(getResourceAsStream("/areas.json"),"UTF-8");
            areas1 = IOUtils.toString(getResourceAsStream("/area1.json"),"UTF-8");
            areas2 = IOUtils.toString(getResourceAsStream("/area2.json"),"UTF-8");
            areas3 = IOUtils.toString(getResourceAsStream("/area3.json"),"UTF-8");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }        
    }
    
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setDateHeader("Last-Modified", System.currentTimeMillis());
        
        String level = request.getParameter("level");
        String content = areas;
        if ("1".equals(level)){
            content = areas1;
        }else if ("2".equals(level)){
            content = areas2;
        }else if ("3".equals(level)){
            content = areas3;
        }
        response.getWriter().append(content);
        response.getWriter().flush();
        
    }
    
    private InputStream getResourceAsStream(String resource){
        return AreasServlet.class.getResourceAsStream(resource);
    }

}
