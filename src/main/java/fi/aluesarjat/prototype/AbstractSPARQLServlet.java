/*
* Copyright 2013 Mysema Ltd
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
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
