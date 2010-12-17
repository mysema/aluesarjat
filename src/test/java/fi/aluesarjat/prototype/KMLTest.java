package fi.aluesarjat.prototype;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.mysema.stat.scovo.XMLID;

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
        System.out.println("@prefix alue: <http://localhost:8080/rdf/dimensions/Alue#> .");
        System.out.println("@prefix geo: <http://www.w3.org/2003/01/geo/> .");
        System.out.println();
        
        
        
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
        Map<String,String> values = new HashMap<String,String>();        
        for (SchemaData schemaData : placemark.getExtendedData().getSchemaData()){
            for (SimpleData simpleData : schemaData.getSimpleData()){
                values.put(simpleData.getName(), simpleData.getValue());
            }
        }
        
        double min_lat = Double.MAX_VALUE;
        double max_lat = 0.0;
        double min_long = Double.MAX_VALUE;
        double max_long = 0.0;
        
        StringBuilder polygons = new StringBuilder();
        // geometry
        Polygon polygon = (Polygon)placemark.getGeometry();
        LinearRing ring = polygon.getOuterBoundaryIs().getLinearRing();
        for (Coordinate coordinate : ring.getCoordinates()){
            if (polygons.length() > 0){
                polygons.append(" ");
            }
            polygons.append(coordinate.getLatitude()).append(",").append(coordinate.getLongitude());
            min_lat = Math.min(min_lat, coordinate.getLatitude());
            max_lat = Math.max(max_lat, coordinate.getLatitude());
            min_long = Math.min(min_long, coordinate.getLongitude());
            max_long = Math.max(max_long, coordinate.getLongitude());
        }
        
        double center_lat = min_lat + (max_lat - min_lat ) / 2;
        double center_long = min_long + (max_long - min_long) / 2;
        
        String kunta = values.get("KUNTA");
        String suur = values.get("SUUR");
        String nimi = values.get("Nimi");
        String code = kunta + " " + suur + " " + nimi;        
        System.out.println("alue:" + XMLID.toXMLID(code) + " geo:center \"" + center_lat+","+center_long + "\" .");
        System.out.println("alue:" + XMLID.toXMLID(code) + " geo:polygon \"" + polygons + "\" .");
    }
    
}
