package com.mysema.stat.scovo;

import com.mysema.rdfbean.sesame.MemoryRepository;
import com.mysema.rdfbean.sesame.SesameRepository;

public class MemoryRepositoryDatasetHandlerTest extends AbstractDatasetHandlerTest{

    @Override
    protected SesameRepository createRepository() {
        return new MemoryRepository();
    }

}
