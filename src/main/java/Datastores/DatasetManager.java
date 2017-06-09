package Datastores;

import java.util.HashSet;
import java.util.Set;

public class DatasetManager {
	private static DatasetManager instance = null;
	private Set<Dataset> mDatasetCollection = new HashSet<>();
	public static DatasetManager getInstance(){
		if(instance == null)
			instance = new DatasetManager();
		return instance;
	}
	private DatasetManager(){
		
	}
	public void addDataset(Dataset dataset){
		mDatasetCollection.add(dataset);
	}
}
