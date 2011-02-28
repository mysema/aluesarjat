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

import com.mysema.commons.lang.Assert;
import com.mysema.commons.lang.IteratorAdapter;
import com.mysema.rdfbean.model.DC;
import com.mysema.rdfbean.model.Format;
import com.mysema.rdfbean.model.GEO;
import com.mysema.rdfbean.model.LIT;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.RDFConnectionCallback;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.model.UID;
import com.mysema.rdfbean.sesame.MemoryRepository;

import de.micromata.opengis.kml.v_2_2_0.*;

public class KMLRDFDump {

    private final Set<STMT> centerStmts = new HashSet<STMT>();

    private final Set<STMT> polygonStmts = new HashSet<STMT>();

    private final Map<String, UID> areas = new HashMap<String, UID>();

    private final Map<String,String> areaTitles = new HashMap<String,String>();

    private final Set<String> level1 = new HashSet<String>();

    private final Set<String> level2 = new HashSet<String>();

    private final Set<String> level3 = new HashSet<String>();

    private final Map<String,Coordinate> centers = new HashMap<String,Coordinate>();

    private final Map<String,List<Coordinate>> polygons = new HashMap<String,List<Coordinate>>();

    private final Map<String,MultiGeometry> multiGeometries = new HashMap<String,MultiGeometry>();

    public static void main(String[] args) throws IOException{
        new KMLRDFDump().init().handle().dumpRDF().dumpGEOJSON();
    }

    public KMLRDFDump init(){
        areaTitles.put("_091_533_Aluemeri", "Aluemeri");

        MemoryRepository repository = new MemoryRepository();
        repository.initialize();
        repository.load(Format.TURTLE, getClass().getResourceAsStream("/area-ids.ttl"), null, false);
        repository.load(Format.TURTLE, getClass().getResourceAsStream("/area-titles.ttl"), null, false);
        repository.load(Format.TURTLE, getClass().getResourceAsStream("/area-kauniainen.ttl"), null, false);

        try{
            repository.execute(new RDFConnectionCallback<Void>(){
                @Override
                public Void doInConnection(RDFConnection connection) throws IOException {
                    List<STMT> stmts = IteratorAdapter.asList(connection.findStatements(null, null, null, null, false));
                    if (stmts.isEmpty()){
                        throw new IllegalStateException("Got no areas");
                    }
                    for (STMT stmt : stmts){
                        if (stmt.getPredicate().equals(DC.identifier)){
                            areas.put(stmt.getObject().getValue(), stmt.getSubject().asURI());
                        }else{
                            areaTitles.put(stmt.getSubject().asURI().ln(), stmt.getObject().getValue());
                        }
                    }
                    return null;
                }

            });

        }finally{
            repository.close();
        }
        return this;
    }

    private KMLRDFDump handle() throws IOException {
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
        return this;
    }

    @SuppressWarnings("unchecked")
    private void dumpGEOJSON() throws IOException{
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
                List<Coordinate> value = polygons.get(code);
                if (value != null){
                    JSONArray coordinates = new JSONArray();
                    for (Coordinate coordinate : value){
                        coordinates.add(toJSONArray(coordinate.getLongitude(), coordinate.getLatitude()));
                    }
                    geometry.put("coordinates", toJSONArray(toJSONArray(coordinates))); // TODO : get rid of wrapping

                }else{
                    MultiGeometry multiGeometry = multiGeometries.get(code);
                    if (multiGeometry == null){
                        continue;
                    }
                    JSONArray array = new JSONArray();
                    for (Geometry geo : multiGeometry.getGeometry()){
                        JSONArray coordinates = new JSONArray();
                        for (Coordinate coordinate : ((Polygon)geo).getOuterBoundaryIs().getLinearRing().getCoordinates()){
                            coordinates.add(toJSONArray(coordinate.getLongitude(), coordinate.getLatitude()));
                        }
                        array.add(coordinates);
                    }
                    geometry.put("coordinates", toJSONArray(array)); // TODO : get rid of wrapping
                }
                feature.put("geometry", geometry);

                JSONObject properties = new JSONObject();
                properties.put("code", code);

                Coordinate centerPoint = centers.get(code);
                properties.put("center", toJSONArray(centerPoint.getLongitude(), centerPoint.getLatitude()));
                properties.put("name", Assert.notNull(areaTitles.get(code),"Got no title for " + code));
                feature.put("properties", properties);

                features.add(feature);
            }
            root.put("features",features);
            String str = root.toString();
            File out = new File("src/main/resources/area"+(counter++)+".json");
            FileUtils.writeStringToFile(out, str, "UTF-8");
        }
    }

    private KMLRDFDump dumpRDF() throws IOException{

        // centers
        RDFUtil.dump(centerStmts, new File("src/main/resources/area-centers.ttl"));

        // coordinates
        RDFUtil.dump(polygonStmts, new File("src/main/resources/area-polygons.ttl"));
        return this;
    }

    private void handlePlacemark(Placemark placemark) throws IOException{
        Map<String,String> values = new HashMap<String,String>();
        for (SchemaData schemaData : placemark.getExtendedData().getSchemaData()){
            for (SimpleData simpleData : schemaData.getSimpleData()){
                values.put(simpleData.getName(), simpleData.getValue());
            }
        }

        String tila = values.get("TILA");
        String pien = values.get("PIEN");
        Set<String> level;
        if (pien != null){
            level = level1;
        }else if (!StringUtils.isEmpty(tila)){
            level = level2;
        }else{
            level = level3;
        }

        UID area = areas.get(values.get("KOKOTUN"));
        if (area == null){
            System.err.println("Got no area for " + values.get("KOKOTUN"));
            return;
        }
        String code = area.ln();
        level.add(code);

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

            if (!polygons.containsKey(code)){
                polygonStmts.add(new STMT(area, GEO.polygon, new LIT(p.toString())));
                polygons.put(code, ring.getCoordinates());
            }


        }else if (placemark.getGeometry() instanceof MultiGeometry){
            if (!multiGeometries.containsKey(code)){
                multiGeometries.put(code, (MultiGeometry)placemark.getGeometry());
            }

        // center point
        }else if (placemark.getGeometry() instanceof Point){
            if (!centers.containsKey(code)){
                Coordinate coordinate = ((Point)placemark.getGeometry()).getCoordinates().get(0);
                centerStmts.add(new STMT(area, GEO.where, new LIT(coordinate.getLatitude()+","+coordinate.getLongitude())));
                centers.put(code, coordinate);
            }

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
