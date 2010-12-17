package fi.aluesarjat.prototype;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

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

public class KMLRDFDump {
    
    public static void main(String[] args) throws IOException{
        StringWriter writer = new StringWriter();
        
        writer.append("@prefix alue: <http://localhost:8080/rdf/dimensions/Alue#> .\n");
        writer.append("@prefix geo: <http://www.w3.org/2003/01/geo/> .\n");
        writer.append("\n");
        
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
                                    handlePlacemark(writer, (Placemark)folderFeature);                            
                                }
                            }
                        }
                    }    
                }    
            }finally{
                is.close();
            }
                
        }   
        
        File target = new File("src/main/resources/area-coordinates.ttl");
        FileUtils.writeStringToFile(target, writer.toString(), "UTF-8");
    }

    
    private static void handlePlacemark(Writer writer, Placemark placemark) throws IOException{        
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
            code = XMLID.toXMLID(kunta + " " + pien + " " + nimi);
        }else if (!StringUtils.isEmpty(tila)){
            code = XMLID.toXMLID(kunta + " " + tila + " " + nimi);
        }else{
            code = XMLID.toXMLID(kunta + " " + suur + " " + nimi);
        }
        
        // polygon
        if (placemark.getGeometry() instanceof Polygon){            
            StringBuilder polygons = new StringBuilder();            
            Polygon polygon = (Polygon)placemark.getGeometry();
            LinearRing ring = polygon.getOuterBoundaryIs().getLinearRing();
            for (Coordinate coordinate : ring.getCoordinates()){
                if (polygons.length() > 0){
                    polygons.append(" ");
                }
                polygons.append(coordinate.getLatitude()).append(",").append(coordinate.getLongitude());
            }       
            writer.append("alue:"+code+" geo:polygon \""+polygons+ "\" . \n");
            
        // center point
        }else if (placemark.getGeometry() instanceof Point){
            Coordinate coordinate = ((Point)placemark.getGeometry()).getCoordinates().get(0);
            writer.append("alue:"+code+" geo:where \""+coordinate.getLatitude()+","+coordinate.getLongitude()+ "\" . \n");
            
        }else{
            System.err.println(code + " has geometry of type " + placemark.getGeometry().getClass().getSimpleName());
        }
        

    }
    
}
