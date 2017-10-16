package Vocabulary;

import Utils.POSTagConverter;

public class DBWord {
	private byte mPOS;
	private int mID;
	private String mText;
	private int mCount;

	public DBWord(int dBID, String text, byte pOS,
			int total_count) {
		// TODO Auto-generated constructor stub
		mID = dBID;
		mText = text.intern();
		mPOS = pOS;
		mCount = total_count;
	}

	public String toString() {
		return mText + "_" + POSTagConverter.getInstance().getTag(mPOS);
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

	public int getCount() {
		return mCount;
	}

	public void incrementCount() {
		mCount++;
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
		if (mPOS == wordFromObj.mPOS && mText == wordFromObj.mText)
			return true;
		else
			return false;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return mText.hashCode();
	}

//	public void updateCountToDB(int level) throws ClassNotFoundException, SQLException {
//		// TODO Auto-generated method stub
//
//		DocumentDatasetDB db = DocumentDatasetDB.getInstance();
//
//		switch (level) {
//		case DocumentDatasetDB.LV1_SPELLING_CORRECTION:
//			db.updateKeyWordCount(mID, mCountLV1, level);
//			break;
//		case DocumentDatasetDB.LV2_ROOTWORD_STEMMING:
//			db.updateKeyWordCount(mID, mCountLV2, level);
//			break;
//		case DocumentDatasetDB.LV3_OVER_STEMMING:
//			db.updateKeyWordCount(mID, mCountLV3, level);
//			break;
//		case DocumentDatasetDB.LV4_ROOTWORD_STEMMING_LITE:
//			db.updateKeyWordCount(mID, mCountLV4, level);
//			break;
//		}
//		
//	}
}
