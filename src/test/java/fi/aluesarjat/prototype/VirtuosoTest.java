package fi.aluesarjat.prototype;

import org.junit.Ignore;
import org.junit.Test;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.store.StoreException;

import virtuoso.sesame3.driver.VirtuosoRepository;

public class VirtuosoTest {

    @Test
    @Ignore
    public void test() throws StoreException, InterruptedException, ClassNotFoundException{
        Repository repository = new VirtuosoRepository("localhost:1111", "dba", "dba");
        repository.initialize();
        RepositoryConnection conn = repository.getConnection();
        conn.close();
        repository.shutDown();
    }

}
