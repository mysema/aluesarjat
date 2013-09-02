package fi.aluesarjat.prototype;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.mutable.MutableInt;

public class LogAnalysis {
    
    public static void main(String[] args) throws IOException {
        LineIterator lines = FileUtils.lineIterator(new File("doc/alllogs"));
        Map<String, MutableInt> queries = new HashMap<String, MutableInt>();
//        Set<String> uniqueQueries = new HashSet<String>();
        while (lines.hasNext()) {
            String line = lines.nextLine();
            if (line.contains("/sparql?")) {
                // strip start
                line = line.substring(line.indexOf("query=") + 6);
                // strip end
                if (line.contains("HTTP/1.1")) {
                    line = line.substring(0, line.indexOf("HTTP/1.1")-1);    
                }                
                // url decode
                line = URLDecoder.decode(line, "UTF-8");
                // strip extra parameters
                if (line.contains("&")) {
                    line = line.substring(0, line.indexOf("&"));    
                }                
                // remove PREFIX lines
                line = line.replaceAll("PREFIX[^\n]+\n", "").replaceAll("&type=json", "");
                
                MutableInt counter = queries.get(line);
                if (counter == null) {
                    counter = new MutableInt(0);
                    queries.put(line, counter);
                }
                counter.increment();
            }            
        }
        lines.close();
        
        // print results
        for (Map.Entry<String, MutableInt> entry : queries.entrySet()) {
            System.out.println(entry.getKey());
            System.out.println("executed : " + entry.getValue() + " times");
            System.out.println();
        }
    }

}
