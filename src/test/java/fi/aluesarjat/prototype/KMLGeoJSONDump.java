package fi.aluesarjat.prototype;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.mysema.stat.scovo.XMLID;

import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import de.micromata.opengis.kml.v_2_2_0.SchemaData;
import de.micromata.opengis.kml.v_2_2_0.SimpleData;

public class KMLGeoJSONDump {
    
    private final Map<String,String> names = new HashMap<String,String>();
    
    private final Set<String> level1 = new HashSet<String>();
    
    private final Set<String> level2 = new HashSet<String>();
    
    private final Set<String> level3 = new HashSet<String>();
    
    private final Map<String,Coordinate> centers = new HashMap<String,Coordinate>();
    
    private final Map<String,List<Coordinate>> polygons = new HashMap<String,List<Coordinate>>();
    
    public static void main(String[] args) throws IOException{
        new KMLGeoJSONDump().handle();                
    }
    
    public void handle() throws IOException{
        File areas = new File("src/test/resources/areas");
        for (File file : areas.listFiles()){
            if (!file.getName().endsWith(".kml")){
                continue;
            }
            InputStream is = new FileInputStream(file);
            try{
                Kml kml = Kml.unmarshal(is);            
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
            }finally{
                is.close();
            }
                
        }   
        
        int counter = 1;
        for (Set<String> codes : Arrays.asList(level1, level2, level3)){
            JSONObject root = new JSONObject();
            root.put("type","FeatureCollection");        
            JSONArray features = new JSONArray();
            for (String code : codes){
                JSONObject feature = new JSONObject();
                feature.put("type", "Feature");
                JSONObject geometry = new JSONObject();
                geometry.put("type","MultiPolygon");
                JSONArray coordinates = new JSONArray();
                List<Coordinate> value = polygons.get(code);
                if (value == null){
                    continue;
                }
                for (Coordinate coordinate : value){
                    coordinates.add(toJSONArray(coordinate.getLongitude(), coordinate.getLatitude()));
                }
                geometry.put("coordinates", toJSONArray(toJSONArray(coordinates))); // TODO : get rid of wrapping
                feature.put("geometry", geometry);
                
                JSONObject properties = new JSONObject();
                properties.put("code", code);
                
                Coordinate centerPoint = centers.get(code);
                properties.put("center", toJSONArray(centerPoint.getLongitude(), centerPoint.getLatitude()));
                properties.put("name", names.get(code));
                feature.put("properties", properties);
                
                features.add(feature);                        
            }        
            root.put("features",features);
            String str = root.toString();
            File out = new File("src/main/resources/area"+(counter++)+".json");
            FileUtils.writeStringToFile(out, str, "UTF-8");
        }
        
    }

    private void handlePlacemark(Placemark placemark) throws IOException{        
        Map<String,String> values = new HashMap<String,String>();        
        for (SchemaData schemaData : placemark.getExtendedData().getSchemaData()){
            for (SimpleData simpleData : schemaData.getSimpleData()){
                values.put(simpleData.getName(), simpleData.getValue());
            }
        }

        String kunta = values.get("KUNTA");
        String suur = values.get("SUUR");
        String tila = values.get("TILA");
        String pien = values.get("PIEN");
        String nimi = values.get("Nimi");
        String code = null;
        if (!StringUtils.isEmpty(pien)){
            // pienialue
            code = XMLID.toXMLID(kunta + " " + pien + " " + nimi);
            level1.add(code);
        }else if (!StringUtils.isEmpty(tila)){
            // ?!?
            code = XMLID.toXMLID(kunta + " " + tila + " " + nimi);
            level2.add(code);
        }else{
            // suuralue
            code = XMLID.toXMLID(kunta + " " + suur + " " + nimi);
            level3.add(code);
        }        
        names.put(code, nimi);
        
        // polygon
        if (placemark.getGeometry() instanceof Polygon){                    
            Polygon polygon = (Polygon)placemark.getGeometry();
            LinearRing ring = polygon.getOuterBoundaryIs().getLinearRing();            
            polygons.put(code, ring.getCoordinates());
            
        // center point
        }else if (placemark.getGeometry() instanceof Point){
            Coordinate coordinate = ((Point)placemark.getGeometry()).getCoordinates().get(0);
            centers.put(code, coordinate);
            
        }else{
            System.err.println(code + " has geometry of type " + placemark.getGeometry().getClass().getSimpleName());
        }
    }
    

    private static JSONArray toJSONArray(Object... objects){
        JSONArray array = new JSONArray();
        for (Object o : objects){
            array.add(o);
        }
        return array;
    }
    
    
}
