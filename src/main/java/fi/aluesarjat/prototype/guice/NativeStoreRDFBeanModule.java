package fi.aluesarjat.prototype.guice;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.mysema.rdfbean.guice.Config;
import com.mysema.rdfbean.guice.RDFBeanRepositoryModule;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.sesame.NativeRepository;

public class NativeStoreRDFBeanModule extends RDFBeanRepositoryModule {

    @Override
    public List<String> getConfiguration() {
        return Arrays.asList("/aluesarjat.properties", "/import-parallel.properties");
    }

    @Override
    public Repository createRepository(@Config Properties properties) {
        File dataDir = new File(System.getProperty("java.io.tmpdir"), "aluesarjat-data");
        dataDir.mkdir();

        final NativeRepository repository = new NativeRepository(dataDir, false);
        repository.setSources(ModuleUtils.getSources(properties.getProperty("baseURI")));
        repository.setIndexes("spoc,posc,cspo,opsc");
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
