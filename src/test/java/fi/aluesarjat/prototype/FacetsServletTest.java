package fi.aluesarjat.prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.servlet.ServletException;

import org.junit.Test;

public class FacetsServletTest extends AbstractFacetSearchServletTest{

    private FacetsServlet servlet;

    @Override
    public void setUp(){
        super.setUp();
        servlet = new FacetsServlet(repository);
    }

    @Test
    public void Correct_CharsetEncoding() throws ServletException, IOException{
        servlet.service(request, response);
        assertEquals("UTF-8", response.getCharacterEncoding());
        assertEquals("application/json", response.getContentType());
        System.err.println(response.getContentAsString());
        assertTrue(response.getContentAsString().contains("Etel\u00E4inen suurpiiri"));
    }
}
