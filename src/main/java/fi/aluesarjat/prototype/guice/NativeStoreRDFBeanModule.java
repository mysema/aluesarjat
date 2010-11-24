package fi.aluesarjat.prototype.guice;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

import com.mysema.rdfbean.guice.Config;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.RepositoryException;
import com.mysema.rdfbean.model.io.Format;
import com.mysema.rdfbean.model.io.RDFSource;
import com.mysema.rdfbean.sesame.NativeRepository;
import com.mysema.stat.scovo.SCV;

public class NativeStoreRDFBeanModule extends RDFBeanRepositoryModule{

    @Override
    public List<String> getConfiguration(){
        return Collections.singletonList("/aluesarjat.properties");
    }

    @Override
    public Repository createRepository(@Config Properties properties) {
        File dataDir = new File(System.getProperty("java.io.tmpdir"), "aluesarjat-data");
        dataDir.mkdir();

        NativeRepository repository = new NativeRepository(dataDir, false);
        repository.setSources(new RDFSource[]{
            getAreaDescriptions(properties),
            new RDFSource("classpath:/scovo.rdf", Format.RDFXML, SCV.NS),
            new RDFSource("classpath:/stat.rdf", Format.RDFXML, "http://data.mysema.com/rdf/pcaxis#")});
        repository.setIndexes("spoc,posc,cspo,opsc");
        repository.initialize();
        return repository;
    }

    private RDFSource getAreaDescriptions(Properties properties){
        try {
            String str = IOUtils.toString(RDFBeanRepositoryModule.class.getResourceAsStream("/alue.ttl"), "ISO-8859-1");
            String normalized = str.replace("http://localhost:8080/rdf/", properties.getProperty("baseURI"));
            return new RDFSource(new ByteArrayInputStream(normalized.getBytes("UTF-8")), Format.TURTLE, normalized + "dimensions/Alue");
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

}
