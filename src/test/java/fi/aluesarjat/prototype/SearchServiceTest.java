package fi.aluesarjat.prototype;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import javax.servlet.ServletException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.inject.internal.Lists;
import com.mysema.rdfbean.model.UID;
import com.mysema.rdfbean.sesame.MemoryRepository;
import com.mysema.stat.scovo.NamespaceHandler;
import com.mysema.stat.scovo.SCV;

import fi.aluesarjat.prototype.guice.ModuleUtils;

public class SearchServiceTest {

    protected static MemoryRepository repository;
    
    protected static SearchService service;

    private static final UID AREA = new UID("http://localhost:8080/rdf/dimensions/Alue");

    private static final UID HKI = new UID("http://localhost:8080/rdf/dimensions/Alue#Helsinki");

    private static final UID ESP = new UID("http://localhost:8080/rdf/dimensions/Alue#Espoo");

    private static final UID YEAR = new UID("http://localhost:8080/rdf/dimensions/Vuosi");

    private static final UID Y2010 = new UID("http://localhost:8080/rdf/dimensions/Vuosi#_2010");

    private static final UID Y2011 = new UID("http://localhost:8080/rdf/dimensions/Vuosi#_2011");

    private static final UID INDUSTRY = new UID("http://localhost:8080/rdf/dimensions/Toimiala");

    private static final UID ICT = new UID("http://localhost:8080/rdf/dimensions/Toimiala#ICT");

    private static final UID ATK = new UID("http://localhost:8080/rdf/dimensions/Toimiala#ATK");

    private static final UID CREATOR = new UID("http://localhost:8080/rdf/dimensions/Tekij\u00E4");

    private static final UID SAMPPA = new UID("http://localhost:8080/rdf/dimensions/Tekij\u00E4#Samppa");

    private static final UID TIMO = new UID("http://localhost:8080/rdf/dimensions/Tekij\u00E4#Timo");

    private static final UID DATASET = SCV.Dataset;

    private static final UID DATASET1 = new UID("http://localhost:8080/rdf/datasets#search-test-1");

    private static final UID DATASET2 = new UID("http://localhost:8080/rdf/datasets#search-test-2");

    private static final UID UNITS = new UID("http://localhost:8080/rdf/dimensions/Yksikk\u00F6");

    private static final UID UNIT = new UID("http://localhost:8080/rdf/dimensions/Yksikk\u00F6#Unit");
    
    @BeforeClass
    public static void setUpClass() throws ServletException, IOException{
        String baseURI = ModuleUtils.DEFAULT_BASE_URI;
        repository = new MemoryRepository();
//        repository.setSources(ModuleUtils.getSources(baseURI));
        repository.initialize();

        NamespaceHandler namespaceHandler = new NamespaceHandler(repository);
        DataService dataService = new DataService(repository, namespaceHandler, baseURI, DataService.Mode.NONTHREADED.name(), "true");
        dataService.setDatasets(Lists.newArrayList("classpath:/search-test-1.px \".\"", "classpath:/search-test-2.px \".\""));
        dataService.initialize();
        
        service = new SearchService(repository);
    }
    
    @Test
    public void Facets() {
        Collection<Facet> facets = service.getFacets();
        
        assertEquals(6, facets.size()); // Dimensions (Alue, Vuosi, Toimiala, Tekijä) + Units + Dataset
        
        Multimap<UID, UID> expectedFacets = LinkedHashMultimap.create();
        expectedFacets.put(AREA, HKI);
        expectedFacets.put(AREA, ESP);
        expectedFacets.put(YEAR, Y2010);
        expectedFacets.put(YEAR, Y2011);
        expectedFacets.put(INDUSTRY, ATK);
        expectedFacets.put(INDUSTRY, ICT);
        expectedFacets.put(CREATOR, SAMPPA);
        expectedFacets.put(CREATOR, TIMO);
        expectedFacets.put(DATASET, DATASET1);
        expectedFacets.put(DATASET, DATASET2);
        expectedFacets.put(UNITS, UNIT);

        for (Facet facet : facets) {
            Collection<UID> expectedValues = expectedFacets.get(facet.getId());
            assertNotNull(expectedValues);
            
            for (Value value : facet.getValues()) {
                assertTrue("Found extra facet/value: " + value.getId(), expectedFacets.remove(facet.getId(), value.getId()));
            }
        }
        
        assertEquals("Not all facets/values found: " + expectedFacets, 0, expectedFacets.size());
    }

    @Test
    public void Single_Dimension() {
        SearchResults results = service.search(Sets.newHashSet(HKI), true, 1000, 0, true);
        
        // Not enough restrictions!
        assertNull(results.getItems());
        assertNull(results.getHeaders());
        
        assertExpectedValues(results.getAvailableValues(), HKI, ESP, Y2010, Y2011, ATK, ICT, SAMPPA, TIMO, DATASET1, DATASET2, UNIT);
    }

    @Test
    public void Single_Dataset() {
        SearchResults results = service.search(Sets.newHashSet(DATASET1), true, 1000, 0, true);
        
        // Not enough restrictions!
        assertNull(results.getItems());
        assertNull(results.getHeaders());
        
        assertExpectedValues(results.getAvailableValues(), HKI, ESP, Y2010, Y2011, ICT, ATK, DATASET1, UNIT);
    }
    
    private void assertExpectedValues(Set<UID> actualValues, UID ... expectedValues) {
        Set<UID> expected = Sets.newHashSet(expectedValues);
        for (UID value : actualValues) {
            assertTrue("Found extra value: " + value, expected.remove(value));
        }
        
        assertEquals("Not all values found: " + expected, 0, expected.size());
    }
    
    
    @AfterClass
    public static void tearDownClass(){
        repository.close();
    }
    
}
