package fi.aluesarjat.prototype;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.QueryLanguage;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.SPARQLQuery;
import com.mysema.rdfbean.model.UID;

public class AreaDataServlet extends AbstractSPARQLServlet{

    private static final long serialVersionUID = 1307767271709547227L;
    
    private static final String QUERY_TEMPLATE = "SELECT ?val \n" +
    	"WHERE {\n" +
        "?item scv:dimension ?area , %s ;\n" +
        "rdf:value ?val . \n" +
        "}";
    
    private final Repository repository;
    
    private final String baseURI;
    
    public AreaDataServlet(Repository repository, String baseURI) {
        this.repository = repository;
        this.baseURI = baseURI;
    }
    
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setDateHeader("Last-Modified", System.currentTimeMillis());
        String areaId= request.getParameter("area");
        Map<String,NODE> bindings = Collections.<String,NODE>singletonMap(
                "area", new UID(baseURI + "dimensions/Alue#", areaId));
        
        RDFConnection connection = repository.openConnection();        
        try{
            JSONArray result = new JSONArray();
            //result.put("id", areaId);
            
            Map<String,String> namespaces = getNamespaces(connection);
            
            // väkiluku
            StringBuilder query = getSPARQLNamespaces(namespaces);
            query.append("SELECT ?ikl ?val \nWHERE {\n");
            query.append("?item scv:dimension vuosi:_2009 , ?area , ?ir ; rdf:value ?val . \n");
            query.append("?ik rdf:type dimension:Ikäryhmä . ?ik dc:title ?ikl \n");
            query.append("}");
            SPARQLQuery sparqlQuery = connection.createQuery(QueryLanguage.SPARQL, query.toString());
            sparqlQuery.setBinding("area", bindings.values().iterator().next());
            CloseableIterator<Map<String,NODE>> sparqlResult = sparqlQuery.getTuples();
            try{
                while (sparqlResult.hasNext()){
                    Map<String,NODE> tuples = sparqlResult.next();
                    JSONObject entry = new JSONObject();
                    entry.put("label", tuples.get("ikl").getValue());
                    entry.put("value", tuples.get("val").getValue());
                    result.add(entry);
                }
            }finally{
                sparqlResult.close();
            }
            
            // asuntotuotanto (lukumäärä)
            query = getSPARQLNamespaces(namespaces);
            query.append(String.format(QUERY_TEMPLATE, "yksikkö:Asuntojen_lukumäärä , hallintaperuste:Asunnot_yhteensä , " +
            		"rahoitusmuoto:Yhteensä , vuosi:_2009, talotyyppi:Yhteensä, huoneistotyyppi:Yhteensä"));
            NODE node = getSingleResult(connection, query.toString(), bindings);
            if (node != null){
                JSONObject entry = new JSONObject();
                entry.put("label", "Asuntotuotanto (lkm)");
                entry.put("value", node.getValue());                
                result.add(entry);
            }       
            
            // asuntotuotanto (pinta-ala)
            query = getSPARQLNamespaces(namespaces);
            query.append(String.format(QUERY_TEMPLATE, "yksikkö:Asuntojen_pinta-ala_m2 , hallintaperuste:Asunnot_yhteensä , " +
                        "rahoitusmuoto:Yhteensä , vuosi:_2009, talotyyppi:Yhteensä, huoneistotyyppi:Yhteensä"));
            node = getSingleResult(connection, query.toString(), bindings);
            if (node != null){
                JSONObject entry = new JSONObject();                
                entry.put("label", "Asuntotuotanto (pinta-ala)");
                entry.put("value", node.getValue());
                result.add(entry);
            }   
            
            Writer out = response.getWriter();
            String jsonpCallback = request.getParameter("jsonp");
            if (jsonpCallback != null){
                out.write(jsonpCallback + "(");
                result.write(out);
                out.write(")");
            }else{
                result.write(out);    
            }
            out.flush();
            
        }finally{
            connection.close();
        }
    }

}
