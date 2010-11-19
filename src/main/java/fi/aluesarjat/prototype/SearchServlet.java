package fi.aluesarjat.prototype;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.model.ID;
import com.mysema.rdfbean.model.LIT;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.QueryLanguage;
import com.mysema.rdfbean.model.RDF;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.SPARQLQuery;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.model.UID;
import com.mysema.stat.scovo.SCV;

public class SearchServlet extends HttpServlet {

    private static final long serialVersionUID = 2149808648205848159L;

    private static final String[] EMPTY = new String[0];

    private Repository repository;

    public SearchServlet(Repository repository) {
        this.repository = repository;
    }

    private StringBuilder whereDimensions(String[] dimensions, String subject, String predicate) {
        StringBuilder where = new StringBuilder();
        if (dimensions.length > 0) {
            where.append(subject);
            // TODO Should we support multiple selections from a given facet?
            for (int i=0; i < dimensions.length; i++) {
                if (i > 0) {
                    where.append(" ;\n");
                }
                // TODO use parameters
                where.append(" ").append(predicate).append(" ").append(dimensions[i]);
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

        String[] dimensions = nullToEmpty(request.getParameterValues("dimension"));
        String[] datasets = nullToEmpty(request.getParameterValues("dataset"));
        int limit = Math.min(getInt(request, "limit", 200), 1000);
        int offset = getInt(request, "offset", 0);

        RDFConnection conn = repository.openConnection();
        CloseableIterator<Map<String,NODE>> iter = null;
        CloseableIterator<STMT> stmts = null;
        try {
            List<JSONObject> items = null;
            StringBuilder sparql;
            SPARQLQuery query;
            Map<String, NODE> row;
            Collection<String> availableDimensions;
            Collection<String> availableDatasets;

            // NAMESPACES
            Map<String,String> namespaces = getNamespaces(conn);
            StringBuilder sparqlNamespaces = new StringBuilder();
            for (Map.Entry<String, String> entry : namespaces.entrySet()) {
                sparqlNamespaces.append("PREFIX ");
                sparqlNamespaces.append(entry.getValue());
                sparqlNamespaces.append(": <");
                sparqlNamespaces.append(entry.getKey());
                sparqlNamespaces.append(">\n");
            }

            int restrictionCount = datasets.length + dimensions.length;
            if (restrictionCount < 3) {
                availableDatasets = new LinkedHashSet<String>();
                availableDimensions = new LinkedHashSet<String>();

                // Find available dimensions via dataset definitions

                StringBuilder filter = new StringBuilder();
                if (datasets.length > 0) {
                    filter.append("FILTER (?dataset = ").append(datasets[0]).append("\n");
                }
                for (int i=0; i < dimensions.length; i++) {
                    if (filter.length() == 0) {
                        filter.append("FILTER (");
                    } else {
                        filter.append(" && ");
                    }

                    stmts = conn.findStatements(getUID(dimensions[i], namespaces), RDF.type, null, null, false);
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
                .append("SELECT ?dataset ?dimension ?value\nWHERE {\n")
                .append(where)
                .append("?dataset stat:datasetDimension ?dimension .\n");

                if (filter.length() > 0) {
                    if (dimensions.length > 0) {
                        sparql.append("?dimension rdf:type ?dimensionType .\n");
                    }
                    sparql.append(filter).append(")\n}\n");
                } else {
                    sparql.append("}\n");
                }

                query = conn.createQuery(QueryLanguage.SPARQL, sparql.toString());
                iter = query.getTuples();
                while (iter.hasNext()) {
                    row = iter.next();
                    availableDatasets.add(getPrefixed((UID) row.get("dataset"), namespaces));
                    availableDimensions.add(getPrefixed((UID) row.get("dimension"), namespaces));
                }
                iter.close();
            } else {
                items = new ArrayList<JSONObject>(limit);
                StringBuilder where = whereDimensions(dimensions, "?item", "scv:dimension");

                if (datasets.length > 0) {
                    // TODO Should we support multiple dataset selections?
                    // TODO use parameters
                    where.append("?item scv:dataset " + datasets[0] + " .\n");
                }

                // Distinct items
                sparql = new StringBuilder()
                .append(sparqlNamespaces)
                .append("SELECT ?item ?dataset ?value\nWHERE {\n")
                .append(where)
                .append("?item scv:dataset ?dataset ; rdf:value ?value.\n}\nLIMIT ")
                .append(limit)
                .append("\nOFFSET ")
                .append(offset);

                query = conn.createQuery(QueryLanguage.SPARQL, sparql.toString());
                iter = query.getTuples();
                while (iter.hasNext()) {
                    row = iter.next();
                    UID id = (UID) row.get("item");
                    UID dataset = (UID) row.get("dataset");
                    LIT value = (LIT) row.get("value");

                    JSONObject json = new JSONObject();
                    json.put("value", value.getValue());
                    json.put("dataset", getPrefixed(dataset, namespaces));

                    stmts = conn.findStatements(id, SCV.dimension, null, dataset, false);
                    while (stmts.hasNext()) {
                        STMT stmt = stmts.next();
                        json.accumulate("dimensions", getPrefixed((UID) stmt.getObject(), namespaces));
                    }
                    stmts.close();
                    items.add(json);
                }
                iter.close();


                // AVAILABLE DIMENSIONS
                availableDimensions = new ArrayList<String>();

                sparql = new StringBuilder()
                .append(sparqlNamespaces)
                .append("SELECT distinct ?dimension\nWHERE {\n")
                .append(where)
                .append("?item scv:dimension ?dimension .\n}\n");

                query = conn.createQuery(QueryLanguage.SPARQL, sparql.toString());
                iter = query.getTuples();
                while (iter.hasNext()) {
                    row = iter.next();
                    UID id = (UID) row.get("dimension");
                    availableDimensions.add(getPrefixed(id, namespaces));
                }
                iter.close();

                // AVAILABLE DATASETS
                availableDatasets = new ArrayList<String>();

                sparql = new StringBuilder()
                .append(sparqlNamespaces)
                .append("SELECT distinct ?dataset\nWHERE {\n")
                .append(where)
                .append("?item scv:dataset ?dataset .\n}\n");

                query = conn.createQuery(QueryLanguage.SPARQL, sparql.toString());
                iter = query.getTuples();
                while (iter.hasNext()) {
                    row = iter.next();
                    UID id = (UID) row.get("dataset");
                    availableDatasets.add(getPrefixed(id, namespaces));
                }
                iter.close();
            }

            JSONObject result = new JSONObject();
            if (items != null) {
                result.put("items", items);
            }
            result.put("dimensions", availableDimensions);
            result.put("datasets", availableDatasets);
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

    // TODO: Join implementation with FacetedSearchServlet
    private String getPrefixed(UID uri, Map<String, String> namespaces) {
        String prefix = namespaces.get(uri.getNamespace());
        if (prefix == null) {
            throw new IllegalArgumentException("Unknown namespace: " + uri);
        }
        return prefix + ":" + uri.getLocalName();
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

    // TODO: Join implementation with FacetedSearchServlet
    private Map<String,String> getNamespaces(RDFConnection conn) {
        SPARQLQuery query = conn.createQuery(QueryLanguage.SPARQL, 
                "SELECT ?ns ?prefix\n" +
                "WHERE {\n" +
                "?ns <http://data.mysema.com/schemas/meta#nsPrefix> ?prefix .\n" +
                "}"
        );
        // Order by descending string length of NS -> when applying namespaces to output longest match comes first
        CloseableIterator<Map<String,NODE>> iter = query.getTuples();
        Map<String,String> namespaces = new HashMap<String, String>(32);
        try {
            while (iter.hasNext()) {
                Map<String, NODE> entry = iter.next();
                namespaces.put(((UID) entry.get("ns")).getId(), ((LIT) entry.get("prefix")).getValue());
            }
        } finally {
            iter.close();
        }
        return namespaces;
    }

}
