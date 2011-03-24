package fi.aluesarjat.prototype;

import java.util.Arrays;

import com.mysema.rdfbean.model.UID;

public class Item {

    private final UID id;

    /**
     * Values of facets by position denoted by headers
     */
    private final UID[] values;

    private final String value;

    public Item(UID id, String value, int headerCount) {
        this.id = id;
        this.value = value;
        values = new UID[headerCount];
    }

    public UID getId() {
        return id;
    }

    public UID[] getValues() {
        return values;
    }

    public String getValue() {
        return value;
    }

    public void setValue(int index, UID value) {
        values[index] = value;
    }

    @Override
    public String toString() {
        return value + ": " + Arrays.toString(values);
    }
}
