package fi.aluesarjat.prototype.guice;

import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.sparql.SPARQLServlet;

import fi.aluesarjat.prototype.ContextAccessServlet;
import fi.aluesarjat.prototype.DataService;

public class CustomServletModule extends ServletModule{

    @Override
    protected void configureServlets() {
        install(new CustomRDFBeanModule());
//        install(new VirtuosoRDFBeanModule());
        install(new Jsr250Module());
        bind(DataService.class).asEagerSingleton();

        serve("/query").with(SPARQLServlet.class);
        serve("/rdf/domain*",
              "/rdf/dimensions*",
              "/rdf/datasets").with(ContextAccessServlet.class);
    }

    @Provides
    @Singleton
    public SPARQLServlet createSPARQLServlet(Repository repository){
        return new SPARQLServlet(repository, 1000);
    }

    @Provides
    @Singleton
    public ContextAccessServlet createContextAccessServlet(Repository repository){
        return new ContextAccessServlet(repository);
    }

}
