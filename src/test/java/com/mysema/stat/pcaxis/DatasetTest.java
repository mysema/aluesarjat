package com.mysema.stat.pcaxis;

import static junit.framework.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;

public class DatasetTest {

    @Test
    public void ignoreDots() {
        Dataset dataset = new Dataset("example-1", PCAxis.parse(getClass().getResourceAsStream("/example-1.px")));
        List<Item> items = dataset.getItems();
        assertEquals(21*2 + 15*36, items.size());
        
        Item item;
        List<Dimension> dimensions;
        
        item = items.get(0);
        dimensions = item.getDimensions();
        assertEquals(new BigDecimal(370772), item.getValue());
        assertEquals("091 Helsinki", dimensions.get(0).getName());
        assertEquals("Toimiala yhteensä", dimensions.get(1).getName());
        assertEquals("1987", dimensions.get(2).getName());
        
        item = items.get(20);
        dimensions = item.getDimensions();
        assertEquals(new BigDecimal(385356), item.getValue());
        assertEquals("091 Helsinki", dimensions.get(0).getName());
        assertEquals("Toimiala yhteensä", dimensions.get(1).getName());
        assertEquals("2007", dimensions.get(2).getName());
        
        item = items.get(21);
        dimensions = item.getDimensions();
        assertEquals(new BigDecimal(604), item.getValue());
        assertEquals("091 Helsinki", dimensions.get(0).getName());
        assertEquals(" A Maa-, riista-, ja metsätalous", dimensions.get(1).getName());
        assertEquals("1993", dimensions.get(2).getName());
        
        item = items.get(35);
        dimensions = item.getDimensions();
        assertEquals(new BigDecimal(371), item.getValue());
        assertEquals("091 Helsinki", dimensions.get(0).getName());
        assertEquals(" A Maa-, riista-, ja metsätalous", dimensions.get(1).getName());
        assertEquals("2007", dimensions.get(2).getName());
        
        item = items.get(291);
        dimensions = item.getDimensions();
        assertEquals(new BigDecimal(2319527), item.getValue());
        assertEquals("049 Espoo", dimensions.get(0).getName());
        assertEquals("Toimiala yhteensä", dimensions.get(1).getName());
        assertEquals("1987", dimensions.get(2).getName());
        
        item = items.get(items.size()-1);
        dimensions = item.getDimensions();
        assertEquals(new BigDecimal(24447), item.getValue());
        assertEquals("049 Espoo", dimensions.get(0).getName());
        assertEquals(" X Toimiala tuntematon", dimensions.get(1).getName());
        assertEquals("2007", dimensions.get(2).getName());
    }
    
}
