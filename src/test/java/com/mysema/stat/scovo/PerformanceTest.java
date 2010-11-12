package com.mysema.stat.scovo;

import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.sesame.MemoryRepository;
import com.mysema.stat.pcaxis.DatasetHandler;
import com.mysema.stat.pcaxis.PCAxisParser;

@Ignore
public class PerformanceTest {

    private final Repository repository = new MemoryRepository();

    private final String[] names = new String[]{
            "A01HKIS_Vaestotulot",
            "B02S_ESP_Vakiluku1975",
            "C02S_VAN_Vakiluku1971"};

    @Before
    public void setUp(){
        repository.initialize();
    }

//    null :     531   519   569  563
//    rdfbean : 5939  5498  5321 5487
//    openrdf : 3911  4327  3992 3821

    @Test
    public void Null() throws IOException{
        DatasetHandler handler = new NullHandler();
        handle("null", handler);
    }

    @Test
    public void RDFBean() throws IOException{
        DatasetHandler handler = new RDFDatasetHandler(repository, "http://www.aluesarjat.fi/rdf/");
        handle("rdfbean", handler);
    }

    @Test
    public void OpenRDF() throws IOException{
        DatasetHandler handler = new OpenRDFDatasetHandler(repository, "http://www.aluesarjat.fi/rdf/");
        handle("openrdf", handler);
    }

    private void handle(String label, DatasetHandler handler) throws IOException{
        PCAxisParser parser = new PCAxisParser(handler);
        long start = System.currentTimeMillis();
        for (String name : names){
            parser.parse(name, getClass().getResourceAsStream("/data/"+name+".px"));
        }
        System.err.println(label + " : " + (System.currentTimeMillis()-start));
    }

}
