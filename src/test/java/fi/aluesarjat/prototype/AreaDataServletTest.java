package fi.aluesarjat.prototype;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.servlet.ServletException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.mysema.rdfbean.sesame.MemoryRepository;

import fi.aluesarjat.prototype.guice.ModuleUtils;

public class AreaDataServletTest {

    protected static MemoryRepository repository;

    protected MockHttpServletRequest request;

    protected MockHttpServletResponse response;

    private AreaDataServlet servlet;

    @BeforeClass
    public static void setUpClass() throws ServletException, IOException{
        String baseURI = ModuleUtils.DEFAULT_BASE_URI;
        repository = new MemoryRepository();
        repository.setSources(ModuleUtils.getSources(baseURI));
        repository.initialize();
    }

    @AfterClass
    public static void tearDownClass(){
        repository.close();
    }

    @Before
    public void setUp(){
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        servlet = new AreaDataServlet(repository, ModuleUtils.DEFAULT_BASE_URI);
    }

    @Test
    public void Correct_Encoding() throws ServletException, IOException{
        request.setParameter("area", "_091_1_Eteläinen_suurpiiri");
        servlet.service(request, response);
        assertEquals("UTF-8", response.getCharacterEncoding());
        assertEquals("application/json", response.getContentType());
    }

    public static void main(String[] args) throws ServletException, IOException{
        setUpClass();
        AreaDataServletTest test = new AreaDataServletTest();
        test.setUp();
        try{
            test.Correct_Encoding();
        }finally{
            tearDownClass();
        }
    }
}
