package com.mysema.stat.scovo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysema.commons.lang.Assert;
import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.model.*;
import com.mysema.rdfbean.owl.OWL;
import com.mysema.rdfbean.xsd.DateTimeConverter;
import com.mysema.stat.META;
import com.mysema.stat.STAT;
import com.mysema.stat.pcaxis.Dataset;
import com.mysema.stat.pcaxis.DatasetHandler;
import com.mysema.stat.pcaxis.Dimension;
import com.mysema.stat.pcaxis.DimensionType;
import com.mysema.stat.pcaxis.Item;

public class RDFDatasetHandler implements DatasetHandler {

    private static final Logger logger = LoggerFactory.getLogger(RDFDatasetHandler.class);

    // E.g. http://www.aluesarjat.fi/rdf/
    private final String baseURI;

    public static final String DIMENSIONS = "dimensions";

    public static final String DIMENSION_NS = DIMENSIONS + "/"; // Alue, Toimiala, Vuosi, ...

    private static final String DATASETS = "datasets";

    public static final String DATASET_CONTEXT_BASE = DATASETS + "#"; // A01S_HKI_Vakiluku, ...

    private static final DateTimeConverter DATE_TIME_CONVERTER = new DateTimeConverter();

//    private UID datasetContext;
//
//    private String datasetNs;

    private Set<STMT> statements;

    private RDFConnection conn;

    private final Repository repository;

    private Map<Dimension, UID> dimensions;

    private static final Map<String, LIT> DECIMAL_CACHE = new HashMap<String, LIT>();

    private List<UID> datasets;

    private final int batchSize = 2000;

    private int itemCount = 0;

    private int skippedCount = 0;

    private Set<String> ignoredValues = new HashSet<String>(Arrays.asList(
            "\".\""
    ));

    static {
        for (int i=0; i <= 1000; i++) {
            String str = Integer.toString(i);
            DECIMAL_CACHE.put(str, new LIT(str, XSD.decimalType));
        }
    }

    public RDFDatasetHandler(Repository repository, String baseURI) {
        this.repository = repository;
        this.baseURI = baseURI;
        Assert.notNull(baseURI, "baseURI");
        Assert.assertThat(baseURI.endsWith("/"), "baseURI doesn't end with /", null, null);
    }

    private String print(UID t) {
        String uri = t.getId();
        return uri.startsWith(baseURI) ? uri.substring(baseURI.length()) : uri;
    }

    private boolean exists(UID id, UID context) {
        return conn.exists(id, null, null, context, false);
    }

    private void add(ID subject, UID predicate, DateTime dateTime, UID context) {
        add(subject, predicate, new LIT(DATE_TIME_CONVERTER.toString(dateTime), XSD.dateTime), context);
    }

    private void addDecimal(ID subject, UID predicate, String decimal, UID context) {
        LIT lit = DECIMAL_CACHE.get(decimal);
        if (lit == null) {
            lit = new LIT(decimal, XSD.decimalType);
        }
        add(subject, predicate, lit, context);
    }

    private void add(ID subject, UID predicate, String name, UID context) {
        add(subject, predicate, new LIT(name), context);
    }

    public static UID datasetUID(String baseURI, String datasetName) {
        return new UID(baseURI + DATASET_CONTEXT_BASE, RDFDatasetHandler.encodeID(datasetName));
    }

    public static String encodeID(String name) {
        return XMLID.toXMLID(name);
    }

    private void add(ID subject, UID predicate, NODE object, UID context) {
        statements.add( new STMT(subject, predicate, object, context) );
    }

    public static UID datasetsContext(String baseURI) {
        return new UID(baseURI, DATASETS);
    }

    @Override
    public void addDataset(Dataset dataset) {
        UID datasetsContext = datasetsContext(baseURI);
        UID datasetUID = datasetUID(baseURI, dataset.getName());

        datasets.add(datasetUID);
        add(datasetUID, RDF.type, SCV.Dataset, datasetsContext);
        if (dataset.getTitle() != null) {
            add(datasetUID, DC.title, dataset.getTitle(), datasetsContext);
        }
        if (dataset.getDescription() != null) {
            add(datasetUID, DC.description, dataset.getDescription(), datasetsContext);
        }

        add(datasetUID, DCTERMS.created, new DateTime(), datasetsContext);

        UID domainContext = new UID(baseURI,  DIMENSIONS);
        String dimensionBase = baseURI + DIMENSION_NS;

        // SCHEMA: DimensionTypes
        for (DimensionType type : dataset.getDimensionTypes()) {
            UID t = new UID(dimensionBase, encodeID(type.getName()));
            UID dimensionContext = new UID(dimensionBase, encodeID(type.getName()));
            String dimensionNs = dimensionContext.getId() + "#";

            if (!exists(t, domainContext)) {
                add(t, RDF.type, RDFS.Class, domainContext);
                add(t, RDF.type, OWL.Class, domainContext);
                add(t, RDFS.subClassOf, SCV.Dimension, domainContext);
                add(t, DC.title, type.getName(), domainContext);

                // Namespace for dimension instances
                addNamespace(repository, dimensionNs, dimensionContext.getLocalName().toLowerCase());
            } else {
                logger.info("Referring to existing DimensionType: " + print(t));
            }

            // INSTANCES: Dimensions
            for (Dimension dimension : type.getDimensions()) {
                UID d = new UID(dimensionNs, encodeID(dimension.getName()));
                dimensions.put(dimension, d);

                if (!exists(d, dimensionContext)) {
                    add(d, RDF.type, t, dimensionContext);
                    add(d, DC.title, dimension.getName(), dimensionContext);
                } else {
                    logger.info("Referring to existing Dimension: " + print(d) + " of type " + print(t));
                }

                add(datasetUID, STAT.datasetDimension, d, datasetsContext);

                // TODO: hierarchy?
                // TODO: subProperty of scv:dimension?
            }
        }
        flush();
    }

    private void flush() {
        conn.update(Collections.<STMT>emptySet(), statements);
        statements.clear();
    }

    @Override
    public void addItem(Item item) {
        if (ignoredValues.contains(item.getValue())) {
            if (++skippedCount % 1000 == 0) {
                logger.info(item.getDataset().getName() + ": skipped " + skippedCount + " items");
            }
        } else {
            Dataset dataset = item.getDataset();
            UID datasetContext = datasetUID(baseURI, dataset.getName());

            BID id = conn.createBNode();

            add(id, RDF.type, SCV.Item, datasetContext);

            String value = item.getValue();
            if (value.startsWith("\"")) {
                add(id, RDF.value, value.substring(1, value.length() - 1), datasetContext);
            } else {
                addDecimal(id, RDF.value, value, datasetContext);
            }
            add(id, SCV.dataset, datasetContext, datasetContext);

            for (Dimension dimension : item.getDimensions()) {
                // TODO: subProperty of scv:dimension?
                add(id, SCV.dimension, dimensions.get(dimension), datasetContext);
            }

            if (++itemCount % batchSize == 0){
                flush();
            }

            if (itemCount % 1000 == 0) {
                logger.info(dataset.getName() + ": loaded " + itemCount + " items");
            }
        }
    }

    public void setIgnoredValues(String... values) {
        this.ignoredValues = new HashSet<String>(Arrays.asList(values));
    }

    @Override
    public void begin() {
        conn = repository.openConnection();
        statements = new LinkedHashSet<STMT>();
        dimensions = new HashMap<Dimension, UID>();
        datasets = new ArrayList<UID>();
    }

    @Override
    public void rollback() {
        if (conn != null){
            conn.close();
        }
    }

    @Override
    public void commit() {
        if (conn != null){
            DateTime now = new DateTime();
            UID datasetsContext = datasetsContext(baseURI);
            for (UID dataset : datasets) {
                add(dataset, DCTERMS.modified, now, datasetsContext);
            }
            flush();
            conn.close();
        }
    }

    public static void addNamespace(Repository repository, String ns, String prefix) {
        RDFConnection conn = repository.openConnection();
        CloseableIterator<STMT> iter = null;
        try {
            LIT prefixLiteral = new LIT(prefix);
            UID uid = new UID(ns);
            STMT nsStmt = new STMT(uid, META.nsPrefix, prefixLiteral, null);
            boolean found = false;

            // Prefix mapped already
            iter = conn.findStatements(null, META.nsPrefix, prefixLiteral, null, false);
            while(iter.hasNext()) {
                STMT stmt = iter.next();
                if (stmt.equals(nsStmt)) {
                    // Retain valid mapping
                    found = true;
                } else {
                    // Remove duplicate prefix-mapping
                    conn.update(Collections.singleton(stmt), null);
                }
            }
            iter.close();

            // URI mapped already
            iter = conn.findStatements(uid, META.nsPrefix, null, null, false);
            while(iter.hasNext()) {
                STMT stmt = iter.next();
                if (stmt.equals(nsStmt)) {
                    // Retain valid mapping
                    found = true;
                } else {
                    // Remove duplicate URI-mapping
                    conn.update(Collections.singleton(stmt), null);
                }
            }
            iter.close();

            if (!found) {
                // Add new mapping
                conn.update(null, Collections.singleton(nsStmt));
            }
        } finally {
            if (iter != null) {
                iter.close();
            }
            conn.close();
        }
    }

}
