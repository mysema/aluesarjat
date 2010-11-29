package fi.aluesarjat.prototype;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import com.mysema.rdfbean.model.*;
import com.mysema.stat.scovo.SCV;

public class SearchServlet extends AbstractFacetSearchServlet {

    private static final Logger log = LoggerFactory.getLogger(SearchServlet.class);

    private static final long serialVersionUID = 2149808648205848159L;

    private static final String[] EMPTY = new String[0];

    private final Repository repository;

    public SearchServlet(Repository repository) {
        this.repository = repository;
    }

    private StringBuilder whereDimensions(List<String> dimensions, String subject, String predicate) {
        StringBuilder where = new StringBuilder();
        if (!dimensions.isEmpty()) {
            where.append(subject);
            // TODO Should we support multiple selections from a given facet?
            for (int i=0; i < dimensions.size(); i++) {
                if (i > 0) {
                    where.append(" ;\n");
                }
                // TODO use parameters
                where.append(" ").append(predicate).append(" ").append(dimensions.get(i));
            }
            where.append(" .\n");
        }
        return where;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        List<String> dimensions = new ArrayList<String>();
        List<String> datasets = new ArrayList<String>();
        for (String value : nullToEmpty(request.getParameterValues("value"))) {
            if (value.startsWith("dataset:")) {
                datasets.add(value);
            } else {
                dimensions.add(value);
            }
        }
        Set<String> includes = getIncludes(request.getParameterValues("include"));

        int limit = Math.min(getInt(request, "limit", 200), 1000);
        int offset = getInt(request, "offset", 0);

        RDFConnection conn = repository.openConnection();
        CloseableIterator<Map<String,NODE>> iter = null;
        CloseableIterator<STMT> stmts = null;
        try {
            int restrictionCount = datasets.size() + dimensions.size();
            if (restrictionCount == 0) {
                throw new IllegalArgumentException("No restrictions (value) specified");
            }
            List<JSONObject> items = null;
            Map<UID,JSONObject> facets = null;
            StringBuilder sparql;
            SPARQLQuery query;
            Map<String, NODE> row;

            // NAMESPACES
            Map<String,String> namespaces = getNamespaces(conn);
            StringBuilder sparqlNamespaces = getSPARQLNamespaces(namespaces);

            // Not enough restrictions for searching items -> find facet estimation
            if (restrictionCount < 3) {
                facets = new LinkedHashMap<UID,JSONObject>();

                // Find available dimensions via dataset definitions

                StringBuilder filter = new StringBuilder();
                if (datasets.size() > 0) {
                    filter.append("FILTER (?dataset = ").append(datasets.get(0)).append("\n");
                }
                for (int i=0; i < dimensions.size(); i++) {
                    if (filter.length() == 0) {
                        filter.append("FILTER (");
                    } else {
                        filter.append(" && ");
                    }

                    stmts = conn.findStatements(getUID(dimensions.get(i), namespaces), RDF.type, null, null, false);
                    if (stmts.hasNext()) {
                        STMT stmt = stmts.next();
                        filter.append("?dimensionType != <");
                        filter.append(((UID) stmt.getObject()).getId());
                        filter.append(">\n");
                    }
                    stmts.close();
                }

                StringBuilder where = whereDimensions(dimensions, "?dataset", "stat:datasetDimension");
                sparql = new StringBuilder()
                .append(sparqlNamespaces)
                .append("SELECT ?dataset ?dimension ?dimensionType\nWHERE {\n")
                .append(where)
                .append("?dataset stat:datasetDimension ?dimension .\n?dimension rdf:type ?dimensionType .\n");

                if (filter.length() > 0) {
                    sparql.append(filter).append(")\n}");
                } else {
                    sparql.append("}");
                }

                Set<UID> distinctDataset = new HashSet<UID>();

                if (log.isInfoEnabled()) {
                    log.info(sparql.toString());
                }
                query = conn.createQuery(QueryLanguage.SPARQL, sparql.toString());
                iter = query.getTuples();
                while (iter.hasNext()) {
                    row = iter.next();
                    addFacet(row, namespaces, facets);

                    UID dataset = (UID) row.get("dataset");
                    if (distinctDataset.add(dataset)) {
                        row = new HashMap<String, NODE>();
                        row.put("dimension", dataset);
                        row.put("dimensionType", SCV.Dataset);
                        addFacet(row, namespaces, facets);
                    }
                }
                iter.close();
            }
            // Find items and exact non-empty facet values
            else {
                items = new ArrayList<JSONObject>(limit);
                StringBuilder where = whereDimensions(dimensions, "?item", "scv:dimension");

                if (!datasets.isEmpty()) {
                    // TODO Should we support multiple dataset selections?
                    // TODO use parameters
                    where.append("?item scv:dataset " + datasets.get(0) + " .\n");
                }

                // Distinct items
                if (includes.contains("items")) {
                    sparql = new StringBuilder()
                    .append(sparqlNamespaces)
                    .append("SELECT ?item ?dataset ?value\nWHERE {\n")
                    .append(where)
                    .append("?item scv:dataset ?dataset ; rdf:value ?value .\n}\nLIMIT ")
                    .append(limit)
                    .append("\nOFFSET ")
                    .append(offset);

                    if (log.isInfoEnabled()) {
                        log.info(sparql.toString());
                    }
                    query = conn.createQuery(QueryLanguage.SPARQL, sparql.toString());
                    iter = query.getTuples();
                    while (iter.hasNext()) {
                        row = iter.next();
                        UID id = (UID) row.get("item");
                        UID dataset = (UID) row.get("dataset");
                        LIT value = (LIT) row.get("value");

                        JSONObject json = new JSONObject();
                        json.put("value", value.getValue());
                        json.accumulate("values", getPrefixed(dataset, namespaces));

                        stmts = conn.findStatements(id, SCV.dimension, null, dataset, false);
                        while (stmts.hasNext()) {
                            STMT stmt = stmts.next();
                            json.accumulate("values", getPrefixed((UID) stmt.getObject(), namespaces));
                        }
                        stmts.close();
                        items.add(json);
                    }
                    iter.close();
                }

                // AVAILABLE DIMENSIONS
                if (includes.contains("facets")) {
                    facets = new LinkedHashMap<UID,JSONObject>();

                    sparql = new StringBuilder()
                    .append(sparqlNamespaces)
                    .append("SELECT distinct ?dimensionType ?dimension\nWHERE {\n")
                    .append(where)
//                    .append("?item scv:dimension ?dimension . OPTIONAL { ?dimension rdf:type ?dimensionType } .\n}");
                    .append("?item scv:dimension ?dimension . ?dimension rdf:type ?dimensionType .\n}");

                    addFacets(conn, sparql.toString(), namespaces, facets, null);

                    // AVAILABLE DATASETS
                    sparql = new StringBuilder()
                    .append(sparqlNamespaces)
                    .append("SELECT distinct ?dimension ?units\nWHERE {\n")
                    .append(where)
                    .append("?item scv:dataset ?dimension . OPTIONAL { ?dimension stat:units ?units }\n}");

                    addFacets(conn, sparql.toString(), namespaces, facets,
                            Collections.singletonMap("dimensionType", (NODE) SCV.Dataset));
                }
            }

            JSONObject result = new JSONObject();
            if (items != null && includes.contains("items")) {
                result.put("items", items);
            }
            if (facets != null && includes.contains("facets")) {
                result.put("facets", facets.values());
            }
            Writer out = response.getWriter();
            result.write(out);
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

    private Set<String> getIncludes(String[] parameterValues) {
        if (parameterValues == null) {
            parameterValues = new String[] {"items", "values"};
        }
        return new HashSet<String>(Arrays.asList(parameterValues));
    }

    private ID getUID(String prefixed, Map<String, String> namespaces) {
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

}
