package fi.aluesarjat.prototype;

import static org.junit.Assert.*;

import java.io.IOException;

import javax.servlet.ServletException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.mysema.rdfbean.sesame.MemoryRepository;

public class ContextAccessServletTest {

    private static MemoryRepository repository;
    
    @BeforeClass
    public static void setUpClass() throws ServletException{
        repository = new MemoryRepository();
        repository.initialize();                
    }
    
    @AfterClass
    public static void tearDownClass(){
        repository.close();
    }
    
    @Test
    public void Get() throws ServletException, IOException{
        ContextAccessServlet servlet = new ContextAccessServlet(repository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        assertTrue(response.getContentAsString().contains("rdf:RDF"));
    }
    
}
