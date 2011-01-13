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
import com.mysema.rdfbean.model.XSD;
import com.mysema.rdfbean.sesame.SesameRepository;
import com.mysema.stat.META;
import com.mysema.stat.pcaxis.PCAxisParser;

import fi.aluesarjat.prototype.guice.ModuleUtils;

public abstract class AbstractDatasetHandlerTest {

    private SesameRepository repository;

    @Before
    public void setUp(){
        repository = createRepository();
        repository.setSources(ModuleUtils.getSources());
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
            long start = System.currentTimeMillis();
            assertTrue(connection.exists(null, SCV.dataset, null, null, false));
            System.err.println((System.currentTimeMillis()-start) + "ms for ?s scv:data ?o");
            assertTrue(connection.exists(null, SCV.dimension, null, null, false));
            System.err.println((System.currentTimeMillis()-start) + "ms for ?s scv:dimension ?o");
            assertTrue(connection.exists(null, RDF.type, SCV.Dataset, null, false));
            System.err.println((System.currentTimeMillis()-start) + "ms for ?s rdf:type ?o");
            assertTrue(connection.exists(null, META.nsPrefix, null, null, false));
            System.err.println((System.currentTimeMillis()-start) + "ms for ?s meta:nsPrefix ?o");

            // SPARQL queries
            SPARQLQuery query = connection.createQuery(QueryLanguage.SPARQL, "select ?ns ?prefix  where { ?ns <"+META.nsPrefix.getId()+"> ?prefix }");
            assertFalse(IteratorAdapter.asList(query.getTuples()).isEmpty());
            
            String prefixes = "PREFIX rdf: <" +RDF.NS + ">\nPREFIX xsd: <" +XSD.NS + ">\n"; 
            query = connection.createQuery(QueryLanguage.SPARQL, prefixes + "select * where { ?item rdf:value ?value . FILTER( datatype(?value) = xsd:decimal ) . }");
            assertFalse(IteratorAdapter.asList(query.getTuples()).isEmpty());
        }finally{
            connection.close();
        }
    }


}
