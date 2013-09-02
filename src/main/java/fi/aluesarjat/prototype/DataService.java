package fi.aluesarjat.prototype;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mysema.rdfbean.Namespaces;
import com.mysema.rdfbean.model.DC;
import com.mysema.rdfbean.model.DCTERMS;
import com.mysema.rdfbean.model.GEO;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.SKOS;
import com.mysema.rdfbean.model.UID;
import com.mysema.stat.META;
import com.mysema.stat.STAT;
import com.mysema.stat.pcaxis.PCAxisParser;
import com.mysema.stat.scovo.NamespaceHandler;
import com.mysema.stat.scovo.SCV;
import com.mysema.stat.scovo.ScovoDatasetHandler;
import com.mysema.stat.scovo.ScovoExtDatasetHandler;

public class DataService {

    public static final String DIMENSION_INFERENCE = "define input:inference \"dimensions\"\n";
    
    private static final Logger logger = LoggerFactory.getLogger(DataService.class);

    private final String baseURI;

    private final boolean forceReload;

    private final Repository repository;

    private final NamespaceHandler namespaceHandler;

    private final DataServiceMode defaultMode;

    private final String datasetsList;
    
    private List<String> datasets;
    
    @Inject
    public DataService(
            Repository repository,
            NamespaceHandler namespaceHandler,
            @Named("baseURI") String baseURI,
            @Named("import.mode") DataServiceMode mode,
            @Named("forceReload") boolean forceReload,
            @Named("datasets.list") String datasetsList) {
        this.repository = repository;
        this.namespaceHandler = namespaceHandler;
        this.baseURI = baseURI;
        this.forceReload = forceReload;
        this.defaultMode = mode;
        this.datasetsList = datasetsList;
    }

    
    @PostConstruct
    public void initialize() throws IOException {
        loadData(defaultMode);
    }
    
    @SuppressWarnings("unchecked")
    public void loadData(DataServiceMode mode) throws IOException {
        logger.info("adding namespaces");
        
        String dimensionNs = baseURI + ScovoDatasetHandler.DIMENSION_NS;

        Map<String,String> namespaces = new HashMap<String,String>(Namespaces.DEFAULT);
        namespaces.put(GEO.NS, "geo");
        namespaces.put(SCV.NS, "scv");
        namespaces.put(META.NS, "meta");
        namespaces.put(DC.NS, "dc");
        namespaces.put(DCTERMS.NS, "dcterms");
        namespaces.put(STAT.NS, "stat");
        namespaces.put(SKOS.NS, "skos");
        namespaces.put(dimensionNs, "dimension");
        namespaces.put(baseURI + ScovoDatasetHandler.DATASET_CONTEXT_BASE, "dataset");
        namespaceHandler.addNamespaces(namespaces);

        logger.info("initializing data");

        if (datasets == null) {
//            datasets = IOUtils.readLines(getStream("/data/datasets"));
            if (datasetsList.startsWith("classpath:")) {
                datasets = IOUtils.readLines(getStream(datasetsList.substring("classpath:".length())));
            } else {
                InputStream in = new URL(datasetsList).openStream();
                datasets = IOUtils.readLines(in);
                in.close();
            }
        }

        if (mode == DataServiceMode.PARALLEL) {
            for (String d : datasets) {
                final String datasetDef = d.trim();
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        importData(datasetDef, forceReload);
                    }
                };
                thread.setDaemon(true);
                thread.start();
            }

        } else if (mode == DataServiceMode.THREADED) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    for (String d : datasets) {
                        importData(d.trim(), forceReload);
                    }
                }
            };
            thread.setDaemon(true);
            thread.start();

        } else {
            for (String d : datasets) {
                importData(d.trim(), forceReload);
            }
        }
    }

    public void importData(String datasetDef, boolean reload) {
        try {
            ScovoExtDatasetHandler handler = new ScovoExtDatasetHandler(repository, namespaceHandler, baseURI);
            PCAxisParser parser = new PCAxisParser(handler);

            if (StringUtils.isNotBlank(datasetDef)) {
                String[] values = datasetDef.split("\\s+");
                String[] protAndPath = values[0].split(":");
                String protocol = protAndPath[0];
                String path = protAndPath[1];
                String datasetName = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
                String[] ignoredValues;
                if (values.length > 1) {
                    ignoredValues = new String[values.length - 1];
                    System.arraycopy(values, 1, ignoredValues, 0, ignoredValues.length);
                } else {
                    ignoredValues = new String[0];
                }

                UID uid = ScovoExtDatasetHandler.datasetUID(baseURI, datasetName);
                UID datasetsContext = ScovoExtDatasetHandler.datasetsContext(baseURI);
                boolean load;
                RDFConnection conn = repository.openConnection();
                try {
                    // TODO: reload -> first delete existing triples
                    load = !conn.exists(uid, DCTERMS.modified, null, datasetsContext, false);
                } finally {
                    conn.close();
                }
                if (load) {
                    handler.setIgnoredValues(ignoredValues);
                    logger.info("Loading " + datasetName + "...");
                    long time = System.currentTimeMillis();
                    InputStream in;
                    if ("classpath".equals(protocol)) {
                        in = getStream(path);
                    } else {
                        URLConnection urlConnection = new URL(values[0]).openConnection();
                        urlConnection.setConnectTimeout(3000);
                        urlConnection.setReadTimeout(3000);
                        in = urlConnection.getInputStream();
                    }
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
        } catch(FileNotFoundException e) {    
            logger.error(e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream getStream(String name) {
        return DataService.class.getResourceAsStream(name);
    }

    public void setDatasets(List<String> datasets) {
        this.datasets = datasets;
    }

}
