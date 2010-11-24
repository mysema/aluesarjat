package fi.aluesarjat.prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.servlet.ServletException;

import org.junit.Test;

public class FacetsServletTest extends AbstractFacetSearchServletTest{

    @Test
    public void Initial() throws ServletException, IOException{
        servlet.service(request, response);
        assertEquals("UTF-8", response.getCharacterEncoding());
        assertEquals("application/json", response.getContentType());
        System.err.println(response.getContentAsString());
        assertTrue(response.getContentAsString().contains("Etel\u00E4inen suurpiiri"));
    }
}
