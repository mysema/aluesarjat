package fi.aluesarjat.prototype;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

public class AreasServletTest extends AbstractServletTest {

    private AreasServlet servlet;

    @Override
    public void setUp() {
        super.setUp();
        servlet = new AreasServlet();
    }

    @Test
    public void IfModifiedSince_Handling() throws ServletException, IOException {
        servlet.service(request, response);
        assertEquals(200, response.getStatus());

        Object lastModified = response.getHeader("Last-Modified");
        request.addHeader("If-Modified-Since", lastModified);
        servlet.service(request, response);
        assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatus());
    }

    @Test
    public void Correct_CharsetEncoding() throws ServletException, IOException {
        servlet.service(request, response);
        assertEquals("UTF-8", response.getCharacterEncoding());
        assertEquals("application/json", response.getContentType());
    }

    @Test
    public void Level1() throws ServletException, IOException {
        request.setParameter("level", "1");
        servlet.service(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void Level2() throws ServletException, IOException {
        request.setParameter("level", "2");
        servlet.service(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void Level3() throws ServletException, IOException {
        request.setParameter("level", "3");
        servlet.service(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void Level4() throws ServletException, IOException {
        request.setParameter("level", "4");
        servlet.service(request, response);
        assertEquals(200, response.getStatus());
    }

}
