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

import java.util.Arrays;

import com.mysema.rdfbean.model.UID;

public class Item {

    private final UID id;

    /**
     * Values of facets by position denoted by headers
     */
    private final UID[] values;

    private final String value;

    public Item(UID id, String value, int headerCount) {
        this.id = id;
        this.value = value;
        values = new UID[headerCount];
    }

    public UID getId() {
        return id;
    }

    public UID[] getValues() {
        return values;
    }

    public String getValue() {
        return value;
    }

    public void setValue(int index, UID value) {
        values[index] = value;
    }

    @Override
    public String toString() {
        return value + ": " + Arrays.toString(values);
    }
}
