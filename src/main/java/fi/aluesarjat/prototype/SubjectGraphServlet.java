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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.mysema.rdfbean.model.Format;
import com.mysema.rdfbean.model.QueryLanguage;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.SPARQLQuery;
import com.mysema.rdfbean.model.UID;

public class SubjectGraphServlet extends HttpServlet {

    private static final long serialVersionUID = 9007924911100922605L;

    private static final long LAST_MODIFIED = System.currentTimeMillis() / 1000 * 1000;

    private final Repository repository;

    public SubjectGraphServlet(Repository repository) {
        this.repository = repository;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;

        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        if (ifModifiedSince >= LAST_MODIFIED) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        String subject = request.getRequestURL().toString();
        RDFConnection connection = repository.openConnection();
        try {
            SPARQLQuery query = connection.createQuery(QueryLanguage.SPARQL, "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
            query.setBinding("s", new UID(subject));
            String contentType = getAcceptedType(request, Format.RDFXML);
            response.setDateHeader("Last-Modified", System.currentTimeMillis());
            response.setContentType(contentType);
            query.streamTriples(response.getWriter(), contentType);
        } finally {
            connection.close();
        }

    }

    // TODO : make sure this works correctly
    private String getAcceptedType(HttpServletRequest request, Format defaultFormat) {
        String accept = request.getHeader("Accept");
        if (!StringUtils.isEmpty(accept)) {
            if (accept.contains(",")) {
                accept = accept.substring(0, accept.indexOf(','));
            }
            return Format.getFormat(accept, defaultFormat).getMimetype();
        } else {
            return defaultFormat.getMimetype();
        }
    }


}
