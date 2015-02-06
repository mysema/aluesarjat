/*
* Copyright 2013 Mysema Ltd
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package fi.aluesarjat.prototype;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.mysema.rdfbean.model.UID;

public class SearchResults {

    private Set<UID> availableValues = Sets.newLinkedHashSet();

    /**
     * Facet IDs
     */
    private List<UID> headers;

    private List<Item> items;

    private boolean hasMoreResults;

    public Set<UID> getAvailableValues() {
        return availableValues;
    }

    public boolean isHasMoreResults() {
        return hasMoreResults;
    }

    public List<UID> getHeaders() {
        return headers;
    }

    public List<Item> getItems() {
        return items;
    }

    void addAvailableValue(UID valueId) {
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

    void setHasMoreResults(boolean hasMoreResults) {
        this.hasMoreResults = hasMoreResults;
    }

}
