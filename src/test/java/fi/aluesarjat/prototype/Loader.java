package fi.aluesarjat.prototype;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import com.mysema.rdfbean.virtuoso.VirtuosoRepository;
import com.mysema.stat.pcaxis.PCAxisParser;
import com.mysema.stat.scovo.NamespaceHandler;
import com.mysema.stat.scovo.RDFDatasetHandler;

import fi.aluesarjat.prototype.guice.ModuleUtils;

public class Loader {
   
    public static void main(String[] args) throws IOException{
        VirtuosoRepository repository;
        String host, port, user, pass, baseURI, dataDir;
        
        Properties properties = new Properties();
        InputStream in = Loader.class.getResourceAsStream("/loader.properties");
        if (in == null){
            throw new IllegalArgumentException("Make sure classpath:/loader.properties is available");
        }
        properties.load(in);
        
        // get properties
        host = properties.getProperty("virtuoso.host");
        port = properties.getProperty("virtuoso.port");
        user = properties.getProperty("virtuoso.user");
        pass = properties.getProperty("virtuoso.pass");
        baseURI = properties.getProperty("baseURI");
        dataDir = properties.getProperty("data.dir");
        
        // initial repository        
        repository = new VirtuosoRepository(host+":"+port, user, pass);
        repository.setSources(ModuleUtils.getSources(baseURI));
        repository.initialize();
        
        try{
            // collect PX files
            Stack<File> unhandled = new Stack<File>();
            unhandled.push(new File(dataDir));
            Set<File> files = new HashSet<File>();
            while (!unhandled.isEmpty()){
                File file = unhandled.pop();
                if (file.isDirectory()){
                    if (file.listFiles() != null){
                        unhandled.addAll(Arrays.asList(file.listFiles()));    
                    }                
                }else if (file.getName().endsWith(".px")){
                    files.add(file);
                }
            }
           
            // load data
            NamespaceHandler namespaceHandler = new NamespaceHandler(repository);
            RDFDatasetHandler handler = new RDFDatasetHandler(repository, namespaceHandler, baseURI);
            PCAxisParser parser = new PCAxisParser(handler);
            for (File file : files){
                String datasetName = file.getName().substring(0, file.getName().length()-3);
                InputStream fileIn = new FileInputStream(file);
                try{
                    parser.parse(datasetName, fileIn);    
                } catch(Exception e){   
                    throw new RuntimeException("Exception when processing " + file.getPath(), e);
                } finally{
                    fileIn.close();
                }            
            }
        }finally{
            if (repository != null){
                repository.close();
            }
        }
        
    }
}
