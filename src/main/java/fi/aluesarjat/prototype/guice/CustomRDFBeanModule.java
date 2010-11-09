package fi.aluesarjat.prototype.guice;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.mysema.rdfbean.guice.RDFBeanModule;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.object.Configuration;
import com.mysema.rdfbean.sesame.NativeRepository;

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
        return repository;
    }
    
    @Provides
    @Singleton
    public Properties createProperties(){
        return properties;
    }
}
