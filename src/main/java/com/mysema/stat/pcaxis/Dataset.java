package com.mysema.stat.pcaxis;

import static com.mysema.stat.pcaxis.PCAxis.DATA;
import static com.mysema.stat.pcaxis.PCAxis.HEADING;
import static com.mysema.stat.pcaxis.PCAxis.STUB;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysema.commons.lang.Assert;

public class Dataset {

    private static final Logger logger = LoggerFactory.getLogger(Dataset.class);
    
    private int dataSize;
    
    private final List<DimensionType> dimensionTypes = new ArrayList<DimensionType>();

    private final List<Item> items;

    private final String name;

    public Dataset(String name, Map<Key, List<Object>> px) {
        this.name = Assert.notNull(name, "name");
        
        dataSize = 1;
        for (Object dimensionType : px.get(STUB)) {
            dataSize *= addDimension((String) dimensionType, px);
        }
        for (Object dimensionType : px.get(HEADING)) {
            dataSize *= addDimension((String) dimensionType, px);
        }

        // Items
        items = new ArrayList<Item>(dataSize);
        
        try {
            addItems(0, new Dimension[dimensionTypes.size()], px.get(DATA), new AtomicInteger(0));
        } catch (IllegalArgumentException e) {
//            System.err.println(e.getMessage());
            logger.warn(e.getMessage());
        }
    }

    private int addDimension(String name, Map<Key, List<Object>> px) {
        DimensionType dimension = new DimensionType(name);
        dimensionTypes.add(dimension);

        List<Object> valueNames = px.get(new Key("VALUES", name));
        List<Object> codes = px.get(new Key("VALUES", name));

        for (int i = 0; i < valueNames.size(); i++) {
            Dimension value = dimension.addDimension((String) valueNames.get(i));
            if (codes != null) {
                value.setCode((String) codes.get(i));
            }
        }
        return valueNames.size();
    }
    
    private void addItems(final int dimensionIndex, final Dimension[] dimensionValues, final List<Object> data, AtomicInteger dataIndex) {
        DimensionType dimension = dimensionTypes.get(dimensionIndex);

        List<Dimension> values = dimension.getDimensions();
        for (int valueIndex = 0; valueIndex < values.size(); valueIndex++) {
            if (dataIndex.get() >= data.size()) {
                throw new IllegalArgumentException("Missing data: expected " + dataSize + " found " + dataIndex.get());
            }
            
            Dimension dimensionValue = values.get(valueIndex);
            dimensionValues[dimensionIndex] = dimensionValue;
            
            if (dimensionIndex + 1 == dimensionTypes.size()) {
                Object dataValue = data.get(dataIndex.getAndIncrement());
                if (dataValue instanceof BigDecimal) {
                    items.add(new Item(this, asList(dimensionValues), (BigDecimal) dataValue));
                }
            } else {
                addItems(dimensionIndex + 1, dimensionValues, data, dataIndex);
            }
        }
    }

    private List<Dimension> asList(final Dimension[] dimensionValues) {
        return new ArrayList<Dimension>(Arrays.asList(dimensionValues));
    }

    public List<DimensionType> getDimensionTypes() {
        return dimensionTypes;
    }

    public List<Item> getItems() {
        return items;
    }

    public String getName() {
        return name;
    }

}
