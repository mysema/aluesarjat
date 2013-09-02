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

public class AreasServlet extends HttpServlet {

    private static final long serialVersionUID = -1216436366722412316L;

    private static final long LAST_MODIFIED = System.currentTimeMillis() / 1000 * 1000;

    private final String areas, areas1, areas2, areas3, areas4;

    public AreasServlet() {
        try {
            areas = IOUtils.toString(getResourceAsStream("/areas.json"),"UTF-8");
            areas1 = IOUtils.toString(getResourceAsStream("/area1.json"),"UTF-8");
            areas2 = IOUtils.toString(getResourceAsStream("/area2.json"),"UTF-8");
            areas3 = IOUtils.toString(getResourceAsStream("/area3.json"),"UTF-8");
            areas4 = IOUtils.toString(getResourceAsStream("/area4.json"),"UTF-8");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;

        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        if (ifModifiedSince >= LAST_MODIFIED) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setDateHeader("Last-Modified", System.currentTimeMillis());
        response.setHeader("Cache-Control", "max-age=86400");

        String level = request.getParameter("level");
        String content;
        if (level == null) {
            content = areas;
        } else if ("1".equals(level)) {
            content = areas1;
        } else if ("2".equals(level)) {
            content = areas2;
        } else if ("3".equals(level)) {
            content = areas3;
        } else if ("4".equals(level)) {
            content = areas4;
        } else {
            throw new IllegalArgumentException("Illegal level " + level);
        }
        response.getWriter().append(content);
        response.getWriter().flush();

    }

    private InputStream getResourceAsStream(String resource) {
        return AreasServlet.class.getResourceAsStream(resource);
    }

}
