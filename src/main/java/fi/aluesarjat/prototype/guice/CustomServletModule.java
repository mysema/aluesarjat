package fi.aluesarjat.prototype.guice;

import org.guiceyfruit.jsr250.Jsr250Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.sparql.SPARQLServlet;
import com.mysema.stat.scovo.NamespaceHandler;

import fi.aluesarjat.prototype.AreasServlet;
import fi.aluesarjat.prototype.ContextAccessServlet;
import fi.aluesarjat.prototype.DataService;
import fi.aluesarjat.prototype.FacetsServlet;
import fi.aluesarjat.prototype.SearchServlet;

public class CustomServletModule extends ServletModule{

    private static final Logger log = LoggerFactory.getLogger(CustomServletModule.class);

    @Override
    protected void configureServlets() {
        String store = System.getProperty("rdfbean.store");
        if ("virtuoso".equals(store)){
            log.info("Using Virtuoso backend");
            install(new VirtuosoRDFBeanModule());
        }else if ("memory".equals(store)){
            log.info("Using Memory backend");
            install(new MemoryStoreRDFBeanModule());
        }else{
            log.info("Using NativeStore backend");
            install(new NativeStoreRDFBeanModule());
        }
        install(new Jsr250Module());
        bind(DataService.class).asEagerSingleton();

        serve("/areas").with(AreasServlet.class);
        serve("/sparql").with(SPARQLServlet.class);
        serve("/search").with(SearchServlet.class);
        serve("/facets").with(FacetsServlet.class);
        serve("/rdf/domain*",
              "/rdf/dimensions*",
              "/rdf/datasets").with(ContextAccessServlet.class);
    }

    @Provides
    @Singleton
    public NamespaceHandler createNamespaceHandler(Repository repository){
        return new NamespaceHandler(repository);
    }

    @Provides
    @Singleton
    public AreasServlet createAreasServlet(Repository repository){
        return new AreasServlet(repository);
    }
    
    @Provides
    @Singleton
    public SPARQLServlet createSPARQLServlet(Repository repository){
        return new SPARQLServlet(repository, 1000, 120);
    }

    @Provides
    @Singleton
    public ContextAccessServlet createContextAccessServlet(Repository repository){
        return new ContextAccessServlet(repository);
    }

    @Provides
    @Singleton
    public SearchServlet createFacetedSearchServlet(Repository repository){
        return new SearchServlet(repository);
    }

    @Provides
    @Singleton
    public FacetsServlet createFacetedSearchInitServlet(Repository repository){
        return new FacetsServlet(repository);
    }

}
