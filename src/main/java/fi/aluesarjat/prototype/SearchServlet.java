package fi.aluesarjat.prototype;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

import com.mysema.rdfbean.model.UID;

public class SearchServlet extends AbstractSPARQLServlet {

//    private static final Logger log = LoggerFactory.getLogger(SearchServlet.class);

    private static final long serialVersionUID = 2149808648205848159L;

    private static final int SPARQL_DEFAULT_LIMIT = 200;

    private static final int SPARQL_MAX_LIMIT = 1000;

    private static final String[] EMPTY = new String[0];

    private final JsonFactory jsonFactory = new JsonFactory();

    private final SearchService searchService;

    public SearchServlet(SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        int limit = Math.min(getInt(request, "limit", SPARQL_DEFAULT_LIMIT), SPARQL_MAX_LIMIT);
        int offset = getInt(request, "offset", 0);

        Map<UID, String> namespaces = searchService.getNamespaces();
        Set<String> includes = getIncludes(request.getParameterValues("include"));
        Set<UID> restrictions = new HashSet<UID>();
        for (String value : nullToEmpty(request.getParameterValues("value"))) {
            restrictions.add(getUID(value, namespaces));
        }
        SearchResults searchResults = searchService.search(restrictions, includes.contains("items"), limit, offset, includes.contains("values"));

        MultiMap<UID, UID> facets = new MultiHashMap<UID, UID>();
        for (UID facet : searchResults.getAvailableValues()){
            facets.put(new UID(facet.ns()), facet);
        }

        String jsonpCallback = request.getParameter("callback");
        if (jsonpCallback != null){
            response.getWriter().write(jsonpCallback + "(");
        }

        JsonGenerator generator = jsonFactory.createJsonGenerator(response.getWriter());
        generator.writeStartObject();

        // facets
        if (!searchResults.getAvailableValues().isEmpty()){
            generator.writeFieldName("facets");
            generator.writeStartArray();
            for (Map.Entry<UID, Collection<UID>> facet : facets.entrySet()){
                generator.writeStartObject();
                generator.writeStringField("id", getPrefixed(facet.getKey(), namespaces));
                generator.writeFieldName("values");
                generator.writeStartArray();
                for (UID value : facet.getValue()){
                    generator.writeString(getPrefixed(value, namespaces));
                }
                generator.writeEndArray();
                generator.writeEndObject();
            }
            generator.writeEndArray();
        }

        // headers
        if (searchResults.getHeaders() != null){
            generator.writeFieldName("headers");
            generator.writeStartArray();
            for (UID header : searchResults.getHeaders()){
                generator.writeString(getPrefixed(header, namespaces));
            }
            generator.writeEndArray();
        }

        // items
        if (searchResults.getItems() != null){
            generator.writeFieldName("items");
            generator.writeStartArray();
            for (Item item : searchResults.getItems()){
                generator.writeStartArray();
                for (UID uid : item.getValues()){
                    generator.writeString(getPrefixed(uid, namespaces));
                }
                generator.writeString(item.getValue());
                generator.writeEndArray();
            }
            generator.writeEndArray();
        }

        // hasMoreResults
        generator.writeBooleanField("hasMoreResults", false); // FIXME
        generator.writeEndObject();
        generator.flush();

        if (jsonpCallback != null){
            response.getWriter().write(")");
        }
        response.getWriter().flush();
    }

    private Set<String> getIncludes(String[] values) {
        String[] parameterValues = values;
        if (parameterValues == null) {
            parameterValues = new String[] {"items", "values"};
        }
        return new HashSet<String>(Arrays.asList(parameterValues));
    }

    private UID getUID(String prefixed, Map<UID, String> namespaces) {
        int i = prefixed.indexOf(':');
        if (i < 0) {
            throw new IllegalArgumentException("Not an URI: " + prefixed);
        }
        String prefix = prefixed.substring(0, i);
        String localName = prefixed.substring(i+1);
        for (Map.Entry<UID, String> entry : namespaces.entrySet()) {
            if (entry.getValue().equals(prefix)) {
                return new UID(entry.getKey().getId(), localName);
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
