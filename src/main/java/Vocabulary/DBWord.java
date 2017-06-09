package Vocabulary;

import Datastores.DocumentDatasetDB;

public class DBWord {
	private byte mPOS;
	private int mID;
	private String mText;
	private int mDatasetID;
	private int mCountLV1;
	private int mCountLV2;
	private int mCountLV3;

	public DBWord(int dBID, String text, byte pOS, int datasetID,
			int total_count_LV1, int total_count_LV2, int total_count_LV3) {
		// TODO Auto-generated constructor stub
		mID = dBID;
		mText = text.intern();
		mPOS = pOS;
		mDatasetID = datasetID;
		mCountLV1 = total_count_LV1;
		mCountLV2 = total_count_LV2;
		mCountLV3 = total_count_LV3;
	}

	public int getDatasetID() {
		return mDatasetID;
	}

	public int getID() {
		return mID;
	}

	public byte getPOS() {
		return mPOS;
	}

	public String getText() {
		return mText;
	}

	public int getCount(int level) {
		switch (level) {
		case DocumentDatasetDB.LV1_SPELLING_CORRECTION:
			return mCountLV1;
		case DocumentDatasetDB.LV2_ROOTWORD_STEMMING:
			return mCountLV2;
		case DocumentDatasetDB.LV3_OVER_STEMMING:
			return mCountLV3;
		}
		return -1;
	}

	public void incrementCount(int level) {
		switch (level) {
		case DocumentDatasetDB.LV1_SPELLING_CORRECTION:
			mCountLV1++;
		case DocumentDatasetDB.LV2_ROOTWORD_STEMMING:
			mCountLV2++;
		case DocumentDatasetDB.LV3_OVER_STEMMING:
			mCountLV3++;
		}
	}
}
