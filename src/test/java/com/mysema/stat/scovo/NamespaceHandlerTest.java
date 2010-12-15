package com.mysema.stat.scovo;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mysema.commons.lang.IteratorAdapter;
import com.mysema.rdfbean.model.DC;
import com.mysema.rdfbean.model.DCTERMS;
import com.mysema.rdfbean.model.LIT;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.UID;
import com.mysema.rdfbean.sesame.MemoryRepository;
import com.mysema.rdfbean.sesame.SesameRepository;
import com.mysema.stat.META;
import com.mysema.stat.STAT;

import fi.aluesarjat.prototype.guice.ModuleUtils;


public class NamespaceHandlerTest {

    private SesameRepository repository;

    private final String baseURI = "http://localhost:8080/rdf/";

    @Before
    public void setUp(){
        repository = new MemoryRepository();
        repository.setSources(ModuleUtils.getSources(baseURI));
        repository.initialize();
    }

    @After
    public void tearDown(){
        repository.close();
    }

    @Test
    public void Add_Namespace(){
        NamespaceHandler namespaceHandler = new NamespaceHandler(repository);
        Map<String,String> namespaces = new HashMap<String,String>();
        namespaces.put(SCV.NS, "scv");
        namespaces.put(META.NS, "meta");
        namespaces.put(DC.NS, "dc");
        namespaces.put(DCTERMS.NS, "dcterms");
        namespaces.put(STAT.NS, "stat");
        namespaces.put(baseURI + RDFDatasetHandler.DIMENSION_NS, "dimension");
        namespaces.put(baseURI + RDFDatasetHandler.DATASET_CONTEXT_BASE, "dataset");
        namespaceHandler.addNamespaces(namespaces);
    }

    @Test
    public void Overwrite_Namespace(){
        NamespaceHandler namespaceHandler = new NamespaceHandler(repository);
        Map<String,String> namespaces = new HashMap<String,String>();
        namespaces.put(SCV.NS, "s");
        namespaceHandler.addNamespaces(namespaces);

        RDFConnection conn = repository.openConnection();
        try{
            assertEquals(1, IteratorAdapter.asList(conn.findStatements(null,            META.nsPrefix, null, null, false)).size());
            assertEquals(1, IteratorAdapter.asList(conn.findStatements(null,            META.nsPrefix, new LIT("s"), null, false)).size());
            assertEquals(1, IteratorAdapter.asList(conn.findStatements(new UID(SCV.NS), META.nsPrefix, null, null, false)).size());
        }finally{
            conn.close();
        }

        // overwrite
        namespaces.put(SCV.NS, "scv");
        namespaceHandler.addNamespaces(namespaces);

        conn = repository.openConnection();
        try{
            assertEquals(1, IteratorAdapter.asList(conn.findStatements(null,            META.nsPrefix, null, null, false)).size());
            assertEquals(0, IteratorAdapter.asList(conn.findStatements(null,            META.nsPrefix, new LIT("s"), null, false)).size());
            assertEquals(1, IteratorAdapter.asList(conn.findStatements(null,            META.nsPrefix, new LIT("scv"), null, false)).size());
            assertEquals(1, IteratorAdapter.asList(conn.findStatements(new UID(SCV.NS), META.nsPrefix, null, null, false)).size());
        }finally{
            conn.close();
        }

    }


}
