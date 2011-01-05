package fi.aluesarjat.prototype;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.model.LIT;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.QueryLanguage;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.SPARQLQuery;
import com.mysema.rdfbean.model.UID;

public abstract class AbstractSPARQLServlet extends HttpServlet {
    
    private static final long serialVersionUID = -7945359392801951785L;

    protected String getPrefixed(UID uri, Map<String, String> namespaces) {
        String prefix = namespaces.get(uri.getNamespace());
        if (prefix == null) {
            throw new IllegalArgumentException("Unknown namespace: " + uri.getNamespace());
        }
        return prefix + ":" + uri.getLocalName();
    }

    protected Map<String,String> getNamespaces(RDFConnection conn) {
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

    protected StringBuilder getSPARQLNamespaces(Map<String, String> namespaces) {
        StringBuilder sparqlNamespaces = new StringBuilder();
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            sparqlNamespaces.append("PREFIX ");
            sparqlNamespaces.append(entry.getValue());
            sparqlNamespaces.append(": <");
            sparqlNamespaces.append(entry.getKey());
            sparqlNamespaces.append(">\n");
        }
        return sparqlNamespaces;
    }
    
    protected NODE getSingleResult(RDFConnection connection, String queryString, Map<String,NODE> bindings) {
        SPARQLQuery query = connection.createQuery(QueryLanguage.SPARQL, queryString);
        for (Map.Entry<String, NODE> binding : bindings.entrySet()){
            query.setBinding(binding.getKey(), binding.getValue());
        }
        CloseableIterator<Map<String,NODE>> result = query.getTuples();
        try{
            if (result.hasNext()){
                return result.next().values().iterator().next();
            }else{
                return null;
            }
        }finally{
            result.close();
        }
    }
    

}
