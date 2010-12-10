package fi.aluesarjat.prototype;

import java.io.IOException;

import org.junit.Test;

import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import de.micromata.opengis.kml.v_2_2_0.SchemaData;
import de.micromata.opengis.kml.v_2_2_0.SimpleData;

public class KMLTest {

    @Test
    public void test() throws IOException{
        Kml kml = Kml.unmarshal(getClass().getResourceAsStream("/PKS_suuralue_TESTI.kml"));
        if (kml.getFeature() instanceof Document){
            Document document = (Document)kml.getFeature();
            for (Feature documentFeature : document.getFeature()){
                if (documentFeature instanceof Folder){
                    Folder folder = (Folder)documentFeature;
                    for (Feature folderFeature : folder.getFeature()){
                        if (folderFeature instanceof Placemark){
                            handlePlacemark((Placemark)folderFeature);                            
                        }
                    }
                }
            }    
        }        
    }
    
    private void handlePlacemark(Placemark placemark){        
        // data
        for (SchemaData schemaData : placemark.getExtendedData().getSchemaData()){
            for (SimpleData simpleData : schemaData.getSimpleData()){
                System.out.println(simpleData.getName() + " " + simpleData.getValue());
            }
        }
        
        // geometry
        Polygon polygon = (Polygon)placemark.getGeometry();
        LinearRing ring = polygon.getOuterBoundaryIs().getLinearRing();
        for (Coordinate coordinate : ring.getCoordinates()){
            System.out.println(coordinate.getLatitude() + " " + coordinate.getLongitude());
        }
    }
    
}
