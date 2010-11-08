package fi.aluesarjat.prototype.guice;

import com.mysema.rdfbean.guice.RDFBeanModule;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.object.Configuration;
import com.mysema.rdfbean.sesame.MemoryRepository;

public class CustomRDFBeanModule extends RDFBeanModule{

    @Override
    public Repository createRepository(Configuration configuration) {
        return new MemoryRepository();
    }    
    
}
