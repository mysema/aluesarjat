package fi.aluesarjat.prototype;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.mysema.rdfbean.model.UID;
import com.mysema.rdfbean.virtuoso.VirtuosoRepository;

public class SearchServiceLoad {

    public static void main(String[] args) throws InterruptedException{
        VirtuosoRepository repository = new VirtuosoRepository("localhost:1111", "dba", "dba");
        repository.initialize();
        SearchService searchService = new SearchServiceImpl(repository);

        List<String> slowQueries = new ArrayList<String>();
        Set<Set<UID>> tested = new HashSet<Set<UID>>();

        try{
            Collection<Facet> facets = searchService.getFacets();
            for (Facet facet : facets){
                // 1
                for (Value value : facet.getValues()){
                    Set<UID> uids = Sets.newHashSet(value.getId());
                    if (!tested.add(uids)) continue;
                    log(uids);
                    long start = System.currentTimeMillis();
                    SearchResults searchResults = searchService.search(uids, true, 50, 0, true);
                    log(slowQueries, uids, System.currentTimeMillis() - start);

                    // 2
                    for (UID value2 : searchResults.getAvailableValues()){
                        uids = Sets.newHashSet(value.getId(), value2);
                        if (!tested.add(uids)) continue;
                        log(uids);
                        start = System.currentTimeMillis();
                        searchResults = searchService.search(uids, true, 50, 0, true);
                        log(slowQueries, uids, System.currentTimeMillis() - start);

                        // 3
                        Set<String> ns = Sets.newHashSet();
                        for (UID value3 : searchResults.getAvailableValues()){
                            uids = Sets.newHashSet(value.getId(), value2, value3);
                            if (!tested.add(uids) || !ns.add(value3.ns())) continue;
                            log(uids);
                            start = System.currentTimeMillis();
                            searchResults = searchService.search(uids, true, 50, 0, true);
                            log(slowQueries, uids, System.currentTimeMillis() - start);
                        }

                        Thread.sleep(100);
                    }

                }
            }

            System.err.println();
            for (String query : slowQueries){
                System.err.println(query);
            }

        }finally{
            repository.close();
        }

    }

    private static void log(Collection<UID> uids){
        System.err.println(toString(uids));
    }

    private static void log(List<String> slowQueries, Collection<UID> uids, long duration) throws InterruptedException {
        if (duration > 1000){
            System.err.println(duration);
            slowQueries.add(duration + "ms : " + toString(uids));
        }
    }

    private static String toString(Collection<UID> uids){
        Set<String> ids = Sets.newHashSet();
        for (UID uid : uids){
            ids.add(uid.getId().substring(uid.getId().lastIndexOf('/')+1));
        }
        return ids.toString();
    }

}
