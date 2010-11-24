package com.mysema.stat.scovo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mysema.commons.lang.IteratorAdapter;
import com.mysema.rdfbean.model.QueryLanguage;
import com.mysema.rdfbean.model.RDF;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.SPARQLQuery;
import com.mysema.rdfbean.model.io.Format;
import com.mysema.rdfbean.model.io.RDFSource;
import com.mysema.rdfbean.sesame.SesameRepository;
import com.mysema.stat.META;
import com.mysema.stat.pcaxis.PCAxisParser;

public abstract class AbstractDatasetHandlerTest {

    private SesameRepository repository;

    @Before
    public void setUp(){
        repository = createRepository();
        repository.setSources(new RDFSource[]{
                new RDFSource("classpath:/alue.ttl", Format.TURTLE, "http://localhost:8080/rdf/dimensions/Alue"),
                new RDFSource("classpath:/scovo.rdf", Format.RDFXML, SCV.NS),
                new RDFSource("classpath:/stat.rdf", Format.RDFXML, "http://data.mysema.com/rdf/pcaxis#")});
        repository.initialize();
    }

    protected abstract SesameRepository createRepository();

    @After
    public void tearDown(){
        repository.close();
    }

    @Test
    public void Parse_and_Query() throws IOException {
        NamespaceHandler namespaceHandler = new NamespaceHandler(repository);
        RDFDatasetHandler handler = new RDFDatasetHandler(repository, namespaceHandler, "http://www.aluesarjat.fi/rdf/");
        PCAxisParser parser = new PCAxisParser(handler);
        parser.parse("A01HKIS_Vaestotulot", getClass().getResourceAsStream("/data/A01HKIS_Vaestotulot.px"));

        RDFConnection connection = repository.openConnection();
        try{
            // exists queries
            assertTrue(connection.exists(null, SCV.dataset, null, null, false));
            assertTrue(connection.exists(null, SCV.dimension, null, null, false));
            assertTrue(connection.exists(null, RDF.type, SCV.Dataset, null, false));
            assertTrue(connection.exists(null, META.nsPrefix, null, null, false));

            // SPARQL queries
            SPARQLQuery query = connection.createQuery(QueryLanguage.SPARQL, "select ?ns ?prefix  where { ?ns <"+META.nsPrefix.getId()+"> ?prefix }");
            assertFalse(IteratorAdapter.asList(query.getTuples()).isEmpty());
        }finally{
            connection.close();
        }
    }


}
