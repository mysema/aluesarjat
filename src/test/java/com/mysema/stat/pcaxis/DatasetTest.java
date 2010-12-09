package com.mysema.stat.pcaxis;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

public class DatasetTest {

    private final DefaultDatasetHandler handler = new DefaultDatasetHandler();
    
    private final PCAxisParser parser = new PCAxisParser(handler);
    
    @Test
    public void IgnoreDots() throws IOException {        

        Dataset dataset = parser.parse("example-1", getClass().getResourceAsStream("/example-1.px"));

        List<Item> items = handler.getItems(dataset);
        assertEquals(21*19*2 - 6*18*2, items.size()); // Items in total minus ignored values

        Item item;
        List<Dimension> dimensions;

        item = items.get(0);
        dimensions = item.getDimensions();
        assertEquals("370772", item.getValue());
        assertEquals("091 Helsinki", dimensions.get(0).getName());
        assertEquals("Toimiala yhteensä", dimensions.get(1).getName());
        assertEquals("1987", dimensions.get(2).getName());

        item = items.get(20);
        dimensions = item.getDimensions();
        assertEquals("385356", item.getValue());
        assertEquals("091 Helsinki", dimensions.get(0).getName());
        assertEquals("Toimiala yhteensä", dimensions.get(1).getName());
        assertEquals("2007", dimensions.get(2).getName());

        item = items.get(21);
        dimensions = item.getDimensions();
        assertEquals("604", item.getValue());
        assertEquals("091 Helsinki", dimensions.get(0).getName());
        assertEquals(" A Maa-, riista-, ja metsätalous", dimensions.get(1).getName());
        assertEquals("1993", dimensions.get(2).getName());

        item = items.get(35);
        dimensions = item.getDimensions();
        assertEquals("371", item.getValue());
        assertEquals("091 Helsinki", dimensions.get(0).getName());
        assertEquals(" A Maa-, riista-, ja metsätalous", dimensions.get(1).getName());
        assertEquals("2007", dimensions.get(2).getName());

        item = items.get(291);
        dimensions = item.getDimensions();
        assertEquals("2319527", item.getValue());
        assertEquals("049 Espoo", dimensions.get(0).getName());
        assertEquals("Toimiala yhteensä", dimensions.get(1).getName());
        assertEquals("1987", dimensions.get(2).getName());

        item = items.get(items.size()-1);
        dimensions = item.getDimensions();
        assertEquals("24447", item.getValue());
        assertEquals("049 Espoo", dimensions.get(0).getName());
        assertEquals(" X Toimiala tuntematon", dimensions.get(1).getName());
        assertEquals("2007", dimensions.get(2).getName());
    }
    
    @Test
    public void Boolean_Entries() throws IOException{
        Dataset dataset = parser.parse("example-3", getClass().getResourceAsStream("/example-copyright.px"));
        assertNotNull(dataset);
        
        // TODO : do we need Copyright=yes into Dataset ?!?
    }
    
    @Test
    public void Negative_Values() throws IOException{
        Dataset dataset = parser.parse("example-4", getClass().getResourceAsStream("/example-negative-values.px"));
        List<Item> items = handler.getItems(dataset);
        assertEquals("-155", items.get(302).getValue());
        
    }
    
}
