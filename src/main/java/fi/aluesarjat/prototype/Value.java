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
