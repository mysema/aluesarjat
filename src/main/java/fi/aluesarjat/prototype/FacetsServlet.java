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
import java.util.Collection;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

import com.mysema.rdfbean.model.UID;


public class FacetsServlet extends AbstractSPARQLServlet {

    private static final long serialVersionUID = 2149808648205848159L;

    private static final long LAST_MODIFIED = System.currentTimeMillis() / 1000 * 1000;

    private final JsonFactory jsonFactory = new JsonFactory();

    private final SearchService searchService;

    public FacetsServlet(SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse)res;

        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        if (ifModifiedSince >= LAST_MODIFIED) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        response.setDateHeader("Last-Modified", System.currentTimeMillis());
        response.setHeader("Cache-Control", "max-age=86400");
        if (request.getParameter("callback") != null) {
            response.setContentType("text/javascript");   
        } else {
            response.setContentType("application/json");    
        }        
        response.setCharacterEncoding("UTF-8");

        Map<UID, String> namespaces = searchService.getNamespaces();
        Collection<Facet> facets = searchService.getFacets();

        JsonGenerator generator = jsonFactory.createJsonGenerator(response.getWriter());

        String jsonpCallback = request.getParameter("callback");
        if (jsonpCallback != null) {
            response.getWriter().write(jsonpCallback + "(");
        }

        generator.writeStartObject();
        generator.writeFieldName("facets");
        generator.writeStartArray();
        for (Facet facet : facets) {
            generator.writeStartObject();
            generator.writeStringField("id", getPrefixed(facet.getId(), namespaces));
            generator.writeStringField("name", facet.getName());
            generator.writeFieldName("values");
            generator.writeStartArray();
            for (Value value : facet.getValues()) {
                generator.writeStartObject();
                generator.writeStringField("id", getPrefixed(value.getId(), namespaces));
                generator.writeStringField("name", value.getName());
                if (value.getDescription() != null) {
                    generator.writeStringField("description", value.getDescription());
                }
                generator.writeEndObject();
            }
            generator.writeEndArray();
            generator.writeEndObject();
        }
        generator.writeEndArray();
        generator.writeEndObject();
        generator.flush();

        if (jsonpCallback != null) {
            response.getWriter().write(")");
        }
        response.getWriter().flush();

    }

}
