package fi.aluesarjat.prototype.guice;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.mysema.rdfbean.guice.Config;
import com.mysema.rdfbean.guice.RDFBeanRepositoryModule;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.ontology.EmptyOntology;

public class VirtuosoRDFBeanModule extends RDFBeanRepositoryModule{

    @Override
    public List<String> getConfiguration(){
        return Arrays.asList("/aluesarjat.properties", "/import-serial.properties");
    }

    @Override
    public Repository createRepository(@Config Properties properties){
        File dataDir = new File(System.getProperty("java.io.tmpdir"), "aluesarjat-virtuoso");
        dataDir.mkdir();
        VirtuosoSesameRepository repository = new VirtuosoSesameRepository(dataDir, "localhost:1111", "dba", "dba");
        repository.setOntology(EmptyOntology.DEFAULT);
        repository.initialize();
        return repository;
    }

}
