package fi.aluesarjat.prototype.guice;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.bigdata.rdf.axioms.NoAxioms;
import com.bigdata.rdf.sail.BigdataSail;
import com.mysema.rdfbean.guice.Config;
import com.mysema.rdfbean.guice.RDFBeanModule;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.RepositoryException;
import com.mysema.rdfbean.object.Configuration;

public class BigDataRDFBeanModule extends RDFBeanModule{

    @Override
    public List<String> getConfiguration(){
        return Collections.singletonList("/aluesarjat.properties");
    }

    @Override
    public Repository createRepository(Configuration configuration, @Config Properties props) {
        try {
            File dataDir = new File(System.getProperty("java.io.tmpdir"), "aluesarjat-data");
            dataDir.mkdir();
            File journal = new File(dataDir, "bigdata.jnl");
            journal.createNewFile();
            // TODO : use external properties ?!?
            Properties properties = new Properties();
            properties.setProperty(BigdataSail.Options.FILE, journal.getAbsolutePath());
            properties.setProperty(BigdataSail.Options.TRUTH_MAINTENANCE, "false");
            properties.setProperty(BigdataSail.Options.STATEMENT_IDENTIFIERS, "false");
            properties.setProperty(BigdataSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
            properties.setProperty(BigdataSail.Options.QUADS, "true");
            return new BigDataSesameRepository(dataDir, properties);
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

}
