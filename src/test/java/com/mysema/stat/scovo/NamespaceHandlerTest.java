package com.mysema.stat.scovo;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mysema.rdfbean.model.io.Format;
import com.mysema.rdfbean.model.io.RDFSource;
import com.mysema.rdfbean.sesame.MemoryRepository;
import com.mysema.rdfbean.sesame.SesameRepository;
import com.mysema.stat.META;
import com.mysema.stat.STAT;


public class NamespaceHandlerTest {

    private SesameRepository repository;

    private final String baseURI = "http://localhost:8080/rdf/";

    @Before
    public void setUp(){
        repository = new MemoryRepository();
        repository.setSources(new RDFSource[]{
                new RDFSource("classpath:/alue.ttl", Format.TURTLE, "http://localhost:8080/rdf/dimensions/Alue"),
                new RDFSource("classpath:/scovo.rdf", Format.RDFXML, SCV.NS),
                new RDFSource("classpath:/stat.rdf", Format.RDFXML, "http://data.mysema.com/rdf/pcaxis#")});
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

}
