package fi.aluesarjat.prototype;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.model.RDFConnection;
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
        try {
            while (stmts.hasNext()) {
                STMT stmt = stmts.next();
                namespaces.put(stmt.getSubject().asURI(), stmt.getObject().getValue());
            }
        } finally {
            stmts.close();
        }
        return namespaces;
    }

}
