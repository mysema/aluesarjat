package fi.aluesarjat.prototype.guice;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

import com.bigdata.rdf.axioms.NoAxioms;
import com.bigdata.rdf.sail.BigdataSail;
import com.mysema.rdfbean.guice.Config;
import com.mysema.rdfbean.guice.RDFBeanModule;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.RepositoryException;
import com.mysema.rdfbean.model.io.Format;
import com.mysema.rdfbean.model.io.RDFSource;
import com.mysema.rdfbean.object.Configuration;
import com.mysema.rdfbean.sesame.SesameRepository;
import com.mysema.stat.scovo.SCV;

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
            SesameRepository repository = new BigDataSesameRepository(dataDir, properties);
            repository.setSources(new RDFSource[]{
                getAreaDescriptions(props),
                new RDFSource("classpath:/scovo.rdf", Format.RDFXML, SCV.NS),
                new RDFSource("classpath:/stat.rdf", Format.RDFXML, "http://data.mysema.com/rdf/pcaxis#")});
            repository.initialize();
            return repository;
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    private RDFSource getAreaDescriptions(Properties properties){
        try {
            String str = IOUtils.toString(getClass().getResourceAsStream("/alue.ttl"), "ISO-8859-1");
            String normalized = str.replace("http://localhost:8080/rdf/", properties.getProperty("baseURI"));
            return new RDFSource(new ByteArrayInputStream(normalized.getBytes("ISO-8859-1")), Format.TURTLE, normalized + "dimensions/Alue");
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

}
