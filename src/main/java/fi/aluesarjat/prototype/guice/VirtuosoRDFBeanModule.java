package fi.aluesarjat.prototype.guice;

import java.io.File;
import java.util.Collections;
import java.util.List;

import com.mysema.rdfbean.guice.RDFBeanModule;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.object.Configuration;
import com.mysema.rdfbean.ontology.EmptyOntology;

public class VirtuosoRDFBeanModule extends RDFBeanModule{

    @Override
    public List<String> getConfiguration(){
        return Collections.singletonList("/aluesarjat.properties");
    }

    @Override
    public Repository createRepository(Configuration configuration){
        File dataDir = new File(System.getProperty("java.io.tmpdir"), "aluesarjat-data");
        dataDir.mkdir();
        VirtuosoSesameRepository repository = new VirtuosoSesameRepository(dataDir, "localhost:1111", "dba", "dba");
        repository.setOntology(EmptyOntology.DEFAULT);
        repository.initialize();
        return repository;
    }

}
