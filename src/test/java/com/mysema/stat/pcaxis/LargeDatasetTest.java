package com.mysema.stat.pcaxis;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.mysema.stat.pcaxis.Dataset;
import com.mysema.stat.pcaxis.Dimension;
import com.mysema.stat.pcaxis.Item;
import com.mysema.stat.pcaxis.Key;
import com.mysema.stat.pcaxis.PCAxis;

import static junit.framework.Assert.*;

public class LargeDatasetTest {
    
    private static Map<Key, List<Object>> px;

    @BeforeClass
    public static void init() {
        px = PCAxis.parse("src/test/resources/A01S_HKI_Vakiluku.px");
    }

    @Test
    public void load() {
        Dataset dataset = new Dataset("A01S_HKI_Vakiluku", px);
        Iterator<Item> items = dataset.getItems().iterator();
        Item item; 

        item = findItem(items, "091 101 Vironniemen peruspiiri", "Yhteensä", "Miehet", "0-vuotiaat", "1996");
        assertNotNull(item);
        assertEquals(new BigDecimal(69), item.getValue());

        item = findItem(items, "091 101 Vironniemen peruspiiri", "Yhteensä", "Miehet", "0-vuotiaat", "1998");
        assertNotNull(item);
        assertEquals(new BigDecimal(52), item.getValue());

        item = findItem(items, "091 101 Vironniemen peruspiiri", "Yhteensä", "Miehet", "2-vuotiaat", "1996");
        assertNotNull(item);
        assertEquals(new BigDecimal(50), item.getValue());

        item = findItem(items, "091 101 Vironniemen peruspiiri", "Yhteensä", "Miehet", "2-vuotiaat", "1998");
        assertNotNull(item);
        assertEquals(new BigDecimal(44), item.getValue());

    

        item = findItem(items, "091 101 Vironniemen peruspiiri", "Yhteensä", "Naiset", "0-vuotiaat", "1996");
        assertNotNull(item);
        assertEquals(new BigDecimal(58), item.getValue());

        item = findItem(items, "091 101 Vironniemen peruspiiri", "Yhteensä", "Naiset", "0-vuotiaat", "1998");
        assertNotNull(item);
        assertEquals(new BigDecimal(57), item.getValue());

        item = findItem(items, "091 101 Vironniemen peruspiiri", "Yhteensä", "Naiset", "2-vuotiaat", "1996");
        assertNotNull(item);
        assertEquals(new BigDecimal(54), item.getValue());

        item = findItem(items, "091 101 Vironniemen peruspiiri", "Yhteensä", "Naiset", "2-vuotiaat", "1998");
        assertNotNull(item);
        assertEquals(new BigDecimal(42), item.getValue());

    
        // Kluuvi
        
        item = findItem(items, "091 20 Kluuvi", "Yhteensä", "Miehet", "0-vuotiaat", "1996");
        assertNotNull(item);
        assertEquals(new BigDecimal(1), item.getValue());

        item = findItem(items, "091 20 Kluuvi", "Yhteensä", "Miehet", "0-vuotiaat", "1998");
        assertNotNull(item);
        assertEquals(new BigDecimal(0), item.getValue());

        item = findItem(items, "091 20 Kluuvi", "Yhteensä", "Miehet", "2-vuotiaat", "1996");
        assertNotNull(item);
        assertEquals(new BigDecimal(1), item.getValue());

        item = findItem(items, "091 20 Kluuvi", "Yhteensä", "Miehet", "2-vuotiaat", "1998");
        assertNotNull(item);
        assertEquals(new BigDecimal(2), item.getValue());

    

        item = findItem(items, "091 20 Kluuvi", "Yhteensä", "Naiset", "0-vuotiaat", "1996");
        assertNotNull(item);
        assertEquals(new BigDecimal(2), item.getValue());

        item = findItem(items, "091 20 Kluuvi", "Yhteensä", "Naiset", "0-vuotiaat", "1998");
        assertNotNull(item);
        assertEquals(new BigDecimal(2), item.getValue());

        item = findItem(items, "091 20 Kluuvi", "Yhteensä", "Naiset", "2-vuotiaat", "1996");
        assertNotNull(item);
        assertEquals(new BigDecimal(0), item.getValue());

        item = findItem(items, "091 20 Kluuvi", "Yhteensä", "Naiset", "2-vuotiaat", "1998");
        assertNotNull(item);
        assertEquals(new BigDecimal(2), item.getValue());
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
