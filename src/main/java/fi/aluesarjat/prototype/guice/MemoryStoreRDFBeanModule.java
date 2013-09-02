package fi.aluesarjat.prototype.guice;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.mysema.rdfbean.guice.Config;
import com.mysema.rdfbean.guice.RDFBeanRepositoryModule;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.sesame.MemoryRepository;

public class MemoryStoreRDFBeanModule extends RDFBeanRepositoryModule {

    @Override
    public List<String> getConfiguration() {
        return Arrays.asList("/aluesarjat.properties", "/import-parallel.properties");
    }

    @Override
    public Repository createRepository(@Config Properties properties) {
        final MemoryRepository repository = new MemoryRepository();
        repository.setSources(ModuleUtils.getSources(properties.getProperty("baseURI")));
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
