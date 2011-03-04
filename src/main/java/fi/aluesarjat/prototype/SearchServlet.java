package fi.aluesarjat.prototype;

import static fi.aluesarjat.prototype.Constants.dataset;
import static fi.aluesarjat.prototype.Constants.dimension;
import static fi.aluesarjat.prototype.Constants.dimensionType;
import static fi.aluesarjat.prototype.Constants.item;
import static fi.aluesarjat.prototype.Constants.value;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.commons.lang.IteratorAdapter;
import com.mysema.query.types.Predicate;
import com.mysema.rdfbean.model.LIT;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.RDF;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.RDFQuery;
import com.mysema.rdfbean.model.RDFQueryImpl;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.model.UID;
import com.mysema.stat.STAT;
import com.mysema.stat.scovo.SCV;

public class SearchServlet extends AbstractFacetSearchServlet {

    private static final Logger log = LoggerFactory.getLogger(SearchServlet.class);

    private static final long serialVersionUID = 2149808648205848159L;

    private static final int SPARQL_DEFAULT_LIMIT = 200;

    private static final int SPARQL_MAX_LIMIT = 1000;

    private static final String[] EMPTY = new String[0];

    private final Repository repository;

    public SearchServlet(Repository repository) {
        this.repository = repository;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Set<String> includes = getIncludes(request.getParameterValues("include"));

        int limit = Math.min(getInt(request, "limit", SPARQL_DEFAULT_LIMIT), SPARQL_MAX_LIMIT);
        int offset = getInt(request, "offset", 0);
        boolean hasMoreResults = false;

        RDFConnection conn = repository.openConnection();
        CloseableIterator<Map<String,NODE>> iter = null;
        CloseableIterator<STMT> stmts = null;
        try {

            List<JSONObject> items = null;
            Map<UID,JSONObject> facets = null;
            Map<String, NODE> row;

            // NAMESPACES
            Map<String,String> namespaces = getNamespaces(conn);

            List<UID> dimensions = new ArrayList<UID>();
            List<UID> datasets = new ArrayList<UID>();
            for (String value : nullToEmpty(request.getParameterValues("value"))) {
                if (value.startsWith("dataset:")) {
                    datasets.add(getUID(value, namespaces));
                } else {
                    dimensions.add(getUID(value, namespaces));
                }
            }
            int restrictionCount = datasets.size() + dimensions.size();

            // Not enough restrictions for searching items -> find facet estimation
            if (restrictionCount < 2) {
                RDFQuery query = new RDFQueryImpl(conn);

                facets = new LinkedHashMap<UID,JSONObject>(16);

                // Find available dimensions via dataset definitions

                List<Predicate> filters = new ArrayList<Predicate>();
                if (!datasets.isEmpty()) {
                    query.set(dataset, datasets.get(0));
                }

                filters.add(dataset.has(STAT.datasetDimension, dimension));
                filters.add(dimension.a(dimensionType));

                // TODO: Optimize as single in-query
                for (UID dimension : dimensions){
                    // Only one dimension per dimensionType is selectable -> exclude selected dimensionTypes
                    stmts = conn.findStatements(dimension, RDF.type, null, null, false);
                    try{
                        if (stmts.hasNext()) {
                            STMT stmt = stmts.next();
                            filters.add(dimensionType.ne(stmt.getObject().asURI()));
                        }
                    }finally{
                        stmts.close();
                    }
                }

                // Required (selected) dimension
                for (UID dimension : dimensions){
                    filters.add(dataset.has(STAT.datasetDimension, dimension));
                }

                // DIMENSIONS
                long start = System.currentTimeMillis();
                iter = query
                    .where(filters.toArray(new Predicate[filters.size()]))
                    .distinct()
                    .select(dimension, dimensionType);
                try{
                    while (iter.hasNext()) {
                        row = iter.next();
                        addFacet(row, namespaces, facets);
                    }
                }finally{
                    iter.close();
                }
                logDuration("Dimensions query", System.currentTimeMillis() - start);

                // DATASETS!
                query = new RDFQueryImpl(conn);
                if (datasets.isEmpty()) {
                    start = System.currentTimeMillis();
                    iter = query
                        .where(filters.toArray(new Predicate[filters.size()]))
                        .distinct()
                        .select(dataset);
    
                    try{
                        while (iter.hasNext()) {
                            row = iter.next();
                            UID datasetUID = (UID) row.get(dataset.getName());
                            row = new HashMap<String, NODE>();
                            row.put("dimension", datasetUID);
                            row.put("dimensionType", SCV.Dataset);
                            addFacet(row, namespaces, facets);
                        }
                    }finally{
                        iter.close();
                    }
                    logDuration("Datasets query", System.currentTimeMillis() - start);
                }

            }
            // Find items and exact non-empty facet values
            else {
                items = new ArrayList<JSONObject>(limit);
                List<Predicate> filters = new ArrayList<Predicate>();

                for (UID dimension : dimensions){
                    filters.add(item.has(SCV.dimension, dimension));
                }

                // Distinct items
                if (includes.contains("items")) {
                    RDFQuery query = new RDFQueryImpl(conn);
                    query
                        .where(filters.toArray(new Predicate[filters.size()]))
                        .where(
                            item.has(SCV.dataset, dataset),
                            item.has(RDF.value, value))
                        .limit(limit+1)
                        .offset(offset);

                    if (!datasets.isEmpty()) {
                        query.set(dataset, datasets.get(0));
                    }

                    long start = System.currentTimeMillis();
                    List<Map<String, NODE>> tuplesList = IteratorAdapter.asList(query.select(item, dataset, value));
                    logDuration("Items query", System.currentTimeMillis() - start);

                    if (tuplesList.size() > limit){
                        hasMoreResults = true;
                        tuplesList = tuplesList.subList(0, limit);
                    }
                    for (Map<String,NODE> tuples : tuplesList){
                        UID id = (UID) tuples.get(item.getName());
                        UID datasetUID = (UID) tuples.get(dataset.getName());
                        LIT valueLIT = (LIT) tuples.get(value.getName());

                        JSONObject json = new JSONObject();
                        json.put("value", valueLIT.getValue());
                        json.accumulate("values", getPrefixed(datasetUID, namespaces));

                        stmts = conn.findStatements(id, SCV.dimension, null, datasetUID, false);
                        try{
                            while (stmts.hasNext()) {
                                STMT stmt = stmts.next();
                                json.accumulate("values", getPrefixed((UID) stmt.getObject(), namespaces));
                            }
                        }finally{
                            stmts.close();
                        }
                        items.add(json);
                    }
                }

                // AVAILABLE DIMENSIONS
                if (includes.contains("facets")) {
                    facets = new LinkedHashMap<UID,JSONObject>(16);
                    RDFQuery query = new RDFQueryImpl(conn);
                    query
                        .where(filters.toArray(new Predicate[filters.size()]))
                        .where(
                            item.has(SCV.dimension, dimension),
                            dimension.a(dimensionType))
                        .distinct();

                    if (!datasets.isEmpty()) {
                        query.where(item.has(SCV.dataset, datasets.get(0)));
                    }

                    long start = System.currentTimeMillis();
                    addFacets(conn,namespaces, facets, query.select(dimension, dimensionType));
                    logDuration("Available dimensions query", System.currentTimeMillis() - start);

                    // AVAILABLE DATASETS
                    query = new RDFQueryImpl(conn);
                    query
                        .where(filters.toArray(new Predicate[filters.size()]))
                        .where(
                            item.has(SCV.dataset, dimension),
                            dimension.a(dimensionType))
                        .distinct();

                    start = System.currentTimeMillis();
                    addFacets(conn, namespaces, facets, query.select(dimension, dimensionType));
                    logDuration("Available datasets query", System.currentTimeMillis() - start);
                }
            }

            JSONObject result = new JSONObject();
            if (items != null && includes.contains("items")) {
                result.put("items", items);
            }
            if (facets != null && includes.contains("facets")) {
                result.put("facets", facets.values());
            }
            if (hasMoreResults){
                result.put("hasMoreResults", true);
            }
            Writer out = response.getWriter();
            String jsonpCallback = request.getParameter("callback");
            if (jsonpCallback != null){
                out.write(jsonpCallback + "(");
                result.write(out);
                out.write(")");
            }else{
                result.write(out);
            }
            out.flush();
        } finally {
            if (stmts != null) {
                stmts.close();
            }
            if (iter != null) {
                iter.close();
            }
            conn.close();
        }
    }

    private Set<String> getIncludes(String[] values) {
        String[] parameterValues = values;
        if (parameterValues == null) {
            parameterValues = new String[] {"items", "values"};
        }
        return new HashSet<String>(Arrays.asList(parameterValues));
    }

    private UID getUID(String prefixed, Map<String, String> namespaces) {
        int i = prefixed.indexOf(':');
        if (i < 0) {
            throw new IllegalArgumentException("Not an URI: " + prefixed);
        }
        String prefix = prefixed.substring(0, i);
        String localName = prefixed.substring(i+1);
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            if (entry.getValue().equals(prefix)) {
                return new UID(entry.getKey(), localName);
            }
        }
        throw new IllegalArgumentException("Unknown prefix: " + prefixed);
    }

    private int getInt(HttpServletRequest request, String name, int defaultValue) {
        String value = request.getParameter(name);
        if (value != null) {
            return Integer.parseInt(value);
        } else {
            return defaultValue;
        }
    }

    private String[] nullToEmpty(String[] values) {
        return values == null ? EMPTY : values;
    }

    private void logDuration(String title, long duration){
        if (log.isInfoEnabled() && duration > 500){
            log.info(title + " took " + duration + "ms");
        }
    }

}
