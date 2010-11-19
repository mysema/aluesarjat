package com.mysema.stat.scovo;

import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.sesame.MemoryRepository;

public class MemoryRepositoryDatasetHandlerTest extends AbstractDatasetHandlerTest{

    @Override
    protected Repository createRepository() {
        return new MemoryRepository();
    }

}
