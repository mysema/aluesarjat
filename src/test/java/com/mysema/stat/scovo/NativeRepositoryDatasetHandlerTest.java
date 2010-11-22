package com.mysema.stat.scovo;

import java.io.File;

import com.mysema.rdfbean.sesame.NativeRepository;
import com.mysema.rdfbean.sesame.SesameRepository;

public class NativeRepositoryDatasetHandlerTest extends AbstractDatasetHandlerTest{

    @Override
    protected SesameRepository createRepository() {
        return new NativeRepository(new File("target/native"), false);
    }

}
