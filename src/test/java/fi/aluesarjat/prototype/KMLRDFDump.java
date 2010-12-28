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

import com.mysema.rdfbean.model.RDF;
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
    
    // TODO : dump boundaries as well ?!?
    
    public static void main(String[] args) throws IOException{
        StringWriter levels = new StringWriter();
        StringWriter centers = new StringWriter();
        StringWriter polygons = new StringWriter();

        levels.append("@prefix rdf: <" + RDF.NS + "> . \n");
        levels.append("@prefix dimension: <http://localhost:8080/rdf/dimensions/> .\n");
        levels.append("@prefix alue: <http://localhost:8080/rdf/dimensions/Alue#> .\n");
        levels.append("\n");
        
        centers.append("@prefix alue: <http://localhost:8080/rdf/dimensions/Alue#> .\n");
        centers.append("@prefix geo: <http://www.w3.org/2003/01/geo/> .\n");
        centers.append("\n");
        
        polygons.append("@prefix alue: <http://localhost:8080/rdf/dimensions/Alue#> .\n");
        polygons.append("@prefix geo: <http://www.w3.org/2003/01/geo/> .\n");
        polygons.append("\n");
        
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
                                    handlePlacemark(centers, polygons, levels, (Placemark)folderFeature);                            
                                }
                            }
                        }
                    }    
                }    
            }finally{
                is.close();
            }
                
        }   
        
        // levels
//        File target = new File("src/main/resources/area-levels.ttl");
//        FileUtils.writeStringToFile(target, levels.toString(), "UTF-8");

        // centers
        File target = new File("src/main/resources/area-centers.ttl");
        FileUtils.writeStringToFile(target, centers.toString(), "UTF-8");
        
        // coordinates
        target = new File("src/main/resources/area-polygons.ttl");
        FileUtils.writeStringToFile(target, polygons.toString(), "UTF-8");
    }

    
    private static void handlePlacemark(Writer centers, Writer polygons, Writer levels, Placemark placemark) throws IOException{        
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
        String level = null;
        if (!StringUtils.isEmpty(pien)){
            // pienialue
            code = XMLID.toXMLID(kunta + " " + pien + " " + nimi);
            level = "1";
        }else if (!StringUtils.isEmpty(tila)){
            // ?!?
            code = XMLID.toXMLID(kunta + " " + tila + " " + nimi);
            level = "2";
        }else{
            // suuralue
            code = XMLID.toXMLID(kunta + " " + suur + " " + nimi);
            level = "3";
        }
        
        // polygon
        if (placemark.getGeometry() instanceof Polygon){            
            StringBuilder p = new StringBuilder();            
            Polygon polygon = (Polygon)placemark.getGeometry();
            LinearRing ring = polygon.getOuterBoundaryIs().getLinearRing();
            for (Coordinate coordinate : ring.getCoordinates()){
                if (p.length() > 0){
                    p.append(" ");
                }
                p.append(coordinate.getLatitude()).append(",").append(coordinate.getLongitude());
            }       
            
            polygons.append("alue:"+code+" geo:polygon \""+p+ "\" . \n");
            
        // center point
        }else if (placemark.getGeometry() instanceof Point){
            Coordinate coordinate = ((Point)placemark.getGeometry()).getCoordinates().get(0);
            levels.append("alue:" + code + " alue:level " + level + " . \n");
            centers.append("alue:"+code+" geo:where \""+coordinate.getLatitude()+","+coordinate.getLongitude()+ "\" . \n");
            
        }else{
            System.err.println(code + " has geometry of type " + placemark.getGeometry().getClass().getSimpleName());
        }
        

    }
    
}
