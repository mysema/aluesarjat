package fi.aluesarjat.prototype;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class DataServiceTest {
    
    @Test
    public void SplitDatasetDef() {
        List<String> elements = DataService.splitDatasetDef("file:/opt/aluesarjat/data/Vaesto_sal/Perheet_sal/B01S_ESP_Perhetyypit.px \".\"");
        assertEquals(Arrays.asList("file:/opt/aluesarjat/data/Vaesto_sal/Perheet_sal/B01S_ESP_Perhetyypit.px", "\".\""), elements);
    }
    
    @Test
    public void SplitDatasetDef_With_Spaces() {
        List<String> elements = DataService.splitDatasetDef("file:/opt/aluesarjat/data/Vaesto sal/Perheet_sal/B01S_ESP_Perhetyypit.px \".\"");
        assertEquals(Arrays.asList("file:/opt/aluesarjat/data/Vaesto sal/Perheet_sal/B01S_ESP_Perhetyypit.px", "\".\""), elements);
    }

}
