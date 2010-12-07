package com.mysema.stat.pcaxis;

import static com.mysema.stat.pcaxis.PCAxis.CONTENTS;
import static com.mysema.stat.pcaxis.PCAxis.DATA;
import static com.mysema.stat.pcaxis.PCAxis.HEADING;
import static com.mysema.stat.pcaxis.PCAxis.NOTE;
import static com.mysema.stat.pcaxis.PCAxis.SOURCE;
import static com.mysema.stat.pcaxis.PCAxis.STUB;
import static com.mysema.stat.pcaxis.PCAxis.UNITS;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import com.mysema.commons.lang.Assert;

public class Dataset {

    private String description;

    private final List<DimensionType> dimensionTypes = new ArrayList<DimensionType>();

    private final String name;

    private String publisher;

    private String title;

    private String units;

    public String getUnits() {
        return units;
    }

    public Dataset(String name) {
        this.name = Assert.notNull(name, "name");
    }

    public void set(Key key, List<String> values) {
        if (CONTENTS.equals(key)) {
            title = toString(values);
        }

        else if (NOTE.equals(key)) {
            description = toString(values);
        }

        else if (SOURCE.equals(key)) {
            publisher = toString(values);
        }

        else if (UNITS.equals(key)) {
            units = toString(values);
        }

        else if (STUB.equals(key)) {
            // Append at start of dimensions
            for (int i=0; i < values.size(); i++) {
                dimensionTypes.add(i, new DimensionType(toString(values.get(i))));
            }
        }

        else if (HEADING.equals(key)) {
            // Append to dimensions
            for (String value : values) {
                dimensionTypes.add(new DimensionType(toString(value)));
            }
        }

        else if ("VALUES".equals(key.getName())) {
            DimensionType type = findDimensionType(key.getSpecifier());
            for (String value : values) {
                type.addDimension(toString(value));
            }
        }

        else if (DATA.equals(key)) {
            throw new IllegalArgumentException("DATA cannot be added directly to Dataset");
        }
    }

    private DimensionType findDimensionType(String name) {
        for (DimensionType d : dimensionTypes) {
            if (d.getName().equals(name)) {
                return d;
            }
        }
        throw new NoSuchElementException("DimensionType: " + name);
    }

    public String getDescription() {
        return description;
    }

    public List<DimensionType> getDimensionTypes() {
        return dimensionTypes;
    }

    public String getName() {
        return name;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getTitle() {
        return title;
    }

    private String toString(String value) {
        if (value.startsWith("\"")) {
            return value.substring(1, value.length()-1).replace('#', '\n');
        } else {
            return value;
        }
    }

    private String toString(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        StringBuilder sb  = new StringBuilder(values.size() * 80);
        for (String value : values) {
            sb.append(toString(value));
        }
        return sb.toString();
    }

    public int dimensions() {
        return dimensionTypes.size();
    }

    public DimensionType getDimensionType(int dimensionIndex) {
        return dimensionTypes.get(dimensionIndex);
    }

    public void addDimensionType(DimensionType dimensionType) {
        dimensionTypes.add(dimensionType);
    }
}
