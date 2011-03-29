package fi.aluesarjat.prototype;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
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

        boolean includeItems = includes.contains("items");
        boolean includeValues = includes.contains("values");
        SearchResults searchResults = searchService.search(restrictions, includeItems, limit, offset, includeValues);

        String jsonpCallback = request.getParameter("callback");
        if (jsonpCallback != null){
            response.getWriter().write(jsonpCallback + "(");
        }

        JsonGenerator generator = jsonFactory.createJsonGenerator(response.getWriter());
        generator.writeStartObject();

        if (searchResults.getAvailableValues() != null){
            addAvailableValues(namespaces, searchResults, generator);
        }

        if (searchResults.getHeaders() != null) {
            addHeaders(namespaces, searchResults, generator);
        }

        if (searchResults.getItems() != null) {
            addItems(namespaces, searchResults, generator);
        }

        // hasMoreResults
        generator.writeBooleanField("hasMoreResults", searchResults.isHasMoreResults());
        generator.writeEndObject();
        generator.flush();

        if (jsonpCallback != null){
            response.getWriter().write(")");
        }
        response.getWriter().flush();
    }

    private void addItems(Map<UID, String> namespaces,
            SearchResults searchResults, JsonGenerator generator)
            throws IOException, JsonGenerationException {
        // items
        generator.writeFieldName("items");
        generator.writeStartArray();
        for (Item item : searchResults.getItems()){
            generator.writeStartObject();
            generator.writeFieldName("values");
            generator.writeStartArray();
            for (UID uid : item.getValues()){
                if (uid != null) {
                    generator.writeString(getPrefixed(uid, namespaces));
                }else{
                    generator.writeNull();
                }
            }
            generator.writeEndArray();
            generator.writeStringField("value", item.getValue());
            generator.writeEndObject();
        }
        generator.writeEndArray();
    }

    private void addHeaders(Map<UID, String> namespaces,
            SearchResults searchResults, JsonGenerator generator)
            throws IOException, JsonGenerationException {
        // headers
        generator.writeFieldName("headers");
        generator.writeStartArray();
        for (UID header : searchResults.getHeaders()){
            generator.writeString(getPrefixed(header, namespaces));
        }
        generator.writeEndArray();
    }

    private void addAvailableValues(Map<UID, String> namespaces,
            SearchResults searchResults, JsonGenerator generator)
            throws IOException, JsonGenerationException {
        // availableValues
        generator.writeFieldName("availableValues");
        generator.writeStartObject();
        for (UID value : searchResults.getAvailableValues()){
            generator.writeBooleanField(getPrefixed(value, namespaces), true);
        }
        generator.writeEndObject();
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
