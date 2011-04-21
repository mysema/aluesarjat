package fi.aluesarjat.prototype.guice;

import org.guiceyfruit.jsr250.Jsr250Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.servlet.ServletModule;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.sparql.SPARQLServlet;
import com.mysema.stat.scovo.NamespaceHandler;
import com.sun.tools.xjc.reader.RawTypeSet.Mode;

import fi.aluesarjat.prototype.AreasServlet;
import fi.aluesarjat.prototype.ContextAccessServlet;
import fi.aluesarjat.prototype.DataService;
import fi.aluesarjat.prototype.FacetsServlet;
import fi.aluesarjat.prototype.SearchService;
import fi.aluesarjat.prototype.SearchServiceImpl;
import fi.aluesarjat.prototype.SearchServlet;
import fi.aluesarjat.prototype.SubjectGraphServlet;

public class CustomServletModule extends ServletModule{

    private static final Logger log = LoggerFactory.getLogger(CustomServletModule.class);

    private static final int SPARQL_LIMIT = SearchServlet.EXPORT_LIMIT;

    private static final int SPARQL_MAX_QUERY_TIME = 120;

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

        serve("/data/domain*",
              "/data/dimensions*",
              "/data/datasets").with(ContextAccessServlet.class);

        serve("/data/items*").with(SubjectGraphServlet.class);
    }

    @Provides
    @Singleton
    public NamespaceHandler createNamespaceHandler(Repository repository){
        return new NamespaceHandler(repository);
    }

    @Provides
    @Singleton
    public AreasServlet createAreasServlet(){
        return new AreasServlet();
    }

    @Provides
    @Singleton
    public SPARQLServlet createSPARQLServlet(Repository repository){
        return new SPARQLServlet(repository, SPARQL_LIMIT, SPARQL_MAX_QUERY_TIME);
    }

    @Provides
    @Singleton
    public ContextAccessServlet createContextAccessServlet(Repository repository){
        return new ContextAccessServlet(repository);
    }

    @Provides
    @Singleton
    public SubjectGraphServlet createSubjectGraphServlet(Repository repository){
        return new SubjectGraphServlet(repository);
    }

    @Provides
    @Singleton
    public SearchServlet createFacetedSearchServlet(SearchService searchService){
        return new SearchServlet(searchService);
    }

    @Provides
    @Singleton
    public FacetsServlet createFacetedSearchInitServlet(SearchService searchService){
        return new FacetsServlet(searchService);
    }

    @Provides
    @Singleton
    public SearchService createSearchService(Repository repository){
        return new SearchServiceImpl(repository);
    }

    @Provides
    @Named("import.mode")
    @Singleton
    public Mode importMode(@Named("import.mode") String mode){
        return Mode.valueOf(mode);
    }

    @Provides
    @Named("forceReload")
    @Singleton
    public boolean forceReload(@Named("forceReload") String forceReload){
        return Boolean.parseBoolean(forceReload);
    }

}
