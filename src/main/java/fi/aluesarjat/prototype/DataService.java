package fi.aluesarjat.prototype;

import java.io.IOException;
import java.io.InputStream;
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
import com.mysema.rdfbean.model.RDF;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.UID;
import com.mysema.stat.META;
import com.mysema.stat.STAT;
import com.mysema.stat.pcaxis.PCAxisParser;
import com.mysema.stat.scovo.DC;
import com.mysema.stat.scovo.DCTERMS;
import com.mysema.stat.scovo.NamespaceHandler;
import com.mysema.stat.scovo.RDFDatasetHandler;
import com.mysema.stat.scovo.SCV;

public class DataService {

    public enum Mode { PARALLEL, THREADED, NONTHREADED }

    private static final Logger logger = LoggerFactory.getLogger(DataService.class);

    private final String baseURI;

    private final boolean forceReload;

    private final Repository repository;

    private final NamespaceHandler namespaceHandler;

    private final Mode mode;

    private List<String> datasets;

    @Inject
    public DataService(
            Repository repository,
            NamespaceHandler namespaceHandler,
            @Named("baseURI") String baseURI,
            @Named("import.mode") String mode,
            @Named("forceReload") String forceReload){
        this.repository = repository;
        this.namespaceHandler = namespaceHandler;
        this.baseURI = baseURI;
        this.forceReload = Boolean.valueOf(forceReload);
        this.mode = Mode.valueOf(mode);
    }

    @PostConstruct
    public void initialize() throws IOException{
        logger.info("adding namespaces");

        Map<String,String> namespaces = new HashMap<String,String>(Namespaces.DEFAULT);
        namespaces.put(SCV.NS, "scv");
        namespaces.put(META.NS, "meta");
        namespaces.put(DC.NS, "dc");
        namespaces.put(DCTERMS.NS, "dcterms");
        namespaces.put(STAT.NS, "stat");
        namespaces.put(baseURI + RDFDatasetHandler.DIMENSION_NS, "dimension");
        namespaces.put(baseURI + RDFDatasetHandler.DATASET_CONTEXT_BASE, "dataset");
        namespaceHandler.addNamespaces(namespaces);

        logger.info("initializing data");

        if (datasets == null){
            datasets = IOUtils.readLines(getStream("/data/datasets"));
        }

        if (mode == Mode.PARALLEL) {
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

        } else if (mode == Mode.THREADED) {
            Thread thread = new Thread(){
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

    private void importData(String datasetDef, boolean reload) {
        try {
            RDFDatasetHandler handler = new RDFDatasetHandler(repository, namespaceHandler, baseURI);
            PCAxisParser parser = new PCAxisParser(handler);

            if (StringUtils.isNotBlank(datasetDef)) {
                String[] values = datasetDef.split("\\s+");
                String datasetName = values[0];
                String[] ignoredValues;
                if (values.length > 1) {
                    ignoredValues = new String[values.length - 1];
                    System.arraycopy(values, 1, ignoredValues, 0, ignoredValues.length);
                } else {
                    ignoredValues = new String[0];
                }

                UID uid = RDFDatasetHandler.datasetUID(baseURI, datasetName);
                UID datasetsContext = RDFDatasetHandler.datasetsContext(baseURI);
                boolean load;
                RDFConnection conn = repository.openConnection();
                try {
                    // TODO: reload -> first delete existing triples
                    load = !conn.exists(uid, RDF.type, SCV.Dataset, datasetsContext, false);
                } finally {
                    conn.close();
                }
                if (load) {
                    handler.setIgnoredValues(ignoredValues);
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
