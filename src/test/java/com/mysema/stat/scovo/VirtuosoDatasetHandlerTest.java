package com.mysema.stat.scovo;

import java.io.IOException;

import org.junit.Ignore;

import com.mysema.rdfbean.sesame.SesameRepository;

@Ignore
public class VirtuosoDatasetHandlerTest extends AbstractDatasetHandlerTest {

    @Override
    protected SesameRepository createRepository() {
//        File dataDir = new File("target/aluesarjat-virtuoso");
//        dataDir.mkdir();
//        VirtuosoSesameRepository repository = new VirtuosoSesameRepository("localhost:1111", "dba", "dba");
//        repository.setOntology(EmptyOntology.DEFAULT);
//        return repository;
        return null;
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
