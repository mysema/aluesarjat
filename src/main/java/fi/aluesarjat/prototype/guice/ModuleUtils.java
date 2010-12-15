package fi.aluesarjat.prototype.guice;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.mysema.rdfbean.model.RepositoryException;
import com.mysema.rdfbean.model.io.Format;
import com.mysema.rdfbean.model.io.RDFSource;
import com.mysema.stat.scovo.SCV;

public class ModuleUtils {

    public static RDFSource[] getSources(String baseURI) {
        return new RDFSource[]{
                getAreaDescriptions(baseURI),
                new RDFSource("classpath:/scovo.rdf", Format.RDFXML, SCV.NS),
                new RDFSource("classpath:/area-hierarchy.ttl", Format.TURTLE, baseURI + "dimensions/Alue"),
                new RDFSource("classpath:/area-coordinates.ttl", Format.TURTLE, baseURI + "dimensions/Alue"),
                new RDFSource("classpath:/stat.rdf", Format.RDFXML, "http://data.mysema.com/rdf/pcaxis#")};
    }
    
    private static RDFSource getAreaDescriptions(String baseURI){
        try {
            String str = IOUtils.toString(ModuleUtils.class.getResourceAsStream("/alue.ttl"), "ISO-8859-1");
            String normalized = str.replace("http://localhost:8080/rdf/", baseURI);
            return new RDFSource(new ByteArrayInputStream(normalized.getBytes("UTF-8")), Format.TURTLE, baseURI + "dimensions/Alue");
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }
}
