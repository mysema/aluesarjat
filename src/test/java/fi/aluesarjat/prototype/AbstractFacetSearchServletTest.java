package fi.aluesarjat.prototype;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.mysema.rdfbean.sesame.MemoryRepository;
import com.mysema.stat.scovo.NamespaceHandler;

import fi.aluesarjat.prototype.guice.ModuleUtils;

public abstract class AbstractFacetSearchServletTest {

    protected static MemoryRepository repository;

    protected MockHttpServletRequest request;

    protected MockHttpServletResponse response;

    @BeforeClass
    public static void setUpClass() throws ServletException, IOException{
        String baseURI = "http://localhost:8080/rdf/";
        repository = new MemoryRepository();
        repository.setSources(ModuleUtils.getSources(baseURI));
        repository.initialize();

        NamespaceHandler namespaceHandler = new NamespaceHandler(repository);
        DataService dataService = new DataService(repository, namespaceHandler, baseURI, DataService.Mode.NONTHREADED.name(), "true");
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
