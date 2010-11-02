package com.mysema.stat.pcaxis;


public class Dimension {

    private String code;

    private final DimensionType type;
    
    private final String name;
    
    Dimension(DimensionType dimension, String name) {
        this.type = dimension;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public DimensionType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setCode(String code) {
        this.code = code;
    }
    
    public String toString() {
        return name;
    }
    
}
