package fi.aluesarjat.prototype;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mysema.rdfbean.model.QueryLanguage;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.SPARQLQuery;
import com.mysema.rdfbean.model.io.Format;

@Singleton
public class ContextAccessServlet extends HttpServlet{
    
    private static final long serialVersionUID = 3545610671574978570L;
    
    @Inject
    private Repository repository;
    
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
            // TODO : content negotiation
            response.setContentType(Format.RDFXML.getMimetype());
            query.streamTriples(response.getWriter(), Format.RDFXML.getMimetype());
        }finally{
            connection.close();
        }

        
    }

}
