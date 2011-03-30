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
import java.util.Collections;
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
import com.google.common.collect.Sets;
import com.mysema.commons.lang.CloseableIterator;
import com.mysema.query.types.ParamExpression;
import com.mysema.query.types.Predicate;
import com.mysema.rdfbean.model.*;
import com.mysema.stat.META;
import com.mysema.stat.STAT;
import com.mysema.stat.scovo.SCV;

public class SearchServiceImpl implements SearchService {

    private static final int SPARQL_MAX_LIMIT = 1000;

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);

    private final Repository repository;

    private final int minRestrictions;

    public SearchServiceImpl(Repository repository) {
        this(repository, 2);
    }

    public SearchServiceImpl(Repository repository, int minRestrictions) {
        this.repository = repository;
        this.minRestrictions = minRestrictions;
    }

    public Collection<Facet> getFacets() {
        RDFConnection conn = repository.openConnection();
        try {
            Map<UID, Facet> dimensionTypes = new LinkedHashMap<UID,Facet>();

            // DIMENSIONS
            long start = System.currentTimeMillis();
            RDFQuery query = new RDFQueryImpl(conn);
            query.where(
                  dimensionType.has(RDFS.subClassOf, SCV.Dimension),
                  dimensionType.has(DC.title, dimensionTypeName),
                  Blocks.graph(
                     dimensionType,
                     dimension.a(dimensionType),
                     dimension.has(DC.title, dimensionName),
                     dimension.has(DC.description, dimensionDescription).asOptional(),
                     dimension.has(SKOS.broader, parent).asOptional()
                  ));
            query.orderBy(dimensionName.asc());

            addFacets(conn, dimensionTypes, query.selectAll());
            logDuration("getFacets(): Dimensions", System.currentTimeMillis() - start);

            // DATASETS
            start = System.currentTimeMillis();
            query = new RDFQueryImpl(conn);
            // Query datasets as a kind of dimension
            query.where(
                  dimension.a(dimensionType), // dimensionType = scv:Dataset
                  dimension.has(DC.title, dimensionName), // datasetName
                  dimension.has(DC.description, dimensionDescription).asOptional());
            query.set(dimensionType, SCV.Dataset);
            query.orderBy(dimensionName.asc());

            addFacets(conn, dimensionTypes, query.selectAll());
            logDuration("getFacets(): Datasets", System.currentTimeMillis() - start);

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

        Map<ParamExpression<UID>, UID> bindings = new HashMap<ParamExpression<UID>, UID>();
        List<Predicate> filters = getSearchFilters(facetRestrictions, bindings);

        boolean containsDatasetRestriction = facetRestrictions.containsKey(SCV.Dataset);

        findAvailableDimensions(filters, bindings, headers, conn);
        findAvailableDatasets(filters, bindings, containsDatasetRestriction, headers, conn);

        SearchResults results = new SearchResults();
        if (includeItems) {
            List<Item> items = findItems(filters, bindings, limit, offset, headers, conn);
            boolean hasMoreResults = items.size() > limit;
            if (hasMoreResults) {
                results.setHasMoreResults(true);
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

    private List<Item> findItems(List<Predicate> filters, Map<ParamExpression<UID>, UID> bindings, int limit, int offset, Headers headers, RDFConnection conn) {
        filters.add(item.has(RDF.value, value));

        RDFQuery query = new RDFQueryImpl(conn);
        query.limit(limit+1).offset(offset);

        if (bindings.containsKey(dataset)){
            query.from(bindings.get(dataset)).where(filters.toArray(new Predicate[filters.size()]));
        }else{
            query.where(Blocks.graph(dataset, filters));
        }

        for (Map.Entry<ParamExpression<UID>, UID> entry : bindings.entrySet()){
            query.set(entry.getKey(), entry.getValue());
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
        logDuration("findItems", System.currentTimeMillis() - start);

        return results;
    }

    private void findAvailableDimensions(List<Predicate> filters, Map<ParamExpression<UID>, UID> bindings, Headers headers, RDFConnection conn) {
        RDFQuery query = new RDFQueryImpl(conn);
        query
            .where(filters.toArray(new Predicate[filters.size()]))
            .where(
                item.has(SCV.dimension, dimension))
            .distinct();

        if (bindings.containsKey(dataset)){
            query.from(bindings.get(dataset));
        }

        for (Map.Entry<ParamExpression<UID>, UID> entry : bindings.entrySet()){
            query.set(entry.getKey(), entry.getValue());
        }

        long start = System.currentTimeMillis();
        CloseableIterator<Map<String,NODE>> iter = query.select(dimension);
        try {
            while (iter.hasNext()) {
                Map<String,NODE> row = iter.next();
                UID dimensionUID = row.get(dimension.getName()).asURI();
                UID dimensionTypeUID = new UID(dimensionUID.ns().substring(0, dimensionUID.ns().length()-1));
                headers.addFacetValue(dimensionTypeUID, dimensionUID);
            }
        } finally {
            iter.close();
        }
        logDuration("findAvailableDimensions", System.currentTimeMillis() - start);
    }

    private void findAvailableDatasets(List<Predicate> filters, Map<ParamExpression<UID>, UID> bindings, boolean containsDatasetRestriction, Headers headers, RDFConnection conn) {
        RDFQuery query = new RDFQueryImpl(conn);
        query.distinct();

        if (!containsDatasetRestriction) {
            filters.add(item.has(SCV.dataset, dataset));
        }

        if (bindings.containsKey(dataset)){
            query.from(bindings.get(dataset)).where(filters.toArray(new Predicate[filters.size()]));
        }else{
            query.where(Blocks.graph(dataset, filters));
        }

        for (Map.Entry<ParamExpression<UID>, UID> entry : bindings.entrySet()){
            query.set(entry.getKey(), entry.getValue());
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
        logDuration("findAvailableDatasets", System.currentTimeMillis() - start);
    }

    private void logDuration(String title, long duration){
        if (log.isInfoEnabled() && duration > 500){
            log.info(title + " took " + duration + "ms");
        }
    }

    private List<Predicate> getSearchFilters(ListMultimap<UID, UID> facetRestrictions, Map<ParamExpression<UID>, UID> bindings) {
        List<Predicate> filters = new ArrayList<Predicate>();

        int dimensionRestrictionCount = 0;
        for (UID facet : facetRestrictions.keySet()) {
            List<UID> values = facetRestrictions.get(facet);
            if (facet.equals(SCV.Dataset)) {
                filters.add(item.has(SCV.dataset, dataset));
                if (values.size() == 1){
                    bindings.put(dataset, values.get(0));
                }else{
                    filters.add(dataset.in(values));
                }
            } else {
                filters.addAll(equalsIn(item, SCV.dimension, values, "dimensionRestriction", ++dimensionRestrictionCount));
            }
        }
        return filters;
    }

    private List<? extends Predicate> equalsIn(QUID subject, UID predicate, Collection<UID> values, String varName, int varIndex) {
        if (values.size() == 1) {
            return Collections.singletonList(subject.has(predicate, values.iterator().next()));
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
                filters.add(dataset.in(values));
            } else {
                excludedDimensionTypes.add(facet);
                includedDimensions.addAll(values);
                filters.addAll(equalsIn(dataset, STAT.datasetDimension, values, "dimensionRestriction", ++dimensionRestrictionCount));
            }
        }

        if (!excludedDimensionTypes.isEmpty()) {
            filters.add(dimension.a(dimensionType));
            filters.add(
                    dimensionType.notIn(excludedDimensionTypes)
                    .or(dimension.in(includedDimensions))
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

    private ListMultimap<UID, UID> getFacetRestrictions(Set<UID> restrictions, RDFConnection conn) {
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
