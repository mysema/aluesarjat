package fi.aluesarjat.prototype.guice;

import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.sparql.SPARQLServlet;
import com.mysema.stat.scovo.NamespaceHandler;

import fi.aluesarjat.prototype.ContextAccessServlet;
import fi.aluesarjat.prototype.DataService;
import fi.aluesarjat.prototype.FacetsServlet;
import fi.aluesarjat.prototype.SearchServlet;

public class CustomServletModule extends ServletModule{

    @Override
    protected void configureServlets() {
        install(new NativeStoreRDFBeanModule());
//        install(new VirtuosoRDFBeanModule());
//        install(new BigDataRDFBeanModule());
        install(new Jsr250Module());
        bind(DataService.class).asEagerSingleton();

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
