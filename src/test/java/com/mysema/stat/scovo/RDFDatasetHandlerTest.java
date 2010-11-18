package com.mysema.stat.scovo;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.sesame.MemoryRepository;
import com.mysema.stat.pcaxis.PCAxisParser;

public class RDFDatasetHandlerTest {

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
    public void Parse() throws IOException {
        RDFDatasetHandler handler = new RDFDatasetHandler(repository, "http://www.aluesarjat.fi/rdf/");
        PCAxisParser parser = new PCAxisParser(handler);
        parser.parse("A01HKIS_Vaestotulot", getClass().getResourceAsStream("/data/A01HKIS_Vaestotulot.px"));
    }

}
