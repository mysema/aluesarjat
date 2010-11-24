package fi.aluesarjat.prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.mysema.rdfbean.model.io.Format;
import com.mysema.rdfbean.model.io.RDFSource;
import com.mysema.rdfbean.sesame.MemoryRepository;
import com.mysema.stat.scovo.NamespaceHandler;
import com.mysema.stat.scovo.SCV;

public class FacetsServletTest {

    private static MemoryRepository repository;

    private static FacetsServlet servlet;

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    @BeforeClass
    public static void setUpClass() throws ServletException, IOException{
        repository = new MemoryRepository();
        repository.setSources(new RDFSource[]{
                new RDFSource("classpath:/alue.ttl", Format.TURTLE, "http://localhost:8080/rdf/dimensions/Alue"),
                new RDFSource("classpath:/scovo.rdf", Format.RDFXML, SCV.NS),
                new RDFSource("classpath:/stat.rdf", Format.RDFXML, "http://data.mysema.com/rdf/pcaxis#")});
        repository.initialize();

        NamespaceHandler namespaceHandler = new NamespaceHandler(repository);
        DataService dataService = new DataService(repository, namespaceHandler, "http://localhost:8080/rdf/", "true");
        dataService.setDatasets(Collections.singletonList("A01HKIS_Vaestotulot \".\""));
        dataService.setSequential(true);
        dataService.initialize();

        servlet = new FacetsServlet(repository);
    }

    @AfterClass
    public static void tearDownClass(){
        repository.close();
    }

    @Before
    public void setUp(){
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    public void Initial() throws ServletException, IOException{
        servlet.service(request, response);
        assertEquals("UTF-8", response.getCharacterEncoding());
        assertEquals("application/json", response.getContentType());
//        System.err.println(response.getContentAsString());
        assertTrue(response.getContentAsString().contains("Etel\u00E4inen suurpiiri"));
    }
}
