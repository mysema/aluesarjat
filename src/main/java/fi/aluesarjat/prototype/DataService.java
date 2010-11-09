package fi.aluesarjat.prototype;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.mysema.rdfbean.model.RDF;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.UID;
import com.mysema.stat.pcaxis.Dataset;
import com.mysema.stat.pcaxis.PCAxis;
import com.mysema.stat.scovo.PXConverter;
import com.mysema.stat.scovo.SCV;

public class DataService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataService.class);
    
    private static final String BASE_URI_PROP = "baseURI";
    
    private static final String FORCE_RELOAD = "forceReload";
    
    @Inject
    private Properties properties;
    
    @Inject
    private Repository repository;
    
    @PostConstruct
    public void initialize(){
        try {
            logger.info("initializing data");
            String baseURI = getBaseURI();
            boolean forceReload = isForceReload();
            
            PXConverter pxc = new PXConverter(baseURI);
            
            @SuppressWarnings("rawtypes")
            List datasets = IOUtils.readLines(getStream("/data/datasets"));
            for (Object d : datasets) {
                String datasetName = d.toString().trim();
                if (StringUtils.isNotBlank(datasetName)) {
                    UID uid = PXConverter.datasetUID(baseURI, datasetName);
                    RDFConnection conn = repository.openConnection();
                    try {
                        if (forceReload || !conn.exists(uid, RDF.type, SCV.Dataset, uid, false)) {
                            InputStream in = getStream("/data/" + datasetName + ".px");
                            try {
                                pxc.convert(new Dataset(datasetName, PCAxis.parse(in)), conn);
                            } finally {
                                in.close();
                            }
                        }
                    } finally {
                        conn.close();
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
    
    private String getBaseURI() {
        return properties.getProperty(BASE_URI_PROP);
    }
    
    private boolean isForceReload() {
        return "true".equalsIgnoreCase(properties.getProperty(FORCE_RELOAD)); 
    }
}
