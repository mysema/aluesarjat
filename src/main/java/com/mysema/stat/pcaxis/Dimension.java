package com.mysema.stat.pcaxis;


/**
 * @author sasa
 *
 */
public class Dimension {

    private final DimensionType type;
    
    private final String name;
    
    Dimension(DimensionType dimension, String name) {
        this.type = dimension;
        this.name = name;
    }

    public DimensionType getType() {
        return type;
    }

    public String getName() {
        return name;
    }
    
    public String toString() {
        return name;
    }
    
}
