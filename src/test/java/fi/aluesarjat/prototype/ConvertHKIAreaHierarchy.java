package fi.aluesarjat.prototype;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.mysema.stat.scovo.XMLID;

public class ConvertHKIAreaHierarchy {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        try {
            List<String> lines = IOUtils.readLines(getStream("/HKI-Aluehierarkia.csv"), "UTF-8");

            System.out.println(
                    "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                    "@prefix dc: <http://purl.org/dc/elements/1.1/> .\n" +
                    "@prefix alue: <http://localhost:8080/rdf/dimensions/Alue#> .\n" +
                    "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
            );
            Set<String> seen = new HashSet<String>();
            for (String line : lines) {
                String[] areas = line.split(";");
                for (int i=1; i < areas.length; i++) {
                    if (seen.add(areas[i-1] + " - " + areas[i])) {
                        System.out.println("alue:" + XMLID.toXMLID(areas[i]) + " skos:broader alue:" + XMLID.toXMLID(areas[i-1]) + " .");
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static InputStream getStream(String name) {
        return ConvertHKIAreaHierarchy.class.getResourceAsStream(name);
    }

}
