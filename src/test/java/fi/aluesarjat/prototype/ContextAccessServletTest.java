package fi.aluesarjat.prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.mysema.rdfbean.model.Format;
import com.mysema.rdfbean.model.LIT;
import com.mysema.rdfbean.model.RDF;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.RDFS;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.model.UID;
import com.mysema.rdfbean.sesame.MemoryRepository;

public class ContextAccessServletTest {

    private static MemoryRepository repository;

    private ContextAccessServlet servlet;

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    @BeforeClass
    public static void setUpClass() throws ServletException {
        repository = new MemoryRepository();
        repository.initialize();
        RDFConnection connection = repository.openConnection();
        try {
            UID uid = new UID("http://localhost:80/data/test");
            Set<STMT> stmts = new HashSet<STMT>();
            stmts.add(new STMT(uid, RDF.type, RDFS.Class, uid));
            stmts.add(new STMT(uid, RDFS.label, new LIT("label"), uid));
            connection.update(Collections.<STMT>emptySet(), stmts);
        } finally {
            connection.close();
        }
    }

    @AfterClass
    public static void tearDownClass() {
        repository.close();
    }

    @Before
    public void setUp() {
        servlet = new ContextAccessServlet(repository);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    public void IfModifiedSince_Handling() throws ServletException, IOException {
        request.setRequestURI("/data/test");
        servlet.service(request, response);
        assertEquals(200, response.getStatus());

        Object lastModified = response.getHeader("Last-Modified");
        request.addHeader("If-Modified-Since", lastModified);
        servlet.service(request, response);
        assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatus());
    }

    @Test
    public void Get_Available_Context() throws ServletException, IOException {
        request.setRequestURI("/data/test");
        servlet.service(request, response);

        assertEquals(Format.RDFXML.getMimetype(), response.getContentType());
        assertTrue(response.getContentAsString().contains("rdf:RDF"));
        assertTrue(response.getContentAsString().contains("type"));
        assertTrue(response.getContentAsString().contains("label"));
    }

    @Test
    public void Get_Available_Context_as_Turtle() throws ServletException, IOException {
        request.setRequestURI("/data/test");
        request.addHeader("Accept", Format.TURTLE.getMimetype());
        servlet.service(request, response);

        assertEquals(Format.TURTLE.getMimetype(), response.getContentType());
        assertFalse(response.getContentAsString().contains("rdf:RDF"));
        assertTrue(response.getContentAsString().contains(" a "));
    }

    @Test
    public void Get_Unavailable_Context() throws ServletException, IOException {
        request.setRequestURI("/data/unknown");
        servlet.service(request, response);

        assertEquals(Format.RDFXML.getMimetype(), response.getContentType());
        assertTrue(response.getContentAsString().contains("rdf:RDF"));
        assertFalse(response.getContentAsString().contains("type"));
        assertFalse(response.getContentAsString().contains("label"));
    }

    @Test
    public void Get_with_Html_Accept() throws ServletException, IOException {
        request.setRequestURI("/data/unknown");
        request.addHeader("Accept", "text/html");
        servlet.service(request, response);

        assertEquals(Format.RDFXML.getMimetype(), response.getContentType());
    }

}
