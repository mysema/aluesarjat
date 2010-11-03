package com.mysema.stat.scovo;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.model.*;
import com.mysema.rdfbean.owl.OWL;
import com.mysema.stat.pcaxis.Dataset;
import com.mysema.stat.pcaxis.Dimension;
import com.mysema.stat.pcaxis.DimensionType;
import com.mysema.stat.pcaxis.Item;

public class PXConverter {
    
    private static final Logger logger = LoggerFactory.getLogger(PXConverter.class);
    
    private String baseURI;
    
    private Repository repository;
    
    private UID context;
    
    private String ns;
    
    private Set<STMT> statements;
    
    public PXConverter(Repository repository, String baseURI) {
        this.repository = repository;
        this.baseURI = baseURI;
    }
    
    public void convert(Dataset dataset) {
        context = new UID(baseURI, encodeID(dataset.getName()));
        ns = context.getId() + "#";
        statements = new LinkedHashSet<STMT>();
        RDFConnection conn = repository.openConnection();

        Map<Dimension, UID> dimensions = new HashMap<Dimension, UID>();
        
        /*
         * METADATA
         */
        // Dataset
        add(context, RDF.type, SCV.Dataset);
        add(context, DC.title, dataset.getName());
        
        // DimensionTypes
        for (DimensionType type : dataset.getDimensionTypes()) {
            // TODO: Use / update common metadata? 
            UID t = new UID(ns, encodeID(type.getName()));

            if (!exists(t, conn)) {
                add(t, RDF.type, RDFS.Class);
                add(t, RDF.type, OWL.Class);
                add(t, RDFS.subClassOf, SCV.Dimension);
                add(t, DC.title, type.getName());
            } else {
                logger.info("Referring to existing DimensionType: " + print(t));
            }
            
            
            // Dimensions
            for (Dimension dimension : type.getDimensions()) {
                // TODO: Use / update common metadata? 
                
                // XXX: ensure, there's no clash here!
                UID d = new UID(ns, encodeID(dimension.getName()));
                dimensions.put(dimension, d);
                
                if (!exists(d, conn)) {
                    add(d, RDF.type, t);
                    add(d, DC.title, dimension.getName());
                } else {
                    logger.info("Referring to existing Dimension: " + print(d) + " of type " + print(t));
                }
                
                // TODO: hierarchy?
                // TODO: subProperty of scv:dimension?
            }
        }
        
        /*
         * DATA
         */
        for (Item item : dataset.getItems()) {
            BID id = conn.createBNode();
            
            add(id, RDF.type, SCV.Item);
            add(id, RDF.value, item.getValue());
            add(id, SCV.dataset, context);
            
            for (Dimension dimension : item.getDimensions()) {
                // TODO: subProperty of scv:dimension?
                add(id, SCV.dimension, dimensions.get(dimension));
            }
        }
        
        conn.update(Collections.<STMT>emptySet(), statements);
        try {
            conn.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String print(UID t) {
        String uri = t.getId();
        return uri.startsWith(baseURI) ? uri.substring(baseURI.length()) : uri;
    }

    private boolean exists(UID id, RDFConnection conn) {
        CloseableIterator<STMT> iter = conn.findStatements(id, null, null, context, false);
        boolean exists = iter.hasNext();
        iter.close();
        return exists;
    }
    
    private void add(ID subject, UID predicate, BigDecimal decimal) {
        add(subject, predicate, new LIT(decimal.toPlainString(), XSD.decimalType));
    }
    
    private void add(ID subject, UID predicate, String name) {
        add(subject, predicate, new LIT(name));
    }

    private String encodeID(String name) {
        return XMLID.toXMLID(name);
    }
    
    private void add(ID subject, UID predicate, NODE object) {
        statements.add( new STMT(subject, predicate, object, context) );
    }
    
}
