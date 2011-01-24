package com.mysema.stat.pcaxis;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sasa
 *
 */
public class DimensionType {
    
    private final String name;
    
    private transient List<Dimension> dimensions = new ArrayList<Dimension>();

    public DimensionType(String name) {
        this.name = name;
    }
    
    public Dimension addDimension(String valueName) {
        Dimension value = new Dimension(this, valueName);
        dimensions.add(value);
        return value;
    }
    
    public String getName() {
        return name;
    }
    
    public List<Dimension> getDimensions() {
        return dimensions;
    }
    
    public int size() {
        return dimensions.size();
    }
    
    public String toString() {
        return name;
    }
    
}
