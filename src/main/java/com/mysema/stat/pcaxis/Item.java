package com.mysema.stat.pcaxis;

import java.util.List;

public class Item {
    
    private final Dataset dataset;

    private final List<Dimension> dimensions;

    private final String value;
    
    public Item(Dataset dataset, List<Dimension> dimensions, String value) {
        this.dataset = dataset;
        this.dimensions = dimensions;
        this.value = value;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public List<Dimension> getDimensions() {
        return dimensions;
    }

    /**
    * @return String ".*" or number [0-9.]+
    */
    public String getValue() {
        return value;
    }
    
    public String toString() {
        return dimensions.toString() + " = " + value;
    }
}
