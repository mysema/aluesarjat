package fi.aluesarjat.prototype.guice;

import org.junit.Ignore;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.mysema.rdfbean.model.Repository;


@Ignore
public class VirtuosoRDFBeanModuleTest {

    @Test
    public void Create(){
        Injector injector = Guice.createInjector(new VirtuosoRDFBeanModule());
        Repository repository = injector.getInstance(Repository.class);
        repository.close();
    }
}
