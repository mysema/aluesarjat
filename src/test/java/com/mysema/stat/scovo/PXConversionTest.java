package com.mysema.stat.scovo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.Namespaces;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.QueryLanguage;
import com.mysema.rdfbean.model.RDF;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.SPARQLQuery;
import com.mysema.rdfbean.model.io.Format;
import com.mysema.rdfbean.sesame.MemoryRepository;
import com.mysema.stat.pcaxis.Dataset;
import com.mysema.stat.pcaxis.PCAxis;

public class PXConversionTest {

    private Repository repository;
    
    @Before
    public void setUp(){
        repository = new MemoryRepository();
        repository.initialize();
    }
    
    @After
    public void tearDown(){
        repository.close();
    }
    
    @Test
    public void convert() throws IOException {
        PXConverter pxc = new PXConverter(repository, "http://www.aluesarjat.fi/rdf/datasets/");        
        pxc.convert(new Dataset("example-1", PCAxis.parse(getClass().getResourceAsStream("/example-1.px"))));
        pxc.convert(new Dataset("example-2", PCAxis.parse(getClass().getResourceAsStream("/example-2.px"))));
        pxc.convert(new Dataset("example-3", PCAxis.parse(getClass().getResourceAsStream("/example-3.px"))));        
        
        RDFConnection conn = repository.openConnection();
        try {
            SPARQLQuery qry = conn.createQuery(
                    QueryLanguage.SPARQL, 
                    "PREFIX scv:   <" + SCV.NS + "> " +
                    "PREFIX rdf:   <" + RDF.NS + "> " +
                    "PREFIX ex1:   <http://www.aluesarjat.fi/rdf/datasets/example-1#> " +
                    "SELECT ?value " +
                    "WHERE { " +
                    "   ?item rdf:value ?value; " +
                    "       scv:dimension ex1:_049_Espoo, " +
                    "           ex1:_O__Muut_yht_kunn__ja_henk_koht__palv_, " +
                    "           ex1:_1999 ." +
                    "}"
            );
            CloseableIterator<Map<String,NODE>> rs = qry.getTuples();
            try {
                assertTrue(rs.hasNext());
                assertEquals("108640", rs.next().get("value").getValue());
                assertFalse(rs.hasNext());
            } finally {
                rs.close();
            }
        } finally {
            conn.close();
        }
        
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.putAll(Namespaces.DEFAULT);
        namespaces.put(DC.NS, "dc");
        namespaces.put(SCV.NS, "scv");
        namespaces.put("http://www.aluesarjat.fi/rdf/datasets/example-1#", "ex1");
        namespaces.put("http://www.aluesarjat.fi/rdf/datasets/example-2#", "ex2");
        namespaces.put("http://www.aluesarjat.fi/rdf/datasets/example-3#", "ex3");
        
        OutputStream out = new BufferedOutputStream(new FileOutputStream("target/example.ttl"));
        try {
            repository.export(Format.TURTLE,  namespaces, out);
            out.flush();
        } finally {
            out.close();
        }
        
        
    }
    
}
