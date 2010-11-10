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
import com.mysema.stat.pcaxis.Dataset;
import com.mysema.stat.pcaxis.PCAxis;
import com.mysema.stat.scovo.PXConverter;
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
            
            PXConverter pxc = new PXConverter(baseURI);
            
            List<String> datasets = IOUtils.readLines(getStream("/data/datasets"));
            for (String d : datasets) {
                String datasetName = d.toString().trim();
                if (StringUtils.isNotBlank(datasetName)) {
                    UID uid = PXConverter.datasetUID(baseURI, datasetName);
                    RDFConnection conn = repository.openConnection();
                    try {
                        if (reload || !conn.exists(uid, RDF.type, SCV.Dataset, uid, false)) {
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
    
}
