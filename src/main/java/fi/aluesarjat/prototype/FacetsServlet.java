package fi.aluesarjat.prototype;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.model.LIT;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.QueryLanguage;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.SPARQLQuery;
import com.mysema.rdfbean.model.UID;
import com.mysema.stat.scovo.SCV;

public class FacetsServlet extends HttpServlet {

    private static final long serialVersionUID = 2149808648205848159L;

    private Repository repository;

    public FacetsServlet(Repository repository) {
        this.repository = repository;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
//        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;
        response.setContentType("application/json");

        RDFConnection conn = repository.openConnection();
        CloseableIterator<Map<String,NODE>> iter = null;
        try {
            Map<UID,JSONObject> dimensionTypes = new LinkedHashMap<UID,JSONObject>();

            SPARQLQuery query;
            Map<String, NODE> row;

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
            
            // DIMENSIONS
            query = conn.createQuery(QueryLanguage.SPARQL, 
                    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                    "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                    "PREFIX scv: <http://purl.org/NET/scovo#>\n" +
                    "SELECT ?dimensionType ?dimensionTypeName ?dimension ?dimensionName\n"+
                    "WHERE {\n"+
                    "?dimensionType rdfs:subClassOf scv:Dimension ;\n"+
                    "    dc:title ?dimensionTypeName .\n"+
                    "?dimension rdf:type ?dimensionType ;\n"+
                    "    dc:title ?dimensionName .\n"+
                    "}\n");
            iter = query.getTuples();
            while (iter.hasNext()) {
                row = iter.next();
                UID type = (UID) row.get("dimensionType");
                JSONObject dimensionType = dimensionTypes.get(type);
                if (dimensionType == null) {
                    // Create dimension type
                    dimensionType = new JSONObject();
                    dimensionType.put("id", getPrefixed(type, namespaces));
                    dimensionType.put("name", ((LIT) row.get("dimensionTypeName")).getValue());
                    dimensionTypes.put(type, dimensionType);
                }
                // Create dimension
                JSONObject dimension = new JSONObject();
                dimension.put("id", getPrefixed((UID) row.get("dimension"), namespaces));
                dimension.put("name", ((LIT) row.get("dimensionName")).getValue());

                dimensionType.accumulate("values", dimension);
            }
            iter.close();
            
            // DATASETS
            JSONObject dimensionType = new JSONObject();
            dimensionType.put("id", "scv:Dataset");
            dimensionType.put("name", "Tilasto");
            dimensionTypes.put(SCV.Dataset, dimensionType);

            query = conn.createQuery(QueryLanguage.SPARQL, 
                    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                    "PREFIX scv: <http://purl.org/NET/scovo#>\n" +
                    "SELECT ?dataset ?datasetName\n" +
                    "WHERE {\n" +
                    "?dataset rdf:type scv:Dataset ; dc:title ?datasetName .\n" +
                    "}");
            iter = query.getTuples();
            while (iter.hasNext()) {
                row = iter.next();
                JSONObject dataset = new JSONObject();
                dataset.put("id", getPrefixed((UID) row.get("dataset"), namespaces));
                dataset.put("name", ((LIT) row.get("datasetName")).getValue());
                
                dimensionType.accumulate("values", dataset);
            }
            
            JSONObject result = new JSONObject();
            result.put("facets", dimensionTypes.values());
            Writer out = response.getWriter();
            result.write(out);
            out.flush();
        } finally {
            if (iter != null) {
                iter.close();
            }
            conn.close();
        }
    }

    // TODO: Join implementation with FacetedSearchServlet
    private String getPrefixed(UID uri, Map<String, String> namespaces) {
        String prefix = namespaces.get(uri.getNamespace());
        if (prefix == null) {
            throw new IllegalArgumentException("Unknown namespace: " + uri);
        }
        return prefix + ":" + uri.getLocalName();
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
