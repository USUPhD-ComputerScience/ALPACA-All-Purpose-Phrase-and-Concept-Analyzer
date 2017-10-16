package Datastores;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import Vocabulary.Vocabulary;

public class Dataset {
	private String mDirectory = null;
	private boolean mHasAuthor = false;
	private boolean mHasTime = false;
	private boolean mHasRating = false;
	private String mOtherMetadata = null;
	private String mDescription = null;
	private String mName = null;
	private int mLevel = -1;
	private Vocabulary mVoc = null;
	private Set<Document> mDocSet = new HashSet<>();

	public Dataset(String name, String description, boolean has_time,
			boolean has_rating, boolean has_author, String meta,
			String directory, int level) {
		// TODO Auto-generated constructor stub
		mName = name;
		mHasRating = has_rating;
		mHasTime = has_time;
		mDescription = description;
		mHasAuthor = has_author;
		mOtherMetadata = meta;
		mDirectory = directory;
		mLevel = level;
		mVoc = new Vocabulary(this, level);
	}

	public String getOtherMetadata(){
		return mOtherMetadata;
	}
	public String getName() {
		return mName;
	}

	public int getLevel() {
		return mLevel;
	}

	public Vocabulary getVocabulary() {
		return mVoc;
	}

	public String getDirectory() {
		return mDirectory;
	}

	public boolean hasTime() {
		return mHasTime;
	}

	public boolean hasRating() {
		return mHasRating;
	}

	public String getDescription() {
		return mDescription;
	}

	public boolean addDocument(Document doc) {
		// TODO Auto-generated method stub
		return mDocSet.add(doc);
	}

	public Set<Document> getDocumentSet() {
		return mDocSet;
	}

	public boolean hasAuthor() {
		// TODO Auto-generated method stub
		return mHasAuthor;
	}

}
