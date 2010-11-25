package fi.aluesarjat.prototype;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.mysema.rdfbean.model.io.Format;
import com.mysema.rdfbean.model.io.RDFSource;
import com.mysema.rdfbean.sesame.MemoryRepository;
import com.mysema.stat.scovo.NamespaceHandler;
import com.mysema.stat.scovo.SCV;

public abstract class AbstractFacetSearchServletTest {

    protected static MemoryRepository repository;

    protected MockHttpServletRequest request;

    protected MockHttpServletResponse response;

    @BeforeClass
    public static void setUpClass() throws ServletException, IOException{
        repository = new MemoryRepository();
        repository.setSources(new RDFSource[]{
                new RDFSource("classpath:/alue.ttl", Format.TURTLE, "http://localhost:8080/rdf/dimensions/Alue"),
                new RDFSource("classpath:/scovo.rdf", Format.RDFXML, SCV.NS),
                new RDFSource("classpath:/stat.rdf", Format.RDFXML, "http://data.mysema.com/rdf/pcaxis#")});
        repository.initialize();

        NamespaceHandler namespaceHandler = new NamespaceHandler(repository);
        DataService dataService = new DataService(repository, namespaceHandler, "http://localhost:8080/rdf/", "false", "true");
        dataService.setDatasets(Collections.singletonList("A01HKIS_Vaestotulot \".\""));
        dataService.initialize();
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

}
