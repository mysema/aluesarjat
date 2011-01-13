package fi.aluesarjat.prototype;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.Stack;

import org.apache.commons.io.FileUtils;

/**
 * Dumps a datasets file for the contents declared in classpath:/loader.propertiesto target/datasets.dump
 *
 */
public class LoaderDatasetsDump {
   
    public static void main(String[] args) throws IOException{
        Properties properties = new Properties();
        InputStream in = LoaderDatasetsDump.class.getResourceAsStream("/loader.properties");
        if (in == null){
            throw new IllegalArgumentException("Make sure classpath:/loader.properties is available");
        }
        properties.load(in);      
        String dataDir = properties.getProperty("data.dir");
        
        // collect PX files
        Stack<File> unhandled = new Stack<File>();
        unhandled.push(new File(dataDir));
        StringBuilder builder = new StringBuilder();
        while (!unhandled.isEmpty()){
            File file = unhandled.pop();
            if (file.isDirectory()){
                if (file.listFiles() != null){
                    unhandled.addAll(Arrays.asList(file.listFiles()));    
                }                
            }else if (file.getName().endsWith(".px")){
                builder.append(file.toURI().toURL().toString() + " \".\"\n");
            }
        }
        
        File file = new File("target/datasets.dump");        
        FileUtils.writeStringToFile(file, builder.toString(), "UTF-8");

    }
}
