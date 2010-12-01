package com.mysema.stat.scovo;

import java.io.File;
import java.io.IOException;

import org.junit.Ignore;

import com.mysema.rdfbean.ontology.EmptyOntology;
import com.mysema.rdfbean.sesame.SesameRepository;

import fi.aluesarjat.prototype.guice.VirtuosoSesameRepository;

@Ignore
public class VirtuosoDatasetHandlerTest extends AbstractDatasetHandlerTest {

    @Override
    protected SesameRepository createRepository() {
        File dataDir = new File("target/aluesarjat-virtuoso");
        dataDir.mkdir();
        VirtuosoSesameRepository repository = new VirtuosoSesameRepository(dataDir, "localhost:1111", "dba", "dba");
        repository.setOntology(EmptyOntology.DEFAULT);
        return repository;
    }

    public static void main(String[] args) throws IOException{
        VirtuosoDatasetHandlerTest test = new VirtuosoDatasetHandlerTest();
        test.setUp();
        try{
            test.Parse_and_Query();
        }finally{
            test.tearDown();
        }
    }

}
