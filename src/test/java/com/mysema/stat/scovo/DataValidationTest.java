package com.mysema.stat.scovo;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.model.GEO;
import com.mysema.rdfbean.model.ID;
import com.mysema.rdfbean.model.RDF;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.model.UID;
import com.mysema.rdfbean.model.io.Format;
import com.mysema.rdfbean.sesame.MemoryRepository;
import com.mysema.rdfbean.sesame.SesameRepository;

import fi.aluesarjat.prototype.guice.ModuleUtils;

public class DataValidationTest {
    
    private final String baseURI = ModuleUtils.DEFAULT_BASE_URI;

    private SesameRepository repository;

    private RDFConnection connection;
    
    @Before
    public void setUp(){
        repository = new MemoryRepository();
        repository.setSources(ModuleUtils.getSources(baseURI));
        repository.initialize();
        repository.load(Format.TURTLE, getClass().getResourceAsStream("/areas.ttl"), null, false);
        repository.load(Format.TURTLE, getClass().getResourceAsStream("/area-polygons.ttl"), null, false);
    }

    @After
    public void tearDown(){
        if (connection != null){
            connection.close();
        }
        repository.close();
    }
    
    @Test
    public void Validate() throws IOException{       
        connection = repository.openConnection();
        UID alueType = new UID(baseURI + "dimensions/", "Alue");
        CloseableIterator<STMT> stmts = connection.findStatements(null, RDF.type, alueType, null, false);
        Set<ID> missing = new HashSet<ID>();
        try{
            while (stmts.hasNext()){
                STMT stmt = stmts.next();
                CloseableIterator<STMT> polygons = connection.findStatements(stmt.getSubject(), GEO.polygon, null, null, false);
                try{
                    if (!polygons.hasNext()){
                        missing.add(stmt.getSubject());
                    }
                }finally{
                    polygons.close();
                }
            }          
            
        }finally{
            stmts.close();
        }
                
        for (ID id : missing){
            System.err.println("No polygons for " + id);
        }
//        assertTrue("Got "+missing.size()+" missing polygons", missing.isEmpty());
    }
    
}
