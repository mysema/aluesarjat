package fi.aluesarjat.prototype;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
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
import com.mysema.rdfbean.model.LIT;
import com.mysema.rdfbean.model.RDF;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.model.UID;
import com.mysema.stat.pcaxis.PCAxisParser;
import com.mysema.stat.scovo.DC;
import com.mysema.stat.scovo.META;
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

    private void addNamespace(String ns, String prefix) {
        RDFConnection conn = repository.openConnection();
        try {
            conn.update(null, Collections.singleton(new STMT(new UID(ns), META.nsPrefix, new LIT(prefix), null)));
        } finally {
            conn.close();
        }
    }

    @SuppressWarnings("unchecked")
    @PostConstruct
    public void initialize(){
        try {
            logger.info("adding namespaces");

            for (Map.Entry<String,String> entry : Namespaces.DEFAULT.entrySet()) {
                addNamespace(entry.getKey(), entry.getValue());
            }
            addNamespace(SCV.NS, "scv");
            addNamespace(META.NS, "meta");
            addNamespace(DC.NS, "dc");
            addNamespace(baseURI + "domain#", "domain");
            addNamespace(baseURI + "datasets#", "dataset");

            logger.info("initializing data");
            boolean reload = "true".equals(this.forceReload);

            RDFDatasetHandler handler = new RDFDatasetHandler(repository, baseURI);
            PCAxisParser parser = new PCAxisParser(handler);

            List<String> datasets = IOUtils.readLines(getStream("/data/datasets"));
            for (String d : datasets) {
                String datasetDef = d.trim();
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
                    boolean load;
                    RDFConnection conn = repository.openConnection();
                    try {
                        load = reload || !conn.exists(uid, RDF.type, SCV.Dataset, uid, false);
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
