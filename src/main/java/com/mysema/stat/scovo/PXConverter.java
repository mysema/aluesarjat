package com.mysema.stat.scovo;

import java.math.BigDecimal;
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
import com.mysema.stat.pcaxis.Dimension;
import com.mysema.stat.pcaxis.DimensionType;
import com.mysema.stat.pcaxis.Item;

public class PXConverter {
    
    private static final Logger logger = LoggerFactory.getLogger(PXConverter.class);
    
    // http://www.aluesarjat.fi/rdf/
    private String baseURI;

    public static final String DATASET_CONTEXT = "datasets/"; // A01S_HKI_Vakiluku, ...

    public static final String DOMAIN = "domain";

    public static final String DIMENSION_CONTEXT = "dimensions/"; // Alue, Toimiala, Vuosi, ...
    
//    private UID datasetContext;
//    
//    private String datasetNs;
    
    private Set<STMT> statements;
    
    public PXConverter(String baseURI) {
        this.baseURI = baseURI;
        Assert.notNull(baseURI, "baseURI");
        Assert.assertThat(baseURI.endsWith("/"), "baseURI doesn't end with /", null, null);
    }
    
    public void convert(Dataset dataset, Repository repository) {
        RDFConnection conn = repository.openConnection();
        try {
            convert(dataset, conn);
        } finally {
            conn.close();
        }
    }
    
    public void convert(Dataset dataset, RDFConnection conn) {
        statements = new LinkedHashSet<STMT>();
        Map<Dimension, UID> dimensions = new HashMap<Dimension, UID>();

        /*
         * METADATA
         */
        UID domainContext = new UID(baseURI + DOMAIN);
        String domainNs = domainContext.getId() + "#";
        
        String dimensionBase = baseURI + DIMENSION_CONTEXT;
        
        // SCHEMA: DimensionTypes
        for (DimensionType type : dataset.getDimensionTypes()) {
            UID t = new UID(domainNs, encodeID(type.getName()));

            if (!exists(t, domainContext, conn)) {
                add(t, RDF.type, RDFS.Class, domainContext);
                add(t, RDF.type, OWL.Class, domainContext);
                add(t, RDFS.subClassOf, SCV.Dimension, domainContext);
                add(t, DC.title, type.getName(), domainContext);
            } else {
                logger.info("Referring to existing DimensionType: " + print(t));
            }
            
            // INSTANCES: Dimensions
            UID dimensionContext = new UID(dimensionBase + encodeID(type.getName()));
            String dimensionNs = dimensionContext.getId() + "#";

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
        
        /*
         * DATASET
         */
        UID datasetContext = datasetUID(baseURI, dataset.getName());
        add(datasetContext, RDF.type, SCV.Dataset, datasetContext);
        if (dataset.getTitle() != null) {
            add(datasetContext, DC.title, dataset.getTitle(), datasetContext);
        }
        if (dataset.getDescription() != null) {
            add(datasetContext, DC.description, dataset.getDescription(), datasetContext);
        }
        
        for (Item item : dataset.getItems()) {
            BID id = conn.createBNode();
            
            add(id, RDF.type, SCV.Item, datasetContext);

            if (item.getValue() instanceof BigDecimal) {
                add(id, RDF.value, (BigDecimal) item.getValue(), datasetContext);
            } else {
                add(id, RDF.value, (String) item.getValue(), datasetContext);
            }
            add(id, SCV.dataset, datasetContext, datasetContext);
            
            for (Dimension dimension : item.getDimensions()) {
                // TODO: subProperty of scv:dimension?
                add(id, SCV.dimension, dimensions.get(dimension), datasetContext);
            }
        }
        
        conn.update(Collections.<STMT>emptySet(), statements);
    }

    private String print(UID t) {
        String uri = t.getId();
        return uri.startsWith(baseURI) ? uri.substring(baseURI.length()) : uri;
    }

    private boolean exists(UID id, UID context, RDFConnection conn) {
        return conn.exists(id, null, null, context, false);
    }
    
    private void add(ID subject, UID predicate, BigDecimal decimal, UID context) {
        add(subject, predicate, new LIT(decimal.toPlainString(), XSD.decimalType), context);
    }
    
    private void add(ID subject, UID predicate, String name, UID context) {
        add(subject, predicate, new LIT(name), context);
    }

    public static UID datasetUID(String baseURI, String datasetName) {
        return new UID(baseURI + DATASET_CONTEXT, PXConverter.encodeID(datasetName));
    }
    
    public static String encodeID(String name) {
        return XMLID.toXMLID(name);
    }
    
    private void add(ID subject, UID predicate, NODE object, UID context) {
        statements.add( new STMT(subject, predicate, object, context) );
    }
    
}
