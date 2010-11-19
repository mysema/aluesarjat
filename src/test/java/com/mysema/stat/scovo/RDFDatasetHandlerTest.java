package com.mysema.stat.scovo;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.mysema.rdfbean.guice.RDFBeanModule;
import com.mysema.rdfbean.model.RDF;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.object.DefaultConfiguration;
import com.mysema.stat.META;
import com.mysema.stat.pcaxis.PCAxisParser;

import fi.aluesarjat.prototype.guice.BigDataRDFBeanModule;

@Ignore
public class RDFDatasetHandlerTest {

    private Repository repository;

    @Before
    public void setUp(){
        RDFBeanModule module = new BigDataRDFBeanModule();
        repository = module.createRepository(new DefaultConfiguration());
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

        RDFConnection connection = repository.openConnection();
        try{
            assertTrue(connection.exists(null, SCV.dataset, null, null, false));
            assertTrue(connection.exists(null, SCV.dimension, null, null, false));
            assertTrue(connection.exists(null, RDF.type, SCV.Dataset, null, false));
            assertTrue(connection.exists(null, META.nsPrefix, null, null, false));
        }finally{
            connection.close();
        }
    }

}
