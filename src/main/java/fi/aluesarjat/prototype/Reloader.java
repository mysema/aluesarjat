/*
* Copyright 2013 Mysema Ltd
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
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
    public void initialize() throws IOException {
        if (datasetsList.startsWith("classpath")) {
            // no reload for classpath based lists
            return;
        }
        
        addJob(0, 0, new Runnable() {
            @Override
            public void run() {
                if (loading) {
                    logger.info("Still loading");
                    return;
                }                
                try {
                    loading = true;
                    long modified = new URL(datasetsList).openConnection().getLastModified();
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
        logger.info("Removing data");
        repository.execute(new RDFConnectionCallback<Void>() {
            @Override
            public Void doInConnection(RDFConnection connection) throws IOException {
                connection.remove(null, null, null, null);
                return null;
            }                            
        });
        
        // reload data
        logger.info("Reloading data");
        dataService.loadData(DataServiceMode.NONTHREADED);            
    }
    
    @PreDestroy
    public void stop() {
        scheduler.shutdown();
    }
    
    private void addJob(int hours, int minutes, Runnable runnable) {       
        DateTime dateTime = new DateTime();
        long minutesOfDay = dateTime.getMinuteOfDay();
        long scheduleTime = hours * 60 + minutes;
        long initialDelay;
        if (minutesOfDay < scheduleTime) {
            initialDelay = scheduleTime - minutesOfDay;
        } else {
            initialDelay = 24 * 60 - minutesOfDay + scheduleTime; 
        }
        System.err.println(initialDelay);
        scheduler.scheduleAtFixedRate(runnable, initialDelay, 24 * 60, TimeUnit.MINUTES);
    }
    
    
}
