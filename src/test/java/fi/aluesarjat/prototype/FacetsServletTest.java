package fi.aluesarjat.prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import fi.aluesarjat.prototype.guice.ModuleUtils;

public class FacetsServletTest extends AbstractServletTest {

    private FacetsServlet servlet;

    @Override
    public void setUp() {
        super.setUp();
        servlet = new FacetsServlet(new SearchServiceImpl(repository, ModuleUtils.DEFAULT_BASE_URI));
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
        System.err.println(response.getContentAsString());
        assertTrue(response.getContentAsString().contains("Etel\u00E4inen suurpiiri"));
    }
    
    @Test
    public void JSON() throws ServletException, IOException {
        servlet.service(request, response);
     
        assertEquals("application/json", response.getContentType());
    }

    @Test
    public void JSONP_Is_Supported() throws ServletException, IOException {
        request.setParameter("callback", "handleResponse");
        servlet.service(request, response);
     
        String res = response.getContentAsString();
        assertTrue(res.startsWith("handleResponse("));
        assertTrue(res.endsWith(")"));
        assertEquals("text/javascript", response.getContentType());
    }
}
