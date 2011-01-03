package fi.aluesarjat.prototype;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.model.io.TurtleStringWriter;

public final class RDFUtil {
    
    public static void dump(Set<STMT> stmts, File file) throws IOException {
        TurtleStringWriter writer = new TurtleStringWriter();
        writer.begin();
        for (STMT stmt : stmts){
            writer.handle(stmt);
        }
        writer.end();
        
        file.createNewFile();
        FileUtils.writeStringToFile(file, writer.toString(), "UTF-8");
    }
    
    private RDFUtil(){}

}
