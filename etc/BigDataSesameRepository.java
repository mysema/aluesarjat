package fi.aluesarjat.prototype.guice;

import java.io.File;
import java.util.Properties;

import org.openrdf.repository.Repository;

import com.bigdata.rdf.sail.BigdataSail;
import com.bigdata.rdf.sail.BigdataSailRepository;
import com.bigdata.rdf.store.AbstractTripleStore;
import com.mysema.rdfbean.model.FileIdSequence;
import com.mysema.rdfbean.model.IdSequence;
import com.mysema.rdfbean.sesame.SesameRepository;

public class BigDataSesameRepository extends SesameRepository{

    private final File dataDir;

    private final Properties properties;

    private final AbstractTripleStore tripleStore;

    private IdSequence idSource;

    public BigDataSesameRepository(File dataDir, Properties properties) {
        this.dataDir = dataDir;
        this.properties = properties;
        this.tripleStore = null;
    }

    public BigDataSesameRepository(File dataDir, AbstractTripleStore tripleStore) {
        this.dataDir = dataDir;
        this.properties = null;
        this.tripleStore = tripleStore;
    }

    @Override
    protected Repository createRepository(boolean sesameInference) {
        BigdataSail sail;
        if (properties != null){
            sail = new BigdataSail(properties);
        } else {
            sail = new BigdataSail(tripleStore);
        }
        Repository repository = new BigdataSailRepository(sail);
        idSource = new FileIdSequence(new File(dataDir, "lastLocalId"));
        return repository;
    }

    @Override
    public long getNextLocalId() {
        return idSource.getNextId();
    }

}
