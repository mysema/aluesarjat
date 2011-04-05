package fi.aluesarjat.prototype.guice;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mysema.rdfbean.model.Format;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.RDFConnectionCallback;
import com.mysema.rdfbean.model.UID;
import com.mysema.rdfbean.sesame.MemoryRepository;

public class ModuleUtilsTest {

    private MemoryRepository repository;

    @Before
    public void setUp(){
        repository = new MemoryRepository();
        repository.setSources(ModuleUtils.getSources("http://www.aluesarjat.fi/data/"));
        repository.initialize();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        repository.export(Format.TURTLE, null, out);
        try {
            System.err.println(new String(out.toByteArray(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @After
    public void tearDown(){
        repository.close();
    }

    @Test
    public void GetSources() {
        repository.execute(new RDFConnectionCallback<Void>(){
            @Override
            public Void doInConnection(RDFConnection connection) throws IOException {
                assertTrue(connection.exists(null, null, new UID("http://www.aluesarjat.fi/data/dimensions/Alue"), null, false));
                assertFalse(connection.exists(null, null, new UID("http://localhost:8080/data/dimensions/Alue"), null, false));
                return null;
            }
        });
    }

}
