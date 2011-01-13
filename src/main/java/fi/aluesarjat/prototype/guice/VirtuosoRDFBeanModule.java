package fi.aluesarjat.prototype.guice;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.mysema.rdfbean.guice.Config;
import com.mysema.rdfbean.guice.RDFBeanRepositoryModule;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.virtuoso.VirtuosoRepository;

public class VirtuosoRDFBeanModule extends RDFBeanRepositoryModule{

    @Override
    public List<String> getConfiguration(){
        return Arrays.asList("/aluesarjat.properties", "/import-serial.properties");
    }

    @Override
    public Repository createRepository(@Config Properties properties){
        File dataDir = new File(System.getProperty("java.io.tmpdir"), "aluesarjat-virtuoso");
        dataDir.mkdir();
        String baseURI = properties.getProperty("baseURI");        
        String hostAndPort = properties.getProperty("virtuoso.host") +":"+properties.getProperty("virtuoso.port");
        String user = properties.getProperty("virtuoso.user");
        String pass = properties.getProperty("virtuoso.pass");
        final VirtuosoRepository repository = new VirtuosoRepository(hostAndPort, user, pass, baseURI);
        repository.setSources(ModuleUtils.getSources(baseURI));
        repository.initialize();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                repository.close();
            }
        });
        return repository;
    }

}
