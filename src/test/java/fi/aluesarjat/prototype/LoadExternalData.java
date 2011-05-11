package fi.aluesarjat.prototype;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.virtuoso.VirtuosoRepository;
import com.mysema.stat.scovo.NamespaceHandler;

public class LoadExternalData {
    
    public static void main(String[] args) throws IOException {
        Properties properties = new Properties();
        properties.load(Loader.class.getResourceAsStream("/aluesarjat.properties"));
        String baseURI = properties.getProperty("baseURI");
        
        Repository repository = new VirtuosoRepository("localhost:1111", "dba", "dba");
        repository.initialize();
        NamespaceHandler namespaceHandler = new NamespaceHandler(repository);
        
        try{
            File file = new File("src/test/resources/pc-axis.txt");
            DataService dataService = new DataService(repository, namespaceHandler, baseURI, DataServiceMode.NONTHREADED, true, file.toURI().toURL().toString());
            dataService.initialize();    
        } finally {
            repository.close();
        }
        
        
    }

}
