package fi.aluesarjat.prototype;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mysema.rdfbean.model.QueryLanguage;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.SPARQLQuery;
import com.mysema.rdfbean.model.io.Format;

/**
 * Dumps the statements of the request url's named graph to the response writer
 * 
 * @author tiwe
 *
 */
public class ContextAccessServlet extends HttpServlet{
    
    private static final long serialVersionUID = 3545610671574978570L;

    private Repository repository;
    
    public ContextAccessServlet(Repository repository) {
        this.repository = repository;
    }
    
    public ContextAccessServlet() {}
        
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;
        String context = request.getRequestURL().toString();
        RDFConnection connection = repository.openConnection();
        try{
            StringBuilder q = new StringBuilder();
            q.append("CONSTRUCT { ?s ?p ?o } \n");
            q.append("FROM    <").append(context).append("> \n");
            q.append("WHERE   { ?s ?p ?o } ");

            SPARQLQuery query = connection.createQuery(QueryLanguage.SPARQL, q.toString());
            String contentType = getAcceptedType(request, Format.RDFXML.getMimetype());
            response.setContentType(contentType);
            query.streamTriples(response.getWriter(), contentType);
        }finally{
            connection.close();
        }        
    }
    
    private String getAcceptedType(HttpServletRequest request, String defaultType){
        String accept = request.getHeader("Accept");
        if (accept != null){
            return accept.contains(",") ? accept.substring(0, accept.indexOf(',')) : accept;
        }else{
            return defaultType;
        }
    }
    
}
