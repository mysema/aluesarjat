package fi.aluesarjat.prototype;

import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.result.TupleResult;
import org.openrdf.store.StoreException;

import virtuoso.sesame3.driver.VirtuosoRepository;

public class VirtuosoQueryTest {
    
    public static void main(String[] args) throws StoreException{
        Repository repository = new VirtuosoRepository("localhost:1111", "dba", "dba");
        repository.initialize();
        RepositoryConnection conn = repository.getConnection();
        try{
            TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, "select ?s ?p ?o where { ?s ?p ?o } limit 5");
            query.setBinding("p", RDFS.LABEL);
            TupleResult result = query.evaluate();
            try{
                while (result.hasNext()){
                    System.out.println(result.asList());
                }    
            }finally{
                result.close();    
            }

        }finally{
            conn.close();
            repository.shutDown();
        }
    }

}
