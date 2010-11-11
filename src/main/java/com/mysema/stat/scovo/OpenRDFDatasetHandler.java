package com.mysema.stat.scovo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.store.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysema.commons.lang.Assert;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.sesame.SesameConnection;
import com.mysema.stat.pcaxis.Dataset;
import com.mysema.stat.pcaxis.DatasetHandler;
import com.mysema.stat.pcaxis.Dimension;
import com.mysema.stat.pcaxis.DimensionType;
import com.mysema.stat.pcaxis.Item;

public class OpenRDFDatasetHandler implements DatasetHandler {

    private static final Logger logger = LoggerFactory.getLogger(OpenRDFDatasetHandler.class);

    // http://www.aluesarjat.fi/rdf/
    private final String baseURI;

    public static final String DATASET_CONTEXT = "datasets/"; // A01S_HKI_Vakiluku, ...

    public static final String DOMAIN = "domain";

    public static final String DIMENSION_CONTEXT = "dimensions/"; // Alue, Toimiala, Vuosi, ...

    //    private UID datasetContext;
    //
    //    private String datasetNs;

    private Set<Statement> statements;

    private RepositoryConnection conn;

    private ValueFactory vf;

    private final Repository repository;

    private Map<Dimension, URI> dimensions;

    //    private static final Map<String, LIT> DECIMAL_CACHE = new HashMap<String, LIT>();

    private int itemCount = 0;

    private int skippedCount = 0;

    private Set<String> ignoredValues = new HashSet<String>(Arrays.asList(
            "\".\""
    ));

    private URI scv_dataset, scv_dimension, scv_Dataset, scv_Dimension, scv_Item;

    private URI dc_title, dc_description;

    private URI meta_instances;


    public OpenRDFDatasetHandler(Repository repository, String baseURI) {
        this.repository = repository;
        this.baseURI = baseURI;
        Assert.notNull(baseURI, "baseURI");
        Assert.assertThat(baseURI.endsWith("/"), "baseURI doesn't end with /", null, null);
    }

    private String print(URI t) {
        String uri = t.stringValue();
        return uri.startsWith(baseURI) ? uri.substring(baseURI.length()) : uri;
    }

    private boolean exists(URI id, URI context) {
//        return conn.exists(id, null, null, context, false);
        try {
            return conn.hasMatch(id, null, null, false, context);
        } catch (StoreException e) {
            throw new RuntimeException(e);
        }
    }

    private void addDecimal(Resource subject, URI predicate, String decimal, URI context) {
        add(subject, predicate, vf.createLiteral(decimal, XMLSchema.DECIMAL), context);
    }

    private void add(Resource subject, URI predicate, String name, URI context) {
        add(subject, predicate, vf.createLiteral(name), context);
    }

    private URI datasetUID(String baseURI, String datasetName) {
        return vf.createURI(baseURI + DATASET_CONTEXT, OpenRDFDatasetHandler.encodeID(datasetName));
    }

    public static String encodeID(String name) {
        return XMLID.toXMLID(name);
    }

    private void add(Resource subject, URI predicate, Value object, URI context) {
        Statement stmt = vf.createStatement(subject, predicate, object, context);
        statements.add(stmt);
    }

    @Override
    public void addDataset(Dataset dataset) {
        URI datasetContext = datasetUID(baseURI, dataset.getName());
        add(datasetContext, RDF.TYPE, scv_Dataset, datasetContext);
        if (dataset.getTitle() != null) {
            add(datasetContext, dc_title, dataset.getTitle(), datasetContext);
        }
        if (dataset.getDescription() != null) {
            add(datasetContext, dc_description, dataset.getDescription(), datasetContext);
        }

        URI domainContext = vf.createURI(baseURI + DOMAIN);
        String domainNs = domainContext.stringValue() + "#";

        String dimensionBase = baseURI + DIMENSION_CONTEXT;

        // SCHEMA: DimensionTypes
        for (DimensionType type : dataset.getDimensionTypes()) {
            URI t = vf.createURI(domainNs, encodeID(type.getName()));
            URI dimensionContext = vf.createURI(dimensionBase + encodeID(type.getName()));
            String dimensionNs = dimensionContext.stringValue() + "#";

            if (!exists(t, domainContext)) {
                add(t, RDF.TYPE, RDFS.CLASS, domainContext);
                add(t, RDF.TYPE, OWL.CLASS, domainContext);
                add(t, RDFS.SUBCLASSOF, scv_Dimension, domainContext);
                add(t, dc_title, type.getName(), domainContext);
                add(t, meta_instances, dimensionContext, domainContext);
            } else {
                logger.info("Referring to existing DimensionType: " + print(t));
            }

            // INSTANCES: Dimensions
            for (Dimension dimension : type.getDimensions()) {
                URI d = vf.createURI(dimensionNs, encodeID(dimension.getName()));
                dimensions.put(dimension, d);

                if (!exists(d, dimensionContext)) {
                    add(d, RDF.TYPE, t, dimensionContext);
                    add(d, dc_title, dimension.getName(), dimensionContext);
                } else {
                    logger.info("Referring to existing Dimension: " + print(d) + " of type " + print(t));
                }

                // TODO: hierarchy?
                // TODO: subProperty of scv:dimension?
            }
        }
//        conn.update(Collections.<STMT>emptySet(), statements);
        try {
            conn.add(statements);
        } catch (StoreException e) {
            throw new RuntimeException(e);
        }
        statements.clear();
    }

    @Override
    public void addItem(Item item) {
        if (ignoredValues.contains(item.getValue())) {
            if (++skippedCount % 1000 == 0) {
                logger.info("Skipped " + skippedCount + " items");
            }
        } else {
            Dataset dataset = item.getDataset();
            URI datasetContext = datasetUID(baseURI, dataset.getName());

            BNode id = vf.createBNode();

            add(id, RDF.TYPE, scv_Item, datasetContext);

            String value = item.getValue();
            if (value.startsWith("\"")) {
                add(id, RDF.VALUE, value.substring(1, value.length() - 1), datasetContext);
            } else {
                addDecimal(id, RDF.VALUE, value, datasetContext);
            }
            add(id, scv_dataset, datasetContext, datasetContext);

            for (Dimension dimension : item.getDimensions()) {
                // TODO: subProperty of scv:dimension?
                add(id, scv_dimension, dimensions.get(dimension), datasetContext);
            }
            // conn.update(Collections.<STMT>emptySet(), statements);
            try {
                conn.add(statements);
            } catch (StoreException e) {
                throw new RuntimeException(e);
            }

            if (++itemCount % 1000 == 0) {
                logger.info("Loaded " + itemCount + " items");
            }

            statements.clear();
        }
    }

    public void setIgnoredValues(String... values) {
        this.ignoredValues = new HashSet<String>(Arrays.asList(values));
    }

    @Override
    public void begin() {
        conn = ((SesameConnection)repository.openConnection()).getConnection();
        vf = conn.getValueFactory();
        statements = new LinkedHashSet<Statement>();
        dimensions = new HashMap<Dimension, URI>();

        scv_dataset = vf.createURI(SCV.dataset.getId());
        scv_dimension = vf.createURI(SCV.dimension.getId());
        scv_Dataset = vf.createURI(SCV.Dataset.getId());
        scv_Dimension = vf.createURI(SCV.Dimension.getId());
        scv_Item = vf.createURI(SCV.Item.getId());
        dc_title = vf.createURI(DC.title.getId());
        dc_description = vf.createURI(DC.description.getId());
        meta_instances = vf.createURI(META.instances.getId());
    }

    @Override
    public void rollback() {
        try {
            conn.close();
        } catch (StoreException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void commit() {
        try {
            conn.close();
        } catch (StoreException e) {
            throw new RuntimeException(e);
        }
    }

}
