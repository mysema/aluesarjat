package fi.aluesarjat.prototype;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mysema.commons.lang.IteratorAdapter;
import com.mysema.rdfbean.model.DC;
import com.mysema.rdfbean.model.ID;
import com.mysema.rdfbean.model.Operation;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.RDFS;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.model.io.Format;
import com.mysema.rdfbean.model.io.RDFSource;
import com.mysema.rdfbean.sesame.MemoryRepository;

public class MergeWithTarinoidenHelsinki {
    
    public static void main(String[] args) throws IOException{
        MemoryRepository repository = new MemoryRepository();
        repository.setSources(
                new RDFSource("classpath:/area-titles.ttl", Format.TURTLE, "http://localhost:8080/rdf/dimensions/Alue"),
                new RDFSource("classpath:/ext/tarinoidenhelsinki.ttl", Format.TURTLE, "http://www.tarinoidenhelsinki.fi"));
        repository.initialize();
        
        try{            
            repository.execute(new Operation<Void>(){
                @Override
                public Void execute(RDFConnection connection) throws IOException {
                    Map<String, ID> areaTitles = new HashMap<String, ID>();
                    Map<String, ID> tarinaTitles = new HashMap<String, ID>();
                    List<STMT> stmts = new ArrayList<STMT>();
                    stmts.addAll(IteratorAdapter.asList(connection.findStatements(null, DC.title, null, null, false)));
                    stmts.addAll(IteratorAdapter.asList(connection.findStatements(null, RDFS.label, null, null, false)));
                    for (STMT stmt : stmts){
                        if (stmt.getSubject().getValue().startsWith("http://www.tarinoidenhelsinki.fi/resource/place")){
                            tarinaTitles.put(stmt.getObject().getValue(), stmt.getSubject());
                        }else if (stmt.getSubject().getValue().startsWith("http://localhost:8080/rdf/dimensions/Alue#")){
                            areaTitles.put(stmt.getObject().getValue(), stmt.getSubject());
                        }
                    }
                    
                    for (Map.Entry<String, ID> entry : tarinaTitles.entrySet()){
                        ID area = getByPrefix(areaTitles, entry.getKey());
                        if (area != null){
                            System.out.println(entry.getKey() + " -> " + area);
                        }else{
                            System.err.println(entry.getKey() + " -> " + entry.getValue());
                        }
                    }
                    
                    return null;
                }                
            });    
            
        }finally{
            repository.close();
        }
    }
    
    private static ID getByPrefix(Map<String, ID> subjects, String prefix){
        prefix = prefix.toLowerCase();
        for (Map.Entry<String, ID> entry : subjects.entrySet()){
            if (entry.getKey().toLowerCase().startsWith(prefix)){
                return entry.getValue();
            }
        }
        return null;
    }
    
}
