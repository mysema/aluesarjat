package fi.aluesarjat.prototype;

import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.result.ModelResult;
import org.openrdf.store.StoreException;

import virtuoso.sesame3.driver.VirtuosoRepository;

import com.mysema.stat.scovo.SCV;

public class VirtuosoClearDatabase {

    public static void main(String[] args) throws StoreException{
        Repository repository = new VirtuosoRepository("localhost:1111", "dba", "dba");
        String baseURI = "http://localhost:8080/rdf/";
        repository.initialize();
        RepositoryConnection conn = repository.getConnection();
        ValueFactory vf = conn.getValueFactory();
        Set<URI> contexts = new HashSet<URI>();
        try{
            // get dimensions
            contexts.add(vf.createURI(baseURI + "dimensions"));
            collect(conn, vf.createURI(SCV.Dimension.getId()), contexts);

            // get datasets
            contexts.add(vf.createURI(baseURI + "datasets"));
            collect(conn, vf.createURI(SCV.Dataset.getId()), contexts);

            // remove contexts
            for (URI context : contexts){
                System.err.println("removing context " + context.stringValue());
                conn.removeMatch((Resource)null, (URI)null, (Value)null, context);
            }

            conn.removeMatch((Resource)null, (URI)null, (Value)null);

        }finally{
            conn.close();
            repository.shutDown();
        }
    }

    private static void collect(RepositoryConnection conn, URI type, Set<URI> contexts) throws StoreException{
        ModelResult result = conn.match(null, RDF.TYPE, type, false);
        try{
            for (Statement stmt : result.asList()){
                contexts.add((URI)stmt.getSubject());
            }
        }finally{
            result.close();
        }
    }
}
