package fi.aluesarjat.prototype.guice;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.mysema.rdfbean.model.Format;
import com.mysema.rdfbean.model.RepositoryException;
import com.mysema.rdfbean.model.io.RDFSource;
import com.mysema.stat.scovo.SCV;

public final class ModuleUtils {
    
    public static final String DEFAULT_BASE_URI = "http://localhost:8080/rdf/";

    public static RDFSource[] getSources() {
        return getSources(DEFAULT_BASE_URI);
    }
    
    public static RDFSource[] getSources(String baseURI) {
        String dimensions = baseURI + "dimensions/";
        String alueContext = baseURI + "dimensions/Alue";
        RDFSource[] sources = new RDFSource[]{
            new RDFSource("classpath:/scovo.rdf", Format.RDFXML, SCV.NS),
            new RDFSource("classpath:/alue.ttl", Format.TURTLE, alueContext),
            new RDFSource("classpath:/area-hierarchy.ttl", Format.TURTLE, alueContext),
            new RDFSource("classpath:/area-kauniainen.ttl", Format.TURTLE, alueContext),
            new RDFSource("classpath:/area-centers.ttl", Format.TURTLE, alueContext),
            new RDFSource("classpath:/area-polygons.ttl", Format.TURTLE, alueContext),
            new RDFSource("classpath:/dimensions.ttl", Format.TURTLE, dimensions),
            new RDFSource("classpath:/stat.rdf", Format.RDFXML, "http://data.mysema.com/rdf/pcaxis#"),
            new RDFSource("classpath:/ext/dbpedia-comments.ttl", Format.TURTLE, "http://dbpedia.org"),
            new RDFSource("classpath:/ext/dbpedia-links.ttl", Format.TURTLE, "http://dbpedia.org"),
            new RDFSource("classpath:/ext/tarinoidenhelsinki.ttl", Format.TURTLE, "http://www.tarinoidenhelsinki.fi"),
            new RDFSource("classpath:/ext/tarinoidenhelsinki-links.ttl", Format.TURTLE, "http://www.tarinoidenhelsinki.fi")
            };
        
        if (!baseURI.equals(DEFAULT_BASE_URI)){
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
            String normalized = str.replace(DEFAULT_BASE_URI, baseURI);
            return new RDFSource(new ByteArrayInputStream(normalized.getBytes(encoding)), source.getFormat(), source.getContext());
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }
    
    private ModuleUtils(){}

    public static void main(String[] args){
        ModuleUtils.getSources("http://localhost:8080/rdf/");
    }
}
