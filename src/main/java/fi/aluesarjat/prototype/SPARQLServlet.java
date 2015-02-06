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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mysema.rdfbean.model.Repository;

public class SPARQLServlet extends com.mysema.rdfbean.sparql.SPARQLServlet {

    public SPARQLServlet() {
        super();
    }

    public SPARQLServlet(Repository repository, Integer limit,
            Integer maxQueryTime) {
        super(repository, limit, maxQueryTime);
    }

    public SPARQLServlet(Repository repository, Integer limit) {
        super(repository, limit);
    }

    public SPARQLServlet(Repository repository) {
        super(repository);
    }

    private static final long serialVersionUID = 1L;

    protected void handleRequest(HttpServletRequest request,
            HttpServletResponse response, String queryString)
            throws IOException {
        String inferenceParam = request.getParameter("inference");
        boolean inference = inferenceParam != null && "true".equalsIgnoreCase(inferenceParam);
        
        if (inference) {
            queryString = DataService.DIMENSION_INFERENCE + queryString;
        }
        
        super.handleRequest(request, response, queryString);
    }
    
}
