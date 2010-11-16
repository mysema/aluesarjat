package fi.aluesarjat.prototype.guice;

import java.io.File;
import java.util.Collections;
import java.util.List;

import com.mysema.rdfbean.guice.RDFBeanModule;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.object.Configuration;
import com.mysema.rdfbean.sesame.NativeRepository;

public class CustomRDFBeanModule extends RDFBeanModule{

    @Override
    public List<String> getConfiguration(){
        return Collections.singletonList("/aluesarjat.properties");
    }

    @Override
    public Repository createRepository(Configuration configuration) {
        File dataDir = new File(System.getProperty("java.io.tmpdir"), "aluesarjat-data");
        dataDir.mkdir();
        NativeRepository repository = new NativeRepository(dataDir, false);
        repository.setIndexes("spoc,posc,cspo,opsc");
        repository.initialize();
        return repository;
    }

}
