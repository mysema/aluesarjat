package com.mysema.stat.scovo;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysema.commons.lang.Assert;
import com.mysema.rdfbean.model.BID;
import com.mysema.rdfbean.model.ID;
import com.mysema.rdfbean.model.LIT;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.RDF;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.RDFS;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.model.UID;
import com.mysema.rdfbean.model.XSD;
import com.mysema.rdfbean.owl.OWL;
import com.mysema.stat.pcaxis.Dataset;
import com.mysema.stat.pcaxis.DatasetHandler;
import com.mysema.stat.pcaxis.Dimension;
import com.mysema.stat.pcaxis.DimensionType;
import com.mysema.stat.pcaxis.Item;

public class RDFDatasetHandler implements DatasetHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(RDFDatasetHandler.class);
    
    // http://www.aluesarjat.fi/rdf/
    private String baseURI;

    public static final String DATASET_CONTEXT = "datasets/"; // A01S_HKI_Vakiluku, ...

    public static final String DOMAIN = "domain";

    public static final String DIMENSION_CONTEXT = "dimensions/"; // Alue, Toimiala, Vuosi, ...
    
//    private UID datasetContext;
//    
//    private String datasetNs;
    
    private Set<STMT> statements;
    
    private RDFConnection conn;
    
    private Repository repository;

    private Map<Dimension, UID> dimensions;

    private static final Map<String, LIT> DECIMAL_CACHE = new HashMap<String, LIT>();

    private int itemCount = 0; 
    
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

    private boolean exists(UID id, UID context, RDFConnection conn) {
        return conn.exists(id, null, null, context, false);
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
        return new UID(baseURI + DATASET_CONTEXT, RDFDatasetHandler.encodeID(datasetName));
    }
    
    public static String encodeID(String name) {
        return XMLID.toXMLID(name);
    }
    
    private void add(ID subject, UID predicate, NODE object, UID context) {
        statements.add( new STMT(subject, predicate, object, context) );
    }

    @Override
    public void addDataset(Dataset dataset) {

        UID datasetContext = datasetUID(baseURI, dataset.getName());
        add(datasetContext, RDF.type, SCV.Dataset, datasetContext);
        if (dataset.getTitle() != null) {
            add(datasetContext, DC.title, dataset.getTitle(), datasetContext);
        }
        if (dataset.getDescription() != null) {
            add(datasetContext, DC.description, dataset.getDescription(), datasetContext);
        }
        
        UID domainContext = new UID(baseURI + DOMAIN);
        String domainNs = domainContext.getId() + "#";
        
        String dimensionBase = baseURI + DIMENSION_CONTEXT;
        
        // SCHEMA: DimensionTypes
        for (DimensionType type : dataset.getDimensionTypes()) {
            UID t = new UID(domainNs, encodeID(type.getName()));
            UID dimensionContext = new UID(dimensionBase + encodeID(type.getName()));
            String dimensionNs = dimensionContext.getId() + "#";

            if (!exists(t, domainContext, conn)) {
                add(t, RDF.type, RDFS.Class, domainContext);
                add(t, RDF.type, OWL.Class, domainContext);
                add(t, RDFS.subClassOf, SCV.Dimension, domainContext);
                add(t, DC.title, type.getName(), domainContext);
                add(t, META.instances, dimensionContext, domainContext);
            } else {
                logger.info("Referring to existing DimensionType: " + print(t));
            }
            
            // INSTANCES: Dimensions
            for (Dimension dimension : type.getDimensions()) {
                UID d = new UID(dimensionNs, encodeID(dimension.getName()));
                dimensions.put(dimension, d);
                
                if (!exists(d, dimensionContext, conn)) {
                    add(d, RDF.type, t, dimensionContext);
                    add(d, DC.title, dimension.getName(), dimensionContext);
                } else {
                    logger.info("Referring to existing Dimension: " + print(d) + " of type " + print(t));
                }
                
                // TODO: hierarchy?
                // TODO: subProperty of scv:dimension?
            }
        }
        conn.update(Collections.<STMT>emptySet(), statements);
        statements.clear();
    }

    @Override
    public void addItem(Item item) {
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
        conn.update(Collections.<STMT>emptySet(), statements);
        
        itemCount++;
        
        if (itemCount % 1000 == 0) {
            logger.info("Loaded " + itemCount + " items");
        }
        
        statements.clear();
    }

    @Override
    public void begin() {
        conn = repository.openConnection();
        statements = new LinkedHashSet<STMT>();
        dimensions = new HashMap<Dimension, UID>();
    }

    @Override
    public void rollback() {
        conn.close();
    }

    @Override
    public void commit() {
        conn.close();
    }
    
}
