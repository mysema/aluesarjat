package com.mysema.stat.scovo;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Test;

import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.io.Format;
import com.mysema.rdfbean.sesame.MemoryRepository;
import com.mysema.stat.pcaxis.Dataset;
import com.mysema.stat.pcaxis.PCAxis;

public class PXConversionTest {

    @Test
    public void convert() throws IOException {
        Repository repository = new MemoryRepository();
        repository.initialize();
        
        PXConverter pxc = new PXConverter(repository, "http://www.aluesarjat.fi/rdf/");
        
        pxc.convert(new Dataset("example-1", PCAxis.parse("src/test/resources/example-1.px")));
        pxc.convert(new Dataset("example-2", PCAxis.parse("src/test/resources/example-2.px")));
        pxc.convert(new Dataset("example-3", PCAxis.parse("src/test/resources/example-3.px")));
        
        OutputStream out = new BufferedOutputStream(new FileOutputStream("target/example.ttl"));
        repository.export(Format.TURTLE, out);
        out.flush();
        out.close();
        repository.close();
        System.out.println("DONE");
    }
    
}
