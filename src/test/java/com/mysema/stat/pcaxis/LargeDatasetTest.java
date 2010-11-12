package com.mysema.stat.pcaxis;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

public class LargeDatasetTest {

    @Test
    public void load() throws IOException {
        DefaultDatasetHandler handler = new DefaultDatasetHandler();
        PCAxisParser parser = new PCAxisParser(handler);

        Dataset dataset = parser.parse("A01S_HKI_Vakiluku", getClass().getResourceAsStream("/A01S_HKI_Vakiluku.px"));

        Iterator<Item> items = handler.getItems(dataset).iterator();
        Item item;

        item = findItem(items, "091 101 Vironniemen peruspiiri", "Yhteensä", "Miehet", "0-vuotiaat", "1996");
        assertNotNull(item);
        assertEquals("69", item.getValue());

        item = findItem(items, "091 101 Vironniemen peruspiiri", "Yhteensä", "Miehet", "0-vuotiaat", "1998");
        assertNotNull(item);
        assertEquals("52", item.getValue());

        item = findItem(items, "091 101 Vironniemen peruspiiri", "Yhteensä", "Miehet", "2-vuotiaat", "1996");
        assertNotNull(item);
        assertEquals("50", item.getValue());

        item = findItem(items, "091 101 Vironniemen peruspiiri", "Yhteensä", "Miehet", "2-vuotiaat", "1998");
        assertNotNull(item);
        assertEquals("44", item.getValue());



        item = findItem(items, "091 101 Vironniemen peruspiiri", "Yhteensä", "Naiset", "0-vuotiaat", "1996");
        assertNotNull(item);
        assertEquals("58", item.getValue());

        item = findItem(items, "091 101 Vironniemen peruspiiri", "Yhteensä", "Naiset", "0-vuotiaat", "1998");
        assertNotNull(item);
        assertEquals("57", item.getValue());

        item = findItem(items, "091 101 Vironniemen peruspiiri", "Yhteensä", "Naiset", "2-vuotiaat", "1996");
        assertNotNull(item);
        assertEquals("54", item.getValue());

        item = findItem(items, "091 101 Vironniemen peruspiiri", "Yhteensä", "Naiset", "2-vuotiaat", "1998");
        assertNotNull(item);
        assertEquals("42", item.getValue());


        // Kluuvi

        item = findItem(items, "091 20 Kluuvi", "Yhteensä", "Miehet", "0-vuotiaat", "1996");
        assertNotNull(item);
        assertEquals("1", item.getValue());

        item = findItem(items, "091 20 Kluuvi", "Yhteensä", "Miehet", "0-vuotiaat", "1998");
        assertNotNull(item);
        assertEquals("0", item.getValue());

        item = findItem(items, "091 20 Kluuvi", "Yhteensä", "Miehet", "2-vuotiaat", "1996");
        assertNotNull(item);
        assertEquals("1", item.getValue());

        item = findItem(items, "091 20 Kluuvi", "Yhteensä", "Miehet", "2-vuotiaat", "1998");
        assertNotNull(item);
        assertEquals("2", item.getValue());



        item = findItem(items, "091 20 Kluuvi", "Yhteensä", "Naiset", "0-vuotiaat", "1996");
        assertNotNull(item);
        assertEquals("2", item.getValue());

        item = findItem(items, "091 20 Kluuvi", "Yhteensä", "Naiset", "0-vuotiaat", "1998");
        assertNotNull(item);
        assertEquals("2", item.getValue());

        item = findItem(items, "091 20 Kluuvi", "Yhteensä", "Naiset", "2-vuotiaat", "1996");
        assertNotNull(item);
        assertEquals("0", item.getValue());

        item = findItem(items, "091 20 Kluuvi", "Yhteensä", "Naiset", "2-vuotiaat", "1998");
        assertNotNull(item);
        assertEquals("2", item.getValue());
    }

    private Item findItem(Iterator<Item> items, String... dimensions) {
        Item item = null;
        while (items.hasNext() && item == null) {
            item = items.next();
            List<Dimension> values = item.getDimensions();
            for (int i=0; i < dimensions.length; i++) {
                if (!dimensions[i].equals(values.get(i).getName())) {
                    item = null;
                    break;
                }
            }
        }
        return item;
    }

}
