package fi.aluesarjat.prototype;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.servlet.ServletException;

import org.junit.Test;

public class AreasServletTest extends AbstractServletTest{
    
    private AreasServlet servlet;

    @Override
    public void setUp(){
        super.setUp();
        servlet = new AreasServlet();
    }

    @Test
    public void Correct_CharsetEncoding() throws ServletException, IOException{
        servlet.service(request, response);
        assertEquals("UTF-8", response.getCharacterEncoding());
        assertEquals("application/json", response.getContentType());
    }

}
