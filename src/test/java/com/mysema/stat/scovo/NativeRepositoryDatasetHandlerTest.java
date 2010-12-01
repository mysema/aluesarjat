package com.mysema.stat.scovo;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.mysema.rdfbean.sesame.NativeRepository;
import com.mysema.rdfbean.sesame.SesameRepository;

public class NativeRepositoryDatasetHandlerTest extends AbstractDatasetHandlerTest{

    @Override
    protected SesameRepository createRepository() {
        try {
            File dataDir = new File("target/native");
            FileUtils.cleanDirectory(dataDir);
            return new NativeRepository(dataDir, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
