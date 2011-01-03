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
        RDFSource[] sources = new RDFSource[]{
            new RDFSource("classpath:/scovo.rdf", Format.RDFXML, SCV.NS),
            new RDFSource("classpath:/alue.ttl", Format.TURTLE, baseURI + "dimension/Alue"),
            new RDFSource("classpath:/area-hierarchy.ttl", Format.TURTLE, baseURI + "dimensions/Alue"),
            new RDFSource("classpath:/area-kauniainen.ttl", Format.TURTLE, baseURI + "dimensions/Alue"),
            new RDFSource("classpath:/area-centers.ttl", Format.TURTLE, baseURI + "dimensions/Alue"),
            new RDFSource("classpath:/area-polygons.ttl", Format.TURTLE, baseURI + "dimensions/Alue"),
            new RDFSource("classpath:/stat.rdf", Format.RDFXML, "http://data.mysema.com/rdf/pcaxis#"),
            new RDFSource("classpath:/ext/dbpedia-comments.ttl", Format.TURTLE, "http://dbpedia.org"),
            new RDFSource("classpath:/ext/dbpedia-links.ttl", Format.TURTLE, "http://dbpedia.org")};
        
        if (!baseURI.equals("http://localhost:8080/rdf/")){
            for (int i = 0; i < sources.length; i++){
                sources[i] = transform(sources[i], baseURI);
            }
        }        
        return sources;
    }
    
    private static RDFSource transform(RDFSource source, String baseURI) {
        try {
            String encoding = source.getFormat().equals(Format.NTRIPLES) ? "US-ASCII" : "UTF-8";
            String str = IOUtils.toString(source.openStream(), encoding);
            String normalized = str.replace("http://localhost:8080/rdf/", baseURI);
            return new RDFSource(new ByteArrayInputStream(normalized.getBytes(encoding)), source.getFormat(), source.getContext());
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

}
