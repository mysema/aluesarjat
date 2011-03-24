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
    
}
