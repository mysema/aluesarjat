package com.mysema.stat.pcaxis;

/**
 * @author sasa
 *
 */
public interface DatasetHandler {

    void addDataset(Dataset dataset);

    void addItem(Item item);

    void begin();

    void rollback();

    void commit();

}
