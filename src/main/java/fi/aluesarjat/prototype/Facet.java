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

import java.util.ArrayList;
import java.util.List;

import com.mysema.rdfbean.model.UID;

public class Facet {

    private final UID id;

    private String name;

    private List<Value> values = new ArrayList<Value>();

    // TODO: Extra properties
    
    public Facet(UID id) {
        this.id = id;
    }
    
    public UID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Value> getValues() {
        return values;
    }
    
    public void addValue(Value value) {
        values.add(value);
    }
    
    void setName(String name) {
        this.name = name;
    }
    
    public String toString() {
        return name;
    }
}
