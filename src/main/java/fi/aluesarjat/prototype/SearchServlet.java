package fi.aluesarjat.prototype;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

import com.google.inject.internal.Maps;
import com.mysema.rdfbean.model.UID;

public class SearchServlet extends AbstractSPARQLServlet {

//    private static final Logger log = LoggerFactory.getLogger(SearchServlet.class);

    private static final long LAST_MODIFIED = System.currentTimeMillis() / 1000 * 1000;

    public static final int EXPORT_LIMIT = 100000;
    
    private static final long serialVersionUID = 2149808648205848159L;

    private static final int SEARCH_DEFAULT_LIMIT = 200;

    private static final int SEARCH_MAX_LIMIT = 1000;
    
    private static final String CSV_DELIM = ";";
    
    private static final String CSV_QUOTE = "\"";
    
    private static final Pattern DECIMAL = Pattern.compile("\\d+\\.\\d+");

    private static final String[] EMPTY = new String[0];

    private static enum Format { 
        JSON("application/json", "UTF-8"), CSV("text/csv", "ISO-8859-15"); 
        final String contentType;
        final String encoding;
        private Format(String contentType, String encoding) {
            this.contentType = contentType;
            this.encoding = encoding;
        }
        public String getContentType() {
            return contentType;
        }
        public String getCharacterEncoding() {
            return encoding;
        }
    };

    private final JsonFactory jsonFactory = new JsonFactory();

    private final SearchService searchService;
    
    private final Map<UID, String> labels = Maps.newHashMap();

    public SearchServlet(SearchService searchService) {
        this.searchService = searchService;
        for (Facet facet : searchService.getFacets()) {
            labels.put(facet.getId(), facet.getName());
            for (Value value : facet.getValues()) {
                labels.put(value.getId(), value.getName());
            }
        }
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;

        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        if (ifModifiedSince >= LAST_MODIFIED){
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
        Format format = getFormat(request);

        response.setCharacterEncoding(format.getCharacterEncoding());
        response.setContentType(format.getContentType());
        response.setDateHeader("Last-Modified", System.currentTimeMillis());
        response.setHeader("Cache-Control", "max-age=86400");
        

        int limit = Math.min(getInt(request, "limit", SEARCH_DEFAULT_LIMIT), SEARCH_MAX_LIMIT);
        int offset = getInt(request, "offset", 0);

        Map<UID, String> namespaces = searchService.getNamespaces();
        Set<String> includes = getIncludes(request.getParameterValues("include"));
        Set<UID> restrictions = new HashSet<UID>();
        for (String value : nullToEmpty(request.getParameterValues("value"))) {
            restrictions.add(getUID(value, namespaces));
        }

        boolean includeItems = includes.contains("items");
        boolean includeValues = includes.contains("values");

        if (Format.JSON.equals(format)) {
            SearchResults searchResults = searchService.search(restrictions, includeItems, limit, offset, includeValues);
            serviceJson(searchResults, namespaces, request, response);
        } else if (Format.CSV.equals(format)) {
            SearchResults searchResults = searchService.search(restrictions, true, EXPORT_LIMIT, 0, false);
            serviceCSV(searchResults, namespaces, request, response);
        }
    }
    
    private void serviceCSV(SearchResults searchResults,
            Map<UID, String> namespaces, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();

        List<UID> headers = searchResults.getHeaders();
        if (headers != null) {
            boolean first = true;
            for (UID header : headers) {
                if (first) {
                    first = false; 
                } else {
                    out.append(CSV_DELIM);
                }
                out.append(labels.get(header));
            }
            out.append(CSV_DELIM);
            out.append("Value");
            out.append('\n');
            List<Item> items = searchResults.getItems();
            if (items != null) {
                for (Item item : items) {
                    first = true;
                    for (UID value : item.getValues()) {
                        if (first) {
                            first = false; 
                        } else {
                            out.append(CSV_DELIM);
                        }
                        if (value != null) {
                            String label = labels.get(value);
                            if (0 <= label.indexOf(CSV_DELIM)) {
                                label = CSV_QUOTE + label + CSV_QUOTE;
                            }
                            out.append(label);
                        }
                    }
                    out.append(CSV_DELIM);
                    out.append(getValue(item.getValue()));
                    out.append('\n');
                }
            }
        }
    }
    
    private String getValue(String val) {
        if (DECIMAL.matcher(val).matches()) {
            return val.replace('.', ',');
        } else {
            return val;
        }
    }

    private void serviceJson(SearchResults searchResults, Map<UID, String> namespaces, HttpServletRequest request, HttpServletResponse response) throws IOException {
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
        response.getWriter().flush();    }

    private Format getFormat(HttpServletRequest req) {
        String format = req.getParameter("format");
        String accept = req.getHeader("Accept");
        
        if ("csv".equalsIgnoreCase(format) || "text/csv".equalsIgnoreCase(accept)){
            return Format.CSV;
        } else {
            return Format.JSON;
        }
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
