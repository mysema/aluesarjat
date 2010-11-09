package fi.aluesarjat.prototype.guice;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysema.rdfbean.guice.RDFBeanModule;
import com.mysema.rdfbean.model.RDF;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.UID;
import com.mysema.rdfbean.object.Configuration;
import com.mysema.rdfbean.sesame.NativeRepository;
import com.mysema.stat.pcaxis.Dataset;
import com.mysema.stat.pcaxis.PCAxis;
import com.mysema.stat.scovo.PXConverter;
import com.mysema.stat.scovo.SCV;

public class CustomRDFBeanModule extends RDFBeanModule{
    
    private static final Logger logger = LoggerFactory.getLogger(CustomRDFBeanModule.class);

    private Properties properties = new Properties();
    
    private static final String BASE_URI_PROP = "baseURI";
    
    private static final String FORCE_RELOAD = "forceReload";
    
    public CustomRDFBeanModule() throws IOException {
        properties.load(getStream("/aluesarjat.properties"));
    }
    
    @Override
    public Repository createRepository(Configuration configuration) {
        File dataDir = new File(System.getProperty("java.io.tmpdir"), "aluesarjat-data");
        dataDir.mkdir();
        Repository repository = new NativeRepository(dataDir, false);
        repository.initialize();
        
        try {
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
                            long time = System.currentTimeMillis();
                            logger.info("Importing " + datasetName);
                            InputStream in = getStream("/data/" + datasetName + ".px");
                            try {
                                pxc.convert(new Dataset(datasetName, PCAxis.parse(in)), conn);
                            } finally {
                                in.close();
                            }
                            logger.info("Imported " + datasetName + " in " + (System.currentTimeMillis() - time) + " ms");
                        } else {
                            logger.info("Skipped " + datasetName);
                        }
                    } finally {
                        conn.close();
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } 
        return repository;
    }
    
    private InputStream getStream(String name) {
        return getClass().getResourceAsStream(name);
    }
    
    public String getBaseURI() {
        return properties.getProperty(BASE_URI_PROP);
    }
    
    public boolean isForceReload() {
        return "true".equalsIgnoreCase(properties.getProperty(FORCE_RELOAD)); 
    }
}
