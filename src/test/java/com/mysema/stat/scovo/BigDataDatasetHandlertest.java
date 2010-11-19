package com.mysema.stat.scovo;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import com.bigdata.rdf.axioms.NoAxioms;
import com.bigdata.rdf.sail.BigdataSail;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.RepositoryException;

import fi.aluesarjat.prototype.guice.BigDataSesameRepository;

public class BigDataDatasetHandlertest extends AbstractDatasetHandlerTest{

    @Override
    protected Repository createRepository() {
        try {
            File dataDir = new File("target/bigdata");
            if (dataDir.exists()){
                FileUtils.cleanDirectory(dataDir);
            }
            dataDir.mkdir();
            File journal = new File(dataDir, "bigdata.jnl");
            journal.createNewFile();

            Properties properties = new Properties();
            properties.setProperty(BigdataSail.Options.FILE, journal.getAbsolutePath());
            properties.setProperty(BigdataSail.Options.TRUTH_MAINTENANCE, "false");
            properties.setProperty(BigdataSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
            properties.setProperty(BigdataSail.Options.QUADS, "true");
            return new BigDataSesameRepository(dataDir, properties);
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

}
