package fi.aluesarjat.prototype;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Stack;

import com.mysema.rdfbean.virtuoso.VirtuosoRepository;
import com.mysema.stat.scovo.NamespaceHandler;

import fi.aluesarjat.prototype.guice.ModuleUtils;

public class Loader {
   
    public static void main(String[] args) throws IOException{
        VirtuosoRepository repository = null;
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
        
        // collect PX files
        Stack<File> unhandled = new Stack<File>();
        unhandled.push(new File(dataDir));
        List<String> datasets = new ArrayList<String>();
        while (!unhandled.isEmpty()){
            File file = unhandled.pop();
            if (file.isDirectory()){
                if (file.listFiles() != null){
                    unhandled.addAll(Arrays.asList(file.listFiles()));    
                }                
            }else if (file.getName().endsWith(".px")){
                datasets.add(file.toURI().toURL().toString());
            }
        }

        // initialize repository
        repository = new VirtuosoRepository(host+":"+port, user, pass);
        repository.setSources(ModuleUtils.getSources(baseURI));
        repository.initialize();
        
        // load PX files
        try{            
            NamespaceHandler namespaceHandler = new NamespaceHandler(repository);
            DataService dataService = new DataService(repository, namespaceHandler, baseURI, DataService.Mode.NONTHREADED.name(), "true");
            dataService.setDatasets(datasets); 
            dataService.initialize();
            
        // close repository
        }finally{
            if (repository != null){
                repository.close();
            }
        }
        
    }
}
