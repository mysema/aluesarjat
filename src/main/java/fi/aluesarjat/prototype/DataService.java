package fi.aluesarjat.prototype;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mysema.rdfbean.model.RDF;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.UID;
import com.mysema.stat.pcaxis.PCAxisParser;
import com.mysema.stat.scovo.RDFDatasetHandler;
import com.mysema.stat.scovo.SCV;

public class DataService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataService.class);
    
    @Inject @Named("baseURI")
    private String baseURI;
    
    @Inject @Named("forceReload")
    private String forceReload;
    
    @Inject
    private Repository repository;
    
    @SuppressWarnings("unchecked")
    @PostConstruct
    public void initialize(){
        try {
            logger.info("initializing data");
            boolean reload = "true".equals(this.forceReload);
            
            RDFDatasetHandler handler = new RDFDatasetHandler(repository, baseURI);
            PCAxisParser parser = new PCAxisParser(handler);
            
            List<String> datasets = IOUtils.readLines(getStream("/data/datasets"));
            for (String d : datasets) {
                String datasetName = d.toString().trim();
                if (StringUtils.isNotBlank(datasetName)) {
                    UID uid = RDFDatasetHandler.datasetUID(baseURI, datasetName);
                    RDFConnection conn = repository.openConnection();
                    boolean load;
                    try {
                        load = reload || !conn.exists(uid, RDF.type, SCV.Dataset, uid, false);
                    } finally {
                        conn.close();
                    }
                    if (load) {
                        logger.info("Loading " + datasetName + "...");
                        long time = System.currentTimeMillis();
                        InputStream in = getStream("/data/" + datasetName + ".px");
                        try {
                            parser.parse(datasetName, in);
                        } finally {
                            in.close();
                        }
                        logger.info("Done loading " + datasetName + " in " + (System.currentTimeMillis() - time) + " ms");
                    } else {
                        logger.info("Skipping existing " + datasetName);
                    }
                }
            }
            logger.info("initialized data");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } 
    }
    
    private InputStream getStream(String name) {
        return getClass().getResourceAsStream(name);
    }
    
}
