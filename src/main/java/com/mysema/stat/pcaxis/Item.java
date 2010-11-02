package com.mysema.stat.pcaxis;

import java.math.BigDecimal;
import java.util.List;

public class Item {
    
    private final Dataset dataset;

    private final List<Dimension> dimensions;

    private final BigDecimal value;
    
    public Item(Dataset dataset, List<Dimension> dimensions, BigDecimal value) {
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

    public BigDecimal getValue() {
        return value;
    }
    
    public String toString() {
        return dimensions.toString() + " = " + value;
    }
}
