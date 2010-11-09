package fi.aluesarjat.prototype.guice;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.mysema.rdfbean.guice.RDFBeanModule;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.object.Configuration;
import com.mysema.rdfbean.sesame.NativeRepository;
import com.mysema.stat.pcaxis.Dataset;
import com.mysema.stat.pcaxis.PCAxis;
import com.mysema.stat.scovo.PXConverter;

public class CustomRDFBeanModule extends RDFBeanModule{

    private Properties properties = new Properties();
    
    public CustomRDFBeanModule() throws IOException {
        properties.load(getClass().getResourceAsStream("/aluesarjat.properties"));
    }
    
    @Override
    public Repository createRepository(Configuration configuration) {
        File dataDir = new File(System.getProperty("java.io.tmpdir"), "aluesarjat-data");
        dataDir.mkdir();
        Repository repository = new NativeRepository(dataDir, false);
        repository.initialize();
        
        PXConverter pxc = new PXConverter(repository, properties.getProperty("baseURI"));
        pxc.convert(new Dataset("example-1", PCAxis.parse(getClass().getResourceAsStream("/example-1.px"))));
        pxc.convert(new Dataset("example-2", PCAxis.parse(getClass().getResourceAsStream("/example-2.px"))));
        
        return repository;
    }    
    
}
