package fi.aluesarjat.prototype;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.UID;
import com.mysema.stat.scovo.SCV;

public class FacetsServlet extends AbstractFacetSearchServlet {

    private static final long serialVersionUID = 2149808648205848159L;

    private final Repository repository;

    public FacetsServlet(Repository repository) {
        this.repository = repository;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse)res;
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        RDFConnection conn = repository.openConnection();
        try {
            Map<UID,JSONObject> dimensionTypes = new LinkedHashMap<UID,JSONObject>();

            // NAMESPACES
            Map<String,String> namespaces = getNamespaces(conn);

            // DIMENSIONS
            addFacets(conn, "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                    "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                    "PREFIX scv: <http://purl.org/NET/scovo#>\n" +
                    "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
                    "SELECT ?dimensionType ?dimensionTypeName ?dimension ?dimensionDescription ?dimensionName ?parent\n"+
                    "WHERE {\n"+
                    "?dimensionType rdfs:subClassOf scv:Dimension ; dc:title ?dimensionTypeName .\n"+
                    "?dimension rdf:type ?dimensionType ; dc:title ?dimensionName .\n"+
                    "OPTIONAL { ?dimension dc:description ?dimensionDescription } ." +
                    "OPTIONAL { ?dimension skos:broader ?parent } ." +
                    "}\nORDER BY ?dimensionName", namespaces, dimensionTypes, null);

            // DATASETS
            addFacets(conn, "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                    "PREFIX scv: <http://purl.org/NET/scovo#>\n" +
                    "PREFIX stat: <http://data.mysema.com/schemas/stat#>\n" +
                    "SELECT ?dimension ?dimensionName ?dimensionDescription\n" +
                    "WHERE {\n" +
                    "?dimension rdf:type scv:Dataset ; dc:title ?dimensionName .\n" +
                    "OPTIONAL { ?dimension dc:description ?dimensionDescription } ." +
                    "}\nORDER BY ?dimensionName", namespaces, dimensionTypes, Collections.singletonMap("dimensionType", (NODE) SCV.Dataset));

            JSONObject result = new JSONObject();
            result.put("facets", dimensionTypes.values());
            Writer out = response.getWriter();
            String jsonpCallback = request.getParameter("callback");
            if (jsonpCallback != null){
                out.write(jsonpCallback + "(");
                result.write(out);
                out.write(")");
            }else{
                result.write(out);    
            }            
            out.flush();
        } finally {
            conn.close();
        }
    }

}
