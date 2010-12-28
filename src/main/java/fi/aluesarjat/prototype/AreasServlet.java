package fi.aluesarjat.prototype;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

public class AreasServlet extends AbstractSPARQLServlet{

    private static final long serialVersionUID = -1216436366722412316L;
    
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setDateHeader("Last-Modified", System.currentTimeMillis());
        
        String level = request.getParameter("level");
        String resource = "/areas.json";
        if ("1".equals(level)){
            resource = "/area1.json";
        }else if ("2".equals(level)){
            resource = "/area2.json";
        }else if ("3".equals(level)){
            resource = "/area3.json";
        }
        IOUtils.copy(getClass().getResourceAsStream(resource), response.getOutputStream());
        
    }

}
