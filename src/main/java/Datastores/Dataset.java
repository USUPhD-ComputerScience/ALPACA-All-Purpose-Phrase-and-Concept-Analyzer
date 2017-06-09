package Datastores;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Dataset {
	private int mID = -1;
	private boolean mHasTime = false;
	private boolean mHasRating = false;
	private String mDescription = null;
	private Set<Document> mDocSet = new HashSet<>();
	public Dataset(int iD, String name, String description, boolean has_time,
			boolean has_rating) {
		// TODO Auto-generated constructor stub
		mID = iD;
		mHasRating = has_rating;
		mHasTime = has_time;
		mDescription = description;
	}

	public boolean hasTime(){
		return mHasTime;
	}
	public boolean hasRating(){
		return mHasRating;
	}
	public int getID() {
		// TODO Auto-generated method stub
		return mID;
	}
	public String getDescription (){
		return mDescription;
	}
	public boolean addDocument(Document doc) {
		// TODO Auto-generated method stub
		return mDocSet.add(doc);
	}
	

}
