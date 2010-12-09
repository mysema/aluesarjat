package com.mysema.stat.scovo;

import java.io.UnsupportedEncodingException;
import java.math.MathContext;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysema.commons.lang.Assert;
import com.mysema.rdfbean.model.ID;
import com.mysema.rdfbean.model.LIT;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.RDF;
import com.mysema.rdfbean.model.RDFBeanTransaction;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.RDFS;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.model.UID;
import com.mysema.rdfbean.model.XSD;
import com.mysema.rdfbean.owl.OWL;
import com.mysema.rdfbean.xsd.DateTimeConverter;
import com.mysema.stat.STAT;
import com.mysema.stat.pcaxis.Dataset;
import com.mysema.stat.pcaxis.DatasetHandler;
import com.mysema.stat.pcaxis.Dimension;
import com.mysema.stat.pcaxis.DimensionType;
import com.mysema.stat.pcaxis.Item;

public class RDFDatasetHandler implements DatasetHandler {

    private static final String UNITS_LOCAL_NAME = "Yksikk\u00F6";

    private static final Logger logger = LoggerFactory.getLogger(RDFDatasetHandler.class);

    private static final int TX_TIMEOUT = -1;

    private static final int TX_ISOLATION = Connection.TRANSACTION_READ_COMMITTED;

    private static final Pattern AREA_NAME_PATTERN = Pattern.compile("[\\d\\s]+(.*)");

    
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

    private final NamespaceHandler namespaceHandler;

    private Map<Dimension, UID> dimensions;

    private static final Map<String, LIT> DECIMAL_CACHE = new HashMap<String, LIT>();

    private List<UID> datasets;

    private final int batchSize = 2000;

    private int itemCount = 0;

    private int skippedCount = 0;

    private Set<String> ignoredValues = Collections.singleton("\".\"");

    static {
        for (int i=0; i <= 1000; i++) {
            String str = Integer.toString(i);
            DECIMAL_CACHE.put(str, new LIT(str, XSD.decimalType));
        }
    }

    public RDFDatasetHandler(Repository repository, NamespaceHandler namespaceHandler, String baseURI) {
        this.repository = repository;
        this.namespaceHandler = namespaceHandler;
        this.baseURI = baseURI;
        Assert.notNull(baseURI, "baseURI");
        Assert.assertThat(baseURI.endsWith("/"), "baseURI doesn't end with /", null, null);
    }

    private void add(ID subject, UID predicate, DateTime dateTime, UID context) {
        add(subject, predicate, new LIT(DATE_TIME_CONVERTER.toString(dateTime), XSD.dateTime), context);
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
        UID unitDimension = new UID(dimensionBase, encodeID(UNITS_LOCAL_NAME));
        boolean unitFound = false;

        Map<String,String> namespaces = new HashMap<String,String>();
        // SCHEMA: DimensionTypes
        for (DimensionType type : dataset.getDimensionTypes()) {
            UID dimensionUID = new UID(dimensionBase, encodeID(type.getName()));
            unitFound = unitFound || dimensionUID.equals(unitDimension);
            
            addDimensionType(type, datasetsContext, datasetUID, domainContext,
                    dimensionUID, namespaces);
        }

        // Units
        if (!unitFound) {
            // Create dynamic dimension type and value from Dataset's UNITS property
            if (dataset.getUnits() != null) {
                String units = dataset.getUnits();
                DimensionType type = new DimensionType(UNITS_LOCAL_NAME);
                type.addDimension(units.substring(0, 1).toUpperCase() + units.substring(1)); // henkilö -> Henkilö
                
                dataset.addDimensionType(type);
                
                addDimensionType(type, datasetsContext, datasetUID, domainContext, unitDimension, namespaces);
            } else {
                logger.warn("Dataset " + dataset.getName() + " has no unit!");
            }
        }

        flush();

        namespaceHandler.addNamespaces(namespaces);
    }

    private void addDimensionType(DimensionType type, UID datasetsContext,
            UID datasetUID, UID domainContext, UID dimensionUID,
            Map<String, String> namespaces) {
        String dimensionNs = dimensionUID.getId() + "#";

        add(dimensionUID, RDF.type, RDFS.Class, domainContext);
        add(dimensionUID, RDF.type, OWL.Class, domainContext);
        add(dimensionUID, RDFS.subClassOf, SCV.Dimension, domainContext);
        add(dimensionUID, DC.title, type.getName(), domainContext);

        // Namespace for dimension instances
        namespaces.put(dimensionNs, dimensionUID.getLocalName().toLowerCase(Locale.ENGLISH));

        // INSTANCES: Dimensions
        for (Dimension dimension : type.getDimensions()) {
            UID d = new UID(dimensionNs, encodeID(dimension.getName()));
            dimensions.put(dimension, d);

            add(d, RDF.type, dimensionUID, dimensionUID);
            add(d, DC.identifier, dimension.getName(), dimensionUID);
            
            if ("Alue".equals(type.getName())) {
                add(d, DC.title, getAreaName(dimension.getName()), dimensionUID);
            } else {
                add(d, DC.title, dimension.getName(), dimensionUID);
            }

            add(datasetUID, STAT.datasetDimension, d, datasetsContext);

            // TODO: hierarchy?
            // TODO: subProperty of scv:dimension?
        }
    }

    private String getAreaName(String name) {
        Matcher m = AREA_NAME_PATTERN.matcher(name);
        if (m.find()) {
            return m.group(1);
        } else {
            return name;
        }
    }

    private void flush() {
        RDFBeanTransaction tx = conn.beginTransaction(false, TX_TIMEOUT, TX_ISOLATION);
        try {
            conn.update(Collections.<STMT>emptySet(), statements);
            statements.clear();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addItem(Item item) {
        if (ignoredValues.contains(item.getValue())) {
            if (++skippedCount % 1000 == 0) {
                logger.info(item.getDataset().getName() + ": skipped " + skippedCount + " items");
            }
        } else {
            try {
                Dataset dataset = item.getDataset();
                UID datasetContext = datasetUID(baseURI, dataset.getName());

                MessageDigest md = MessageDigest.getInstance("SHA-1");
                List<NODE[]> properties = new ArrayList<NODE[]>();

                // PROPERTIES from which an ID for the Item is derived
                addProperty(RDF.type, SCV.Item, properties, md);

                String value = item.getValue();
                if (value.startsWith("\"")) {
                    addProperty(RDF.value, value.substring(1, value.length() - 1), properties, md);
                } else {
                    addProperty(RDF.value, value, properties, md);
                }
                addProperty(SCV.dataset, datasetContext, properties, md);

                for (Dimension dimension : item.getDimensions()) {
                    addProperty(SCV.dimension, dimensions.get(dimension), properties, md);
                }
                // ADD TRIPLES
                UID id = new UID("item:", new String(Hex.encodeHex(md.digest())));
                for (NODE[] property : properties) {
                    add(id, (UID) property[0], property[1], datasetContext);
                }

                if (++itemCount % batchSize == 0) {
                    flush();
                    logger.info(dataset.getName() + ": loaded " + itemCount + " items");
                }
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }

        }
    }

    private void addProperty(UID predicate, String object, List<NODE[]> properties, MessageDigest md) throws UnsupportedEncodingException {
        addProperty(predicate, new LIT(object), properties, md);
    }

    private void addProperty(UID predicate, NODE object, List<NODE[]> properties, MessageDigest md) throws UnsupportedEncodingException {
        properties.add(new NODE[] { predicate, object } );
        md.update(predicate.getId().getBytes("UTF-8"));
        md.update(object.toString().getBytes("UTF-8"));
    }

    public void setIgnoredValues(String... values) {
        this.ignoredValues = new HashSet<String>(Arrays.asList(values));
    }

    @Override
    public void begin() {
        conn = repository.openConnection();
        statements = new LinkedHashSet<STMT>(batchSize * 10);
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



}
