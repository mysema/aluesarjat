/**
 *
 */
package fi.aluesarjat.prototype;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mysema.rdfbean.model.UID;

class Headers {

    private final Map<UID, Integer> headerIndexes = Maps.newLinkedHashMap();

    private final Map<UID, UID> valueToFacet = Maps.newHashMap();

    public int addFacetValue(UID facet, UID value) {
        valueToFacet.put(value, facet);
        Integer index = headerIndexes.get(facet);
        if (index == null) {
            index = headerIndexes.size();
            headerIndexes.put(facet, index);
        }
        return index.intValue();
    }

    public int getHeaderCount() {
        return headerIndexes.size();
    }

    public List<UID> getHeaders() {
        return Lists.newArrayList(headerIndexes.keySet());
    }

    public Set<UID> getAvailableValues() {
        return valueToFacet.keySet();
    }

    public int getFacetIndex(UID facet) {
        Integer index = headerIndexes.get(facet);
        if (index == null) {
            throw new IllegalArgumentException("Unknown value: " + facet);
        }
        return index.intValue();
    }

    public int getValueIndex(UID value) {
        return getFacetIndex(valueToFacet.get(value));
    }
}