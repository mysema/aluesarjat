package com.mysema.stat.pcaxis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultDatasetHandler implements DatasetHandler {

    private Map<Dataset, List<Item>> datasets = new HashMap<Dataset, List<Item>>();
    
    private Set<String> ignoredValues = new HashSet<String>(Arrays.asList(
            "\".\""
    ));
        
    @Override
    public void addDataset(Dataset dataset) {
        datasets.put(dataset, new ArrayList<Item>());
    }

    @Override
    public void addItem(Item item) {
        if (!ignoredValues.contains(item.getValue())) {
            datasets.get(item.getDataset()).add(item);
        }
    }

    @Override
    public void begin() {
    }
    
    @Override
    public void commit() {
    }
    
    public Dataset getDataset(String name) {
        for (Dataset dataset : datasets.keySet()) {
            if (dataset.getName().equals(name)) {
                return dataset;
            }
        }
        return null;
    }

    public List<Item> getItems(Dataset dataset) {
        return datasets.get(dataset);
    }

    public List<Item> getItems(String datasetName) {
        return getItems(getDataset(datasetName));
    }

    @Override
    public void rollback() {
    }
}
