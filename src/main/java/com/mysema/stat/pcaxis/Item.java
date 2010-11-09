package com.mysema.stat.pcaxis;

import java.util.List;

public class Item {
    
    private final Dataset dataset;

    private final List<Dimension> dimensions;

    private final Object value;
    
    public Item(Dataset dataset, List<Dimension> dimensions, Object value) {
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
     * @return String or BigDecimal
     */
    public Object getValue() {
        return value;
    }
    
    public String toString() {
        return dimensions.toString() + " = " + value;
    }
}
