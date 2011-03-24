package fi.aluesarjat.prototype;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.mysema.rdfbean.model.UID;

public class SearchResults {

    private Set<UID> availableValues;

    /**
     * Facet IDs
     */
    private List<UID> headers;

    private List<Item> items;

    public Set<UID> getAvailableValues() {
        return availableValues;
    }

    public List<UID> getHeaders() {
        return headers;
    }

    public List<Item> getItems() {
        return items;
    }

    void addAvailableValue(UID valueId) {
        if (availableValues == null) {
            availableValues = Sets.newLinkedHashSet();
        }
        availableValues.add(valueId);
    }

    void setAvailableValues(Set<UID> availableValues) {
        this.availableValues = availableValues;
    }

    void setHeaders(List<UID> headers) {
        this.headers = headers;
    }

    void setItems(List<Item> items) {
        this.items = items;
    }

}
