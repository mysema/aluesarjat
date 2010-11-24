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
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.RepositoryException;
import com.mysema.rdfbean.model.io.Format;
import com.mysema.rdfbean.model.io.RDFSource;
import com.mysema.rdfbean.sesame.SesameRepository;
import com.mysema.stat.scovo.SCV;

public class BigDataRDFBeanModule extends RDFBeanRepositoryModule{

    @Override
    public List<String> getConfiguration(){
        return Collections.singletonList("/aluesarjat.properties");
    }

    @Override
    public Repository createRepository(@Config Properties props) {
        try {
            File dataDir = new File(System.getProperty("java.io.tmpdir"), "aluesarjat-bigdata");
            dataDir.mkdir();
            File journalFile = new File(dataDir, "bigdata.jnl");
            journalFile.createNewFile();
            // TODO : use external properties ?!?
            Properties properties = new Properties();
            properties.setProperty(BigdataSail.Options.FILE, journalFile.getAbsolutePath());
            properties.setProperty(BigdataSail.Options.TRUTH_MAINTENANCE, "false");
            properties.setProperty(BigdataSail.Options.STATEMENT_IDENTIFIERS, "false");
            properties.setProperty(BigdataSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
            properties.setProperty(BigdataSail.Options.QUADS, "true");
            properties.setProperty(BigdataSail.Options.TEXT_INDEX, "false");
//            properties.setProperty(BigdataSail.Options.BUFFER_MODE, BufferMode.DiskRW.name());

//            properties.setProperty(Options.FILE, journalFile.getAbsolutePath());
//            properties.setProperty(Options.TRUTH_MAINTENANCE, "false");
//            properties.setProperty(Options.STATEMENT_IDENTIFIERS, "false");
//            properties.setProperty(Options.AXIOMS_CLASS, NoAxioms.class.getName());
//            properties.setProperty(Options.QUADS, "true");
//            properties.setProperty(Options.TEXT_INDEX, "false");
//            properties.setProperty(Options.BUFFER_MODE, BufferMode.DiskRW.name());

//            Journal journal = new Journal(properties);
//            if (journal.getIndex("testIndex") == null){
//                IndexMetadata indexMetadata = new IndexMetadata( "testIndex", UUID.randomUUID());
//                indexMetadata.setIsolatable(true);
//                journal.registerIndex(indexMetadata);
//                journal.commit();
//            }
//            LocalTripleStore store = new LocalTripleStore(journal, "kb", ITx.UNISOLATED, properties);
//            if (journalFile.length() == 0){
//                store.create();
//            }
//            SesameRepository repository = new BigDataSesameRepository(dataDir, store);

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
            return new RDFSource(new ByteArrayInputStream(normalized.getBytes("UTF-8")), Format.TURTLE, normalized + "dimensions/Alue");
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

}
