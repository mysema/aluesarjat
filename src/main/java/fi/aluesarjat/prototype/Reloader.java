package fi.aluesarjat.prototype;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.RDFConnectionCallback;
import com.mysema.rdfbean.model.Repository;

public class Reloader {

    private static final Logger logger = LoggerFactory.getLogger(Reloader.class);
    
    private final Repository repository;
    
    private final DataService dataService;
    
    private final String datasetsList;
    
    private final ScheduledExecutorService scheduler;
    
    private long lastModified = System.currentTimeMillis();
    
    private boolean loading = false;
    
    @Inject
    public Reloader(Repository repository, DataService dataService, @Named("datasets.list") String datasetsList) {
        this.repository = repository;
        this.dataService = dataService;
        this.datasetsList = datasetsList;
        this.scheduler = Executors.newScheduledThreadPool(3);
    }
    
    @PostConstruct
    public void initialize() throws IOException{
        if (datasetsList.startsWith("classpath")){
            // no reload for classpath based lists
            return;
        }
        
        addJob(12, 0, new Runnable() {
            @Override
            public void run() {
                if (loading) {
                    return;
                }                
                try {
                    loading = true;
                    long modified = new URL("datasetsList").openConnection().getLastModified();
                    if (modified > lastModified) {
                        reload();
                        lastModified = modified;                
                    } else {
                        logger.info("Skipped reloading");                        
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    loading = false;
                }
            }            
        });
    }
    
    private void reload() throws IOException {        
        // remove all data
        repository.execute(new RDFConnectionCallback<Void>(){
            @Override
            public Void doInConnection(RDFConnection connection) throws IOException {
                connection.remove(null, null, null, null);
                return null;
            }                            
        });
        
        // reload data
        dataService.loadData(DataService.Mode.NONTHREADED);            
    }
    
    @PreDestroy
    public void stop(){
        scheduler.shutdown();
    }
    
    private void addJob(int hours, int minutes, Runnable runnable){       
        DateTime dateTime = new DateTime();
        long minutesOfDay = dateTime.getMinuteOfDay();
        long scheduleTime = hours * 60 + minutes;
        long initialDelay;
        if (minutesOfDay < scheduleTime){
            initialDelay = minutesOfDay - scheduleTime;
        }else{
            initialDelay = 24 * 60 - minutesOfDay + scheduleTime; 
        }
        scheduler.scheduleAtFixedRate(runnable, initialDelay, 24 * 60, TimeUnit.MINUTES);
    }
    
    
}
