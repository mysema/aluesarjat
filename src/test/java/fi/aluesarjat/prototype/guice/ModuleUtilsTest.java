package fi.aluesarjat.prototype.guice;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.RDFConnectionCallback;
import com.mysema.rdfbean.model.UID;
import com.mysema.rdfbean.sesame.MemoryRepository;

public class ModuleUtilsTest {

    private MemoryRepository repository;

    @Before
    public void setUp(){
        repository = new MemoryRepository();
        repository.setSources(ModuleUtils.getSources("http://www.aluesarjat.fi/rdf/"));
        repository.initialize();
    }

    @After
    public void tearDown(){
        repository.close();
    }

    @Test
    public void GetSources() {
        repository.execute(new RDFConnectionCallback<Void>(){
            @Override
            public Void execute(RDFConnection connection) throws IOException {
                assertTrue(connection.exists(null, null, new UID("http://www.aluesarjat.fi/rdf/dimensions/Alue"), null, false));
                assertFalse(connection.exists(null, null, new UID("http://localhost:8080/rdf/dimensions/Alue"), null, false));
                return null;
            }
        });
    }

}
