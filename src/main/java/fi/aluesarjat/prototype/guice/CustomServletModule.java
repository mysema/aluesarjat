package fi.aluesarjat.prototype.guice;

import java.io.IOException;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.sparql.SPARQLServlet;

import fi.aluesarjat.prototype.ContextAccessServlet;

public class CustomServletModule extends ServletModule{
    
    @Override
    protected void configureServlets() {
        try {
            install(new CustomRDFBeanModule());
            serve("/query").with(SPARQLServlet.class);
            serve("/rdf/*").with(ContextAccessServlet.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }        
    }
    
    @Provides
    @Singleton
    public SPARQLServlet createSPARQLServlet(Repository repository){
        return new SPARQLServlet(repository);
    }
    
    @Provides
    @Singleton
    public ContextAccessServlet createContextAccessServlet(Repository repository){
        return new ContextAccessServlet(repository);
    }

}
