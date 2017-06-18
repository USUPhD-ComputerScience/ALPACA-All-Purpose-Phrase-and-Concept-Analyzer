package Vocabulary;

import java.sql.SQLException;

import Datastores.Document;
import Datastores.DocumentDatasetDB;
import Utils.POSTagConverter;

public class DBWord {
	private byte mPOS;
	private int mID;
	private String mText;
	private int mDatasetID;
	private int mCountLV1;
	private int mCountLV2;
	private int mCountLV3;
	private int mCountLV4;

	public DBWord(int dBID, String text, byte pOS, int datasetID,
			int total_count_LV1, int total_count_LV2, int total_count_LV3,
			int total_count_LV4) {
		// TODO Auto-generated constructor stub
		mID = dBID;
		mText = text.intern();
		mPOS = pOS;
		mDatasetID = datasetID;
		mCountLV1 = total_count_LV1;
		mCountLV2 = total_count_LV2;
		mCountLV3 = total_count_LV3;
		mCountLV4 = total_count_LV4;
	}

	public String toString() {
		return mText + "_" + POSTagConverter.getInstance().getTag(mPOS);
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
		case DocumentDatasetDB.LV4_ROOTWORD_STEMMING_LITE:
			return mCountLV4;
		
		}
		return -1;
	}

	public void incrementCount(int level) {
		switch (level) {
		case DocumentDatasetDB.LV1_SPELLING_CORRECTION:
			mCountLV1++;
			break;
		case DocumentDatasetDB.LV2_ROOTWORD_STEMMING:
			mCountLV2++;
			break;
		case DocumentDatasetDB.LV3_OVER_STEMMING:
			mCountLV3++;
			break;
		case DocumentDatasetDB.LV4_ROOTWORD_STEMMING_LITE:
			mCountLV4++;
			break;
		}
	}

	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof DBWord))
			return false;
		DBWord wordFromObj = (DBWord) obj;
		if (mPOS == wordFromObj.mPOS && mText == wordFromObj.mText
				&& mDatasetID == wordFromObj.mDatasetID)
			return true;
		else
			return false;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return mText.hashCode();
	}

	public void updateCountToDB(int level) throws ClassNotFoundException, SQLException {
		// TODO Auto-generated method stub

		DocumentDatasetDB db = DocumentDatasetDB.getInstance();

		switch (level) {
		case DocumentDatasetDB.LV1_SPELLING_CORRECTION:
			db.updateKeyWordCount(mID, mCountLV1, level);
			break;
		case DocumentDatasetDB.LV2_ROOTWORD_STEMMING:
			db.updateKeyWordCount(mID, mCountLV2, level);
			break;
		case DocumentDatasetDB.LV3_OVER_STEMMING:
			db.updateKeyWordCount(mID, mCountLV3, level);
			break;
		case DocumentDatasetDB.LV4_ROOTWORD_STEMMING_LITE:
			db.updateKeyWordCount(mID, mCountLV4, level);
			break;
		}
		
	}
}
