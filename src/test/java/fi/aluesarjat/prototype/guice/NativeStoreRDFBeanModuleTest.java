package fi.aluesarjat.prototype.guice;

import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.mysema.rdfbean.model.Repository;


public class NativeStoreRDFBeanModuleTest {

    @Test
    public void Create() {
        Injector injector = Guice.createInjector(new NativeStoreRDFBeanModule());
        Repository repository = injector.getInstance(Repository.class);
        repository.close();
    }

}
