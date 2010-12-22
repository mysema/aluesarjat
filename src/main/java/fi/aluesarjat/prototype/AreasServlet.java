package fi.aluesarjat.prototype;

import java.io.IOException;
import java.io.Writer;
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

public class AreasServlet extends AbstractSPARQLServlet{

    private static final long serialVersionUID = -1216436366722412316L;
    
    private final Repository repository;
    
    public AreasServlet(Repository repository) {
        this.repository = repository;
    }
    
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse)res;
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setDateHeader("Last-Modified", System.currentTimeMillis());
        
        RDFConnection conn = repository.openConnection();
        try{
            Map<String,String> namespaces = getNamespaces(conn);
            StringBuilder query = getSPARQLNamespaces(namespaces);
            query.append("SELECT *\n");
            query.append("WHERE {\n");
            query.append("  ?area geo:polygon ?polygon ; geo:where ?center ; dc:title ?title ; alue:level ?level .\n");
            query.append("}");
            
            JSONArray result = new JSONArray();
            SPARQLQuery sparqlQuery = conn.createQuery(QueryLanguage.SPARQL, query.toString());
            CloseableIterator<Map<String,NODE>> tuplesResult = sparqlQuery.getTuples();
            
            try{
                while (tuplesResult.hasNext()){
                    JSONObject row = new JSONObject();
                    Map<String,NODE> tuples = tuplesResult.next();
                    for (Map.Entry<String, NODE> entry : tuples.entrySet()){
                        if (entry.getValue().isURI()){
                            row.put(entry.getKey(), getPrefixed(entry.getValue().asURI(), namespaces));
                        }else{
                            row.put(entry.getKey(), entry.getValue().getValue());
                        }
                    }
                    result.add(row);
                }    
            }finally{
                tuplesResult.close();
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
            conn.close();
        }
    }

}
