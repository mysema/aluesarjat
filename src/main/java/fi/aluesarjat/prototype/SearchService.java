package fi.aluesarjat.prototype;

import static fi.aluesarjat.prototype.Constants.dataset;
import static fi.aluesarjat.prototype.Constants.dimension;
import static fi.aluesarjat.prototype.Constants.dimensionDescription;
import static fi.aluesarjat.prototype.Constants.dimensionName;
import static fi.aluesarjat.prototype.Constants.dimensionType;
import static fi.aluesarjat.prototype.Constants.dimensionTypeName;
import static fi.aluesarjat.prototype.Constants.item;
import static fi.aluesarjat.prototype.Constants.parent;
import static fi.aluesarjat.prototype.Constants.value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mysema.commons.lang.CloseableIterator;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.rdfbean.model.Blocks;
import com.mysema.rdfbean.model.DC;
import com.mysema.rdfbean.model.LIT;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.QID;
import com.mysema.rdfbean.model.RDF;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.RDFQuery;
import com.mysema.rdfbean.model.RDFQueryImpl;
import com.mysema.rdfbean.model.RDFS;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.SKOS;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.model.UID;
import com.mysema.stat.META;
import com.mysema.stat.STAT;
import com.mysema.stat.scovo.SCV;

public class SearchService {

    private static final int SPARQL_MAX_LIMIT = 1000;

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private static class Headers {
        private final Map<UID, Integer> headerIndexes = Maps.newLinkedHashMap();
        private final Map<UID, UID> valueToFacet = Maps.newHashMap();

        public int addFacetValue(UID facet, UID value) {
            valueToFacet.put(value, facet);
            Integer index = headerIndexes.get(facet);
            if (index == null) {
                index = headerIndexes.size();
                headerIndexes.put(facet, index);
            }
            return index.intValue();
        }

        public int getHeaderCount() {
            return headerIndexes.size();
        }

        public List<UID> getHeaders() {
            return Lists.newArrayList(headerIndexes.keySet());
        }

        public Set<UID> getAvailableValues() {
            return valueToFacet.keySet();
        }

        public int getFacetIndex(UID facet) {
            Integer index = headerIndexes.get(facet);
            if (index == null) {
                throw new IllegalArgumentException("Unknown value: " + facet);
            }
            return index.intValue();
        }

        public int getValueIndex(UID value) {
            return getFacetIndex(valueToFacet.get(value));
        }
    }

    private final Repository repository;
    
    private final int minRestrictions;

    public SearchService(Repository repository) {
        this(repository, 2);
    }

    public SearchService(Repository repository, int minRestrictions) {
        this.repository = repository;
        this.minRestrictions = minRestrictions;
    }

    public Collection<Facet> getFacets() {
        RDFConnection conn = repository.openConnection();
        try {
            Map<UID, Facet> dimensionTypes = new LinkedHashMap<UID,Facet>();

            // DIMENSIONS
            RDFQuery query = new RDFQueryImpl(conn);
            query.where(
                  dimensionType.has(RDFS.subClassOf, SCV.Dimension),
                  dimensionType.has(DC.title, dimensionTypeName),
                  dimension.a(dimensionType),
                  dimension.has(DC.title, dimensionName),
                  Blocks.optional(dimension.has(DC.description, dimensionDescription)),
                  Blocks.optional(dimension.has(new UID(SKOS.NS, "broader"), parent)));
            query.orderBy(dimensionName.asc());

            addFacets(conn, dimensionTypes, query.selectAll());

            // DATASETS
            query = new RDFQueryImpl(conn);
            // Query datasets as a kind of dimension
            query.where(
                  dimension.a(dimensionType), // dimensionType = scv:Dataset
                  dimension.has(DC.title, dimensionName), // datasetName
                  Blocks.optional(dimension.has(DC.description, dimensionDescription)));
            query.set(dimensionType, SCV.Dataset);
            query.orderBy(dimensionName.asc());

            addFacets(conn, dimensionTypes, query.selectAll());

            return dimensionTypes.values();
        } finally {
            conn.close();
        }
    }


    protected void addFacets(RDFConnection conn, Map<UID,Facet> dimensionTypes, CloseableIterator<Map<String,NODE>> results) {
        try {
            Map<String, NODE> row;
            while (results.hasNext()) {
                row = results.next();
                addFacet(row, dimensionTypes);
            }
        } finally {
            results.close();
        }
    }

    protected void addFacet(Map<String, NODE> row, Map<UID,Facet> facets) {
        UID type = (UID) row.get(dimensionType.getName());
        Facet facet = facets.get(type);
        if (facet == null) {
            // Create dimension type
            facet = new Facet(type);
            if (type.equals(SCV.Dataset)) {
                facet.setName("Tilasto");
            } else {
                LIT typeName = (LIT) row.get(dimensionTypeName.getName());
                if (typeName != null) {
                    facet.setName(typeName.getValue());
                }
            }
            facets.put(type, facet);
        }
        // Create dimension
        Value value = new Value((UID) row.get(dimension.getName()));

        // Literal metadata
        LIT lit = (LIT) row.get(dimensionName.getName());
        if (lit != null) {
            value.setName(lit.getValue());
        }

        lit = (LIT) row.get(dimensionDescription.getName());
        if (lit != null) {
            value.setDescription(lit.getValue());
        }

        UID uid = (UID) row.get(parent.getName());
        if (uid != null) {
            value.setParent(uid);
        }

        facet.addValue(value);
    }

    public Map<UID, String> getNamespaces() {
        RDFConnection conn = repository.openConnection();
        try {
            CloseableIterator<STMT> stmts = conn.findStatements(null, META.nsPrefix, null, null, false);
            Map<UID,String> namespaces = new HashMap<UID, String>(32);
            try{
                while (stmts.hasNext()){
                    STMT stmt = stmts.next();
                    namespaces.put((UID) stmt.getSubject(), stmt.getObject().getValue());
                }
            }finally{
                stmts.close();
            }
            return namespaces;
        } finally {
            conn.close();
        }
    }

    public SearchResults search(Set<UID> restrictions, boolean includeItems, int limit, int offset, boolean includeAvailableValues) {
        RDFConnection conn = repository.openConnection();
        try {
            ListMultimap<UID, UID> facetRestrictions = getFacetRestrictions(restrictions, conn);

            if (facetRestrictions.keySet().size() < minRestrictions) {
                return getAvailableDatasetValues(facetRestrictions, conn);
            } else {
                return getResults(facetRestrictions, includeItems, limit, offset, includeAvailableValues, conn);
            }
        } finally {
            conn.close();
        }
    }

    private SearchResults getResults(ListMultimap<UID, UID> facetRestrictions, boolean includeItems, int limit, int offset, boolean includeAvailableValues, RDFConnection conn) {
        limit = Math.min(limit, SPARQL_MAX_LIMIT);
        Headers headers = new Headers();

        List<Predicate> filters = getSearchFilters(facetRestrictions);

        boolean containsDatasetRestriction = facetRestrictions.containsKey(SCV.Dataset);

        findAvailableDimensions(filters, headers, conn);
        findAvailableDatasets(filters, containsDatasetRestriction, headers, conn);

        SearchResults results = new SearchResults();
        if (includeItems) {
            List<Item> items = findItems(filters, containsDatasetRestriction, limit, offset, headers, conn);
            boolean hasMoreResults = items.size() > limit;
            if (hasMoreResults) {
                items = items.subList(0, limit);
            }
            results.setHeaders(headers.getHeaders());
            results.setItems(items);
        }
        if (includeAvailableValues) {
            results.setAvailableValues(headers.getAvailableValues());
        }
        return results;
    }

    private List<Item> findItems(List<Predicate> filters, boolean containsDatasetRestriction, int limit, int offset, Headers headers, RDFConnection conn) {

        RDFQuery query = new RDFQueryImpl(conn);
        query
            .where(filters.toArray(new Predicate[filters.size()]))
            .where(
                item.has(RDF.value, value))
            .limit(limit+1)
            .offset(offset);

        if (!containsDatasetRestriction) {
            query.where(item.has(SCV.dataset, dataset));
        }

        List<Item> results = new ArrayList<Item>(limit+1);
        long start = System.currentTimeMillis();
        CloseableIterator<Map<String,NODE>> iter = query.select(item, dataset, value);
        try {
            while (iter.hasNext()) {
                Map<String,NODE> row = iter.next();
                UID id = (UID) row.get(item.getName());
                UID datasetUID = (UID) row.get(dataset.getName());
                LIT valueLIT = (LIT) row.get(value.getName());

                Item item = new Item(id, valueLIT.getValue(), headers.getHeaderCount());
                item.setValue(headers.getFacetIndex(SCV.Dataset), datasetUID);

                CloseableIterator<STMT> stmts = conn.findStatements(id, SCV.dimension, null, datasetUID, false);
                try {
                    while (stmts.hasNext()) {
                        STMT stmt = stmts.next();
                        UID dimensionUID = (UID) stmt.getObject();
                        item.setValue(headers.getValueIndex(dimensionUID), dimensionUID);
                    }
                } finally {
                    stmts.close();
                }
                results.add(item);
            }
        } finally {
            iter.close();
        }
        logDuration("Available dimensions query", System.currentTimeMillis() - start);

        return results;
    }

    private void findAvailableDimensions(List<Predicate> filters, Headers headers, RDFConnection conn) {
        RDFQuery query = new RDFQueryImpl(conn);
        query
            .where(filters.toArray(new Predicate[filters.size()]))
            .where(
                item.has(SCV.dimension, dimension),
                dimension.a(dimensionType))
            .distinct();

        long start = System.currentTimeMillis();
        CloseableIterator<Map<String,NODE>> iter = query.select(dimension, dimensionType);
        try {
            while (iter.hasNext()) {
                Map<String,NODE> row = iter.next();
                headers.addFacetValue((UID) row.get(dimensionType.getName()), (UID) row.get(dimension.getName()));
            }
        } finally {
            iter.close();
        }
        logDuration("Available dimensions query", System.currentTimeMillis() - start);
    }

    private void findAvailableDatasets(List<Predicate> filters, boolean containsDatasetRestriction, Headers headers, RDFConnection conn) {
        RDFQuery query = new RDFQueryImpl(conn);
        query
            .where(filters.toArray(new Predicate[filters.size()]))
            .distinct();

        if (!containsDatasetRestriction) {
            query.where(item.has(SCV.dataset, dataset));
        }

        long start = System.currentTimeMillis();
        CloseableIterator<Map<String,NODE>> iter = query.select(dataset);
        try {
            while (iter.hasNext()) {
                Map<String,NODE> row = iter.next();
                headers.addFacetValue(SCV.Dataset, (UID) row.get(dataset.getName()));
            }
        } finally {
            iter.close();
        }
        logDuration("Available datasets query", System.currentTimeMillis() - start);
    }

    private void logDuration(String title, long duration){
        if (log.isInfoEnabled() && duration > 500){
            log.info(title + " took " + duration + "ms");
        }
    }

    private List<Predicate> getSearchFilters(
            ListMultimap<UID, UID> facetRestrictions) {
        List<Predicate> filters = new ArrayList<Predicate>();

        int dimensionRestrictionCount = 0;
        for (UID facet : facetRestrictions.keySet()) {
            List<UID> values = facetRestrictions.get(facet);
            if (facet.equals(SCV.Dataset)) {
                filters.add(item.has(SCV.dataset, dataset));
                filters.add(equalsIn(dataset, values));
            } else {
                filters.addAll(equalsIn(item, SCV.dimension, values, "dimensionRestriction", ++dimensionRestrictionCount));
            }
        }
        return filters;
    }
    
    private BooleanExpression equalsIn(QID subject, Collection<UID> values) {
        if (values.size() == 1) {
            return subject.eq(values.iterator().next());
        } else {
            return subject.in(values);
        }
    }
    
    private BooleanExpression notEqualsIn(QID subject, Collection<UID> values) {
        if (values.size() == 1) {
            return subject.ne(values.iterator().next());
        } else {
            return subject.notIn(values);
        }
    }
    
    private List<? extends Predicate> equalsIn(QID subject, UID predicate, Collection<UID> values, String varName, int varIndex) {
        if (values.size() == 1) {
            return Lists.newArrayList(subject.has(predicate, values.iterator().next()));
        } else {
            QID var = new QID(varName + varIndex);
            return Lists.newArrayList(
                    subject.has(predicate, var),
                    var.in(values)
            );
        }
    }
    
    private SearchResults getAvailableDatasetValues(ListMultimap<UID, UID> facetRestrictions, RDFConnection conn) {
        SearchResults result = new SearchResults();

        List<Predicate> filters = new ArrayList<Predicate>();

        filters.add(dataset.has(STAT.datasetDimension, dimension));

        Set<UID> excludedDimensionTypes = Sets.newLinkedHashSet();
        Set<UID> includedDimensions = Sets.newLinkedHashSet();
        
        int dimensionRestrictionCount = 0;
        for (UID facet : facetRestrictions.keySet()) {
            List<UID> values = facetRestrictions.get(facet);
            if (facet.equals(SCV.Dataset)) {
                filters.add(equalsIn(dataset, values));
            } else {
                excludedDimensionTypes.add(facet);
                includedDimensions.addAll(values);
                filters.addAll(equalsIn(dataset, STAT.datasetDimension, values, "dimensionRestriction", ++dimensionRestrictionCount));
            }
        }

        if (!excludedDimensionTypes.isEmpty()) {
            filters.add(dimension.a(dimensionType));
            filters.add(
                    notEqualsIn(dimensionType, excludedDimensionTypes)
                    .or(equalsIn(dimension, includedDimensions))
            );
        }

        // DIMENSIONS
        RDFQuery query = new RDFQueryImpl(conn)
            .where(filters.toArray(new Predicate[filters.size()]))
            .distinct();

        CloseableIterator<Map<String, NODE>> iter = query.select(dimension);
        try {
            Map<String, NODE> row;
            while (iter.hasNext()) {
                row = iter.next();
                result.addAvailableValue((UID) row.get(dimension.getName()));
            }
        }finally{
            iter.close();
        }

        // DATASETS
        query = new RDFQueryImpl(conn)
            .where(filters.toArray(new Predicate[filters.size()]))
            .distinct();

        iter = query.select(dataset);
        try {
            Map<String, NODE> row;
            while (iter.hasNext()) {
                row = iter.next();
                result.addAvailableValue((UID) row.get(dataset.getName()));
            }
        }finally{
            iter.close();
        }

        return result;
    }

    private ListMultimap<UID, UID> getFacetRestrictions(Set<UID> restrictions,
            RDFConnection conn) {
        ListMultimap<UID, UID> facetValues = ArrayListMultimap.create();
        if (!restrictions.isEmpty()) {
            RDFQuery query = new RDFQueryImpl(conn);
            query.where(dimension.a(dimensionType), dimension.in(restrictions));
    
            CloseableIterator<Map<String, NODE>> iter = query.select(dimension, dimensionType);
            try {
                Map<String, NODE> row;
                while (iter.hasNext()) {
                    row = iter.next();
                    facetValues.put((UID) row.get(dimensionType.getName()), (UID) row.get(dimension.getName()));
                }
            }finally{
                iter.close();
            }
        }
        return facetValues;
    }

}
