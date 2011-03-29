package fi.aluesarjat.prototype;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.mysema.rdfbean.model.UID;

public interface SearchService {

    Collection<Facet> getFacets();

    Map<UID, String> getNamespaces();

    SearchResults search(Set<UID> restrictions, boolean includeItems, int limit, int offset, boolean includeAvailableValues);

}