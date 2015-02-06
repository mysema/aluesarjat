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

import com.mysema.rdfbean.model.UID;

public class Value {
    
    private final UID id;

    private String name;
    
    private String description;
    
    private UID parent;
    
    // TODO: Extra properties
    
    public Value(UID id) {
        this.id = id;
    }

    public UID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    public UID getParent() {
        return parent;
    }

    void setParent(UID parent) {
        this.parent = parent;
    }

    public String toString() {
        return name;
    }
}
