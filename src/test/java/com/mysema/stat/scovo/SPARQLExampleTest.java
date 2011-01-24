package com.mysema.stat.scovo;

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
import com.mysema.rdfbean.model.DC;
import com.mysema.rdfbean.model.DCTERMS;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.QueryLanguage;
import com.mysema.rdfbean.model.RDF;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.SPARQLQuery;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.model.UID;
import com.mysema.rdfbean.model.io.Format;
import com.mysema.rdfbean.sesame.MemoryRepository;
import com.mysema.stat.META;
import com.mysema.stat.pcaxis.PCAxisParser;

public class SPARQLExampleTest {

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
        RDFConnection conn = repository.openConnection();
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.putAll(Namespaces.DEFAULT);
        namespaces.put(DC.NS, "dc");
        namespaces.put(DCTERMS.NS, "dcterms");
        namespaces.put(SCV.NS, "scv");
        namespaces.put(META.NS, "meta");
        namespaces.put("http://www.aluesarjat.fi/rdf/domain#", "domain");
        namespaces.put("http://www.aluesarjat.fi/rdf/datasets/example-1#", "ex1");
        namespaces.put("http://www.aluesarjat.fi/rdf/datasets/example-2#", "ex2");

        try {
            NamespaceHandler namespaceHandler = new NamespaceHandler(repository);
            RDFDatasetHandler handler = new RDFDatasetHandler(repository, namespaceHandler, "http://www.aluesarjat.fi/rdf/");
            PCAxisParser parser = new PCAxisParser(handler);

            parser.parse("example-1", getClass().getResourceAsStream("/example-1.px"));
            parser.parse("example-2", getClass().getResourceAsStream("/example-2.px"));

            SPARQLQuery qry = conn.createQuery(
                    QueryLanguage.SPARQL,
                    "PREFIX scv:   <" + SCV.NS + "> " +
                    "PREFIX rdf:   <" + RDF.NS + "> " +
                    "PREFIX ex1:   <http://www.aluesarjat.fi/rdf/datasets/example-1#> " +
                    "SELECT ?value " +
                    "WHERE { " +
                    "   ?item rdf:value ?value; " +
                    "       scv:dimension ex1:_049_Espoo, " +
                    "           ex1:_O__Muut_yht_kunn__ja_henk_koht__palv_ . " +
//                    "           ex1:_1999 ." +
                    "}"
            );
            CloseableIterator<Map<String,NODE>> rs = qry.getTuples();
            try {
                while (rs.hasNext()) {
                    System.out.println(rs.next());
                }
//                assertTrue(rs.hasNext());
//                assertEquals("108640", rs.next().get("value").getValue());
//                assertFalse(rs.hasNext());
            } finally {
                rs.close();
            }

            // Dimensions namespace prefixes
            CloseableIterator<STMT> dimensions = conn.findStatements(null, META.instances, null, null, false);
            try {
                while (dimensions.hasNext()) {
                    STMT stmt = dimensions.next();
                    UID dimensionType = (UID) stmt.getSubject();
                    UID instancesContext = (UID) stmt.getObject();
                    namespaces.put(instancesContext.getId() + "#", dimensionType.getLocalName().toLowerCase());
                }
            } finally {
                dimensions.close();
            }
        } finally {
            conn.close();
        }



        OutputStream out = new BufferedOutputStream(new FileOutputStream("target/example.ttl"));
        try {
            repository.export(Format.TURTLE,  namespaces, null, out);
            out.flush();
        } finally {
            out.close();
        }


    }
}
