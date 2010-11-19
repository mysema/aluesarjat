package com.mysema.stat.scovo;

import java.io.File;

import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.sesame.NativeRepository;

public class NativeRepositoryDatasetHandlerTest extends AbstractDatasetHandlerTest{

    @Override
    protected Repository createRepository() {
        return new NativeRepository(new File("target/native"), false);
    }

}
