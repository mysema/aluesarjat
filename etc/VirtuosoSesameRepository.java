package fi.aluesarjat.prototype.guice;

import java.io.File;

import org.openrdf.repository.Repository;

import virtuoso.sesame3.driver.VirtuosoRepository;

import com.mysema.rdfbean.model.FileIdSequence;
import com.mysema.rdfbean.model.IdSequence;
import com.mysema.rdfbean.sesame.SesameRepository;

public class VirtuosoSesameRepository extends SesameRepository{

    private IdSequence idSource;

    private final File dataDir;

    private final String hostlist, user, pass;

    public VirtuosoSesameRepository(File dataDir, String hostlist, String user, String pass) {
        this.dataDir = dataDir;
        this.hostlist = hostlist;
        this.user = user;
        this.pass = pass;
    }

    @Override
    protected Repository createRepository(boolean sesameInference) {
        Repository repository = new VirtuosoRepository(hostlist, user, pass);
        idSource = new FileIdSequence(new File(dataDir, "lastLocalId"));
        return repository;
    }

    @Override
    public long getNextLocalId() {
        return idSource.getNextId();
    }

}
