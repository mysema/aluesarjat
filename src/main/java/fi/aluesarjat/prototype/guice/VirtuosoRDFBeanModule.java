package fi.aluesarjat.prototype.guice;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.mysema.rdfbean.guice.Config;
import com.mysema.rdfbean.guice.RDFBeanRepositoryModule;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.virtuoso.VirtuosoRepository;
import com.mysema.rdfbean.virtuoso.VirtuosoRepositoryConnection;
import com.mysema.stat.scovo.ScovoDatasetHandler;

public class VirtuosoRDFBeanModule extends RDFBeanRepositoryModule{

    @Override
    public List<String> getConfiguration(){
        return Arrays.asList("/aluesarjat.properties", "/import-serial.properties");
    }

    @Override
    public Repository createRepository(@Config Properties properties){
        File dataDir = new File(System.getProperty("java.io.tmpdir"), "aluesarjat-virtuoso");
        dataDir.mkdir();
        String baseURI = properties.getProperty("baseURI");        
        String hostAndPort = properties.getProperty("virtuoso.host") +":"+properties.getProperty("virtuoso.port");
        String user = properties.getProperty("virtuoso.user");
        String pass = properties.getProperty("virtuoso.pass");
        String dimensionNs = baseURI + ScovoDatasetHandler.DIMENSION_NS;
        
        final VirtuosoRepository repository = new VirtuosoRepository(hostAndPort, user, pass, baseURI);
        repository.setSources(ModuleUtils.getSources(baseURI));
        repository.initialize();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                repository.close();
            }
        });

        // Initialize inference
        VirtuosoRepositoryConnection conn = (VirtuosoRepositoryConnection) repository.openConnection();
        try {
            // Borrow sql connection
            Connection sqlconn = conn.getConnection();
            CallableStatement stmt = sqlconn.prepareCall("rdfs_rule_set (?, ?, 0)");
            try {
                stmt.setString(1, "dimensions");
                stmt.setString(2, dimensionNs);
                stmt.execute();    
            } finally {
                stmt.close();
            }                
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            conn.close();
        }

        return repository;
    }

}
