package fi.aluesarjat.prototype.guice;

import java.io.File;

import com.mysema.rdfbean.guice.RDFBeanModule;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.object.Configuration;
import com.mysema.rdfbean.sesame.NativeRepository;
import com.mysema.stat.pcaxis.Dataset;
import com.mysema.stat.pcaxis.PCAxis;
import com.mysema.stat.scovo.PXConverter;

public class CustomRDFBeanModule extends RDFBeanModule{

    @Override
    public Repository createRepository(Configuration configuration) {
        File dataDir = new File(System.getProperty("java.io.tmpdir"), "aluesarjat-data");
        dataDir.mkdir();
        Repository repository = new NativeRepository(dataDir, false);
        repository.initialize();
        
        // TODO : make URL configurable
        PXConverter pxc = new PXConverter(repository, "http://localhost:8080/rdf/");
        pxc.convert(new Dataset("example-1", PCAxis.parse(getClass().getResourceAsStream("/example-1.px"))));
        pxc.convert(new Dataset("example-2", PCAxis.parse(getClass().getResourceAsStream("/example-2.px"))));
        
        return repository;
    }    
    
}
