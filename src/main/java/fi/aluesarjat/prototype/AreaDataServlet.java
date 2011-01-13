package fi.aluesarjat.prototype;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.QueryLanguage;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.SPARQLQuery;
import com.mysema.rdfbean.model.UID;

public class AreaDataServlet extends AbstractSPARQLServlet{
    
    private static final Logger log = LoggerFactory.getLogger(AbstractFacetSearchServlet.class);

    private static final long serialVersionUID = 1307767271709547227L;
    
    private final Repository repository;
    
    private final String baseURI;
    
    private final UID hel1, hel2, van1, van2, esp1, esp2;
    
    public AreaDataServlet(Repository repository, String baseURI) {
        this.repository = repository;
        this.baseURI = baseURI;
        hel1 = new UID(baseURI + "datasets#", "A02S_HKI_Vakiluku1962");
        esp1 = new UID(baseURI + "datasets#", "B02S_ESP_Vakiluku1975");
        van1 = new UID(baseURI + "datasets#", "C02S_VAN_Vakiluku1971");        
        hel2 = new UID(baseURI + "datasets#", "A01HKI_Astuot_hper_rahoitus_talotyyppi");
        esp2 = new UID(baseURI + "datasets#", "B01ESP_Asuntotuotanto_1997_");
        van2 = new UID(baseURI + "datasets#", "C01VAN_Astuot_hper_rahoitus_talotyyppi");
        
    }
    
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setDateHeader("Last-Modified", System.currentTimeMillis());
        String areaId= request.getParameter("area");
        Map<String,NODE> bindings = new HashMap<String,NODE>();
        bindings.put("area", new UID(baseURI + "dimensions/Alue#", areaId));
        if (areaId.startsWith("_049")){
            bindings.put("g", esp1);
        }else if (areaId.startsWith("_092")){
            bindings.put("g", van1);
        }else{
            bindings.put("g", hel1);    
        }
        
        RDFConnection connection = repository.openConnection();        
        try{
            JSONArray result = new JSONArray();
            //result.put("id", areaId);
            
            // väkiluku
            StringBuilder query = new StringBuilder();
            query.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
            query.append("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n");
            query.append("PREFIX dimension: <"+baseURI+"dimensions/>\n");
            query.append("PREFIX scv: <http://purl.org/NET/scovo#> \n");
            query.append("PREFIX vuosi: <"+baseURI+"dimensions/Vuosi#>\n");
            
            query.append("SELECT ?ikl ?val \nWHERE { \n");
            query.append(" GRAPH ?g { \n");
            query.append("  ?item scv:dimension vuosi:_2009 , ?area , ?ik ; rdf:value ?val . \n");           
            query.append(" } \n");
            query.append(" ?ik rdf:type dimension:Ikäryhmä ; dc:title ?ikl \n");
            query.append("}");
            if (log.isInfoEnabled()){
                log.info(query.toString());    
            }            
            SPARQLQuery sparqlQuery = connection.createQuery(QueryLanguage.SPARQL, query.toString());
            sparqlQuery.setBinding("area", bindings.get("area"));
            sparqlQuery.setBinding("g", bindings.get("g"));
            long start = System.currentTimeMillis();
            CloseableIterator<Map<String,NODE>> sparqlResult = sparqlQuery.getTuples();
//            System.err.println((System.currentTimeMillis()-start)+"ms");
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
            
//            if (areaId.startsWith("_049")){
//                bindings.put("g", esp2);
//            }else if (areaId.startsWith("_092")){
//                bindings.put("g", van2);
//            }else{
//                bindings.put("g", hel2);    
//            }
//            
//            // asuntotuotanto (lukumäärä)
//            query = new StringBuilder();
//            query.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
//            query.append("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n");
//            query.append("PREFIX dimension: <"+baseURI+"dimensions/>\n");
//            query.append("PREFIX yksikkö: <"+baseURI+"dimensions/Yksikkö#>\n");
//            query.append("PREFIX huoneistotyyppi: <"+baseURI+"dimensions/Huoneistotyyppi#>\n");
//            query.append("PREFIX hallintaperuste: <"+baseURI+"dimensions/Hallintaperuste#>\n");
//            query.append("PREFIX vuosi: <"+baseURI+"dimensions/Vuosi#>\n");
//            query.append("PREFIX rahoitusmuoto: <"+baseURI+"dimensions/Rahoitusmuoto#>\n");
//            query.append("PREFIX talotyyppi: <"+baseURI+"dimensions/Talotyyppi#>\n");
//            query.append("PREFIX scv: <http://purl.org/NET/scovo#> \n");
//            
//            query.append("SELECT ?val \nWHERE {\n");
//            query.append(" GRAPH ?g {\n");
//            query.append("  ?item scv:dimension ?area , yksikkö:Asuntojen_lukumäärä , hallintaperuste:Asunnot_yhteensä , " +
//                         "  rahoitusmuoto:Yhteensä , vuosi:_2009, talotyyppi:Yhteensä, huoneistotyyppi:Yhteensä ; \n");
//            query.append("  rdf:value ?val . \n");
//            query.append(" }\n");
//            query.append("}");        
//            if (log.isInfoEnabled()){
//                log.info(query.toString());    
//            }            
//            start = System.currentTimeMillis();
//            NODE node = getSingleResult(connection, query.toString(), bindings);
//            System.err.println((System.currentTimeMillis()-start)+"ms");
//            if (node != null){
//                JSONObject entry = new JSONObject();
//                entry.put("label", "Asuntotuotanto (lkm)");
//                entry.put("value", node.getValue());                
//                result.add(entry);
//            }       
//            
//            // asuntotuotanto (pinta-ala)
//            query = new StringBuilder();
//            query.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
//            query.append("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n");
//            query.append("PREFIX dimension: <"+baseURI+"dimensions/>\n");
//            query.append("PREFIX yksikkö: <"+baseURI+"dimensions/Yksikkö#>\n");
//            query.append("PREFIX huoneistotyyppi: <"+baseURI+"dimensions/Huoneistotyyppi#>\n");
//            query.append("PREFIX hallintaperuste: <"+baseURI+"dimensions/Hallintaperuste#>\n");
//            query.append("PREFIX vuosi: <"+baseURI+"dimensions/Vuosi#>\n");
//            query.append("PREFIX rahoitusmuoto: <"+baseURI+"dimensions/Rahoitusmuoto#>\n");
//            query.append("PREFIX talotyyppi: <"+baseURI+"dimensions/Talotyyppi#>\n");
//            query.append("PREFIX scv: <http://purl.org/NET/scovo#> \n");
//            
//            query.append("SELECT ?val \nWHERE {\n");
//            query.append(" GRAPH ?g {\n");
//            query.append("  ?item scv:dimension ?area , yksikkö:Asuntojen_pinta-ala_m2 , hallintaperuste:Asunnot_yhteensä , " +
//                         "  rahoitusmuoto:Yhteensä , vuosi:_2009, talotyyppi:Yhteensä, huoneistotyyppi:Yhteensä ; \n");
//            query.append("  rdf:value ?val . \n");
//            query.append(" }\n");
//            query.append("}");            
//            if (log.isInfoEnabled()){
//                log.info(query.toString());    
//            }            
//            start = System.currentTimeMillis();
//            node = getSingleResult(connection, query.toString(), bindings);
//            System.err.println((System.currentTimeMillis()-start)+"ms");
//            if (node != null){
//                JSONObject entry = new JSONObject();                
//                entry.put("label", "Asuntotuotanto (pinta-ala)");
//                entry.put("value", node.getValue());
//                result.add(entry);
//            }   
            
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
