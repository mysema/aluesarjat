package fi.aluesarjat.prototype.guice;

import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.mysema.rdfbean.model.Repository;


public class MemoryStoreRDFBeanModuleTest {

    @Test
    public void Create(){
        Injector injector = Guice.createInjector(new MemoryStoreRDFBeanModule());
        Repository repository = injector.getInstance(Repository.class);
        repository.close();
    }

}
