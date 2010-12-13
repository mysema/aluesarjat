package fi.aluesarjat.prototype;

import java.util.Collections;

import org.openrdf.store.StoreException;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.virtuoso.VirtuosoRepository;

public class VirtuosoClearDatabase {

    public static void main(String[] args) throws StoreException{
        Repository repository = new VirtuosoRepository("localhost:1111", "dba", "dba");
        repository.initialize();
        RDFConnection conn = repository.openConnection();
        try{
            conn.remove(null, null, null, null);

            // remove the rest
            CloseableIterator<STMT> results = conn.findStatements(null, null, null, null, false);
            try{
                while (results.hasNext()){
                    STMT stmt = results.next();
                    conn.update(Collections.singleton(stmt), null);
                }    
            }finally{
                results.close();
            }             

        }finally{
            conn.close();
            repository.close();
        }
    }


}
