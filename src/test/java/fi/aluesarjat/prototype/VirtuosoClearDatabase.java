package fi.aluesarjat.prototype;

import org.openrdf.store.StoreException;

import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.virtuoso.VirtuosoRepository;

public class VirtuosoClearDatabase {

    public static void main(String[] args) throws StoreException{
        Repository repository = new VirtuosoRepository("localhost:1111", "dba", "dba");
        repository.initialize();
        RDFConnection conn = repository.openConnection();
        try{
            conn.remove(null, null, null, null);

        }finally{
            conn.close();
            repository.close();
        }
    }


}
