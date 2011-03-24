package fi.aluesarjat.prototype;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.QueryLanguage;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.SPARQLQuery;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.model.UID;
import com.mysema.stat.META;

public abstract class AbstractSPARQLServlet extends HttpServlet {

    private static final long serialVersionUID = -7945359392801951785L;

    protected String getPrefixed(UID uri, Map<UID, String> namespaces) {
        String prefix = namespaces.get(new UID(uri.getNamespace()));
        if (prefix == null) {
            throw new IllegalArgumentException("Unknown namespace: " + uri.getNamespace());
        }
        return prefix + ":" + uri.getLocalName();
    }

    protected Map<UID,String> getNamespaces(RDFConnection conn) {
        CloseableIterator<STMT> stmts = conn.findStatements(null, META.nsPrefix, null, null, false);
        Map<UID,String> namespaces = new HashMap<UID, String>(32);
        try{
            while (stmts.hasNext()){
                STMT stmt = stmts.next();
                namespaces.put(stmt.getSubject().asURI(), stmt.getObject().getValue());
            }
        }finally{
            stmts.close();
        }
        return namespaces;
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
