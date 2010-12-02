package fi.aluesarjat.prototype;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.result.ModelResult;
import org.openrdf.store.StoreException;

import virtuoso.sesame3.driver.VirtuosoRepository;

public class VirtuosoClearDatabase {

    public static void main(String[] args) throws StoreException{
        Repository repository = new VirtuosoRepository("localhost:1111", "dba", "dba");
        repository.initialize();
        RepositoryConnection conn = repository.getConnection();
        try{

            conn.removeMatch((Resource)null, (URI)null, (Value)null);
            conn.removeMatch((Resource)null, (URI)null, (Value)null, (Resource)null);

            // remove the rest
            conn.begin();
            try{
                ModelResult results = conn.match(null, null, null, false);
                while (results.hasNext()){
                    Statement stmt = results.next();
                    System.err.println(stmt);
                    conn.remove(stmt, stmt.getContext());
                }
                results.close();    
                conn.commit();
            }catch(Exception e){
                conn.rollback();
                throw new RuntimeException(e);
            }            

        }finally{
            conn.close();
            repository.shutDown();
        }
    }


}
