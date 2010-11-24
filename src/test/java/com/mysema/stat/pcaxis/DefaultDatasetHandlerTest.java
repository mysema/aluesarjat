package com.mysema.stat.pcaxis;

import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.junit.Test;


public class DefaultDatasetHandlerTest {

    @Test
    public void Parse() throws IOException{
        DefaultDatasetHandler handler = new DefaultDatasetHandler();
        PCAxisParser parser = new PCAxisParser(handler);
        parser.parse("A01HKIS_Vaestotulot", getClass().getResourceAsStream("/data/A01HKIS_Vaestotulot.px"));
        for (Item item : handler.getItems("A01HKIS_Vaestotulot")){
            String str = item.getDimensions().toString();
            assertFalse(str, str.contains("yhteensï¿½"));
        }
    }

}
