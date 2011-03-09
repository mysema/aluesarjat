package fi.aluesarjat.prototype;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.mysema.rdfbean.model.Format;
import com.mysema.rdfbean.model.QueryLanguage;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.SPARQLQuery;
import com.mysema.rdfbean.model.UID;

public class SubjectGraphServlet extends HttpServlet{

    private static final long serialVersionUID = 9007924911100922605L;

    private final Repository repository;

    public SubjectGraphServlet(Repository repository) {
        this.repository = repository;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;
        String subject = request.getRequestURL().toString();
        RDFConnection connection = repository.openConnection();
        try{
            SPARQLQuery query = connection.createQuery(QueryLanguage.SPARQL,"CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
            query.setBinding("s", new UID(subject));
            String contentType = getAcceptedType(request, Format.RDFXML);
            response.setContentType(contentType);
            query.streamTriples(response.getWriter(), contentType);
        }finally{
            connection.close();
        }

    }

    // TODO : make sure this works correctly
    private String getAcceptedType(HttpServletRequest request, Format defaultFormat){
        String accept = request.getHeader("Accept");
        if (!StringUtils.isEmpty(accept)){
            if (accept.contains(",")){
                accept = accept.substring(0, accept.indexOf(','));
            }
            return Format.getFormat(accept, defaultFormat).getMimetype();
        }else{
            return defaultFormat.getMimetype();
        }
    }


}
