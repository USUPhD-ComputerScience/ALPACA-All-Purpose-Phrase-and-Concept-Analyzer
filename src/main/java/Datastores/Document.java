package Datastores;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;


public class Document {
	private int mID = -1;
	private String mRawText = null;
	private int mRating = -1;
	private long mTime = -1;
	private int[][] sentences;
	private byte[][] postag;	
	private int mLevel; // lv1, 2 or 3 of cleaned text
	private boolean isEnglish = false;

	public boolean isEnglish() {
		return isEnglish;
	}
	public Document(int id, String raw_text, int rating, long time)
			throws Exception {
		// TODO Auto-generated constructor stub
		mID = id;
		mRawText = raw_text;
		if (raw_text == null)
			throw new Exception("Exception on document ID<" + id
					+ ">: raw_text is null, which is not allowed!");
		mRating = rating;
		mTime = time;
	}

	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof Document))
			return false;
		Document docFromObj = (Document) obj;
		if (mID == docFromObj.mID)
			return true;
		else
			return false;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return mRawText.hashCode();
	}

	public int getDocumentID(){
		return mID;
	}
	public String getRawText() {
		// TODO Auto-generated method stub
		return mRawText;
	}

	public int getRating() {
		// TODO Auto-generated method stub
		return mRating;
	}

	public int getLevel(){
		return mLevel;
	}
	public long getTime() {
		// TODO Auto-generated method stub
		return mTime;
	}

	public int[][] getSentences() {
		return sentences;
	}

	public void setSentences(int[][] sens) {
		sentences = sens;
	}
//	
//	/**
//	 * Only for constructor. This function break a string into words in 4 steps:
//	 * 
//	 * <pre>
//	 * - Step 1: Lower case
//	 * - Step 2: PoS tagging
//	 * - Step 3: Remove StopWord
//	 * - Step 4: Use Snowball Stemming (Porter 2)
//	 * </pre>
//	 * 
//	 * @param fullSentence
//	 *            - The sentence to extract words from
//	 * @return TRUE if it successfully extracted some words, FALSE otherwise
//	 * @throws SQLException
//	 * @throws ParseException
//	 */
//	public int extractSentences(int level) throws SQLException, ParseException {
//		TextNormalizer.TextNormalizer normalizer = TextNormalizer.TextNormalizer.getInstance();
//		Vocabulary voc = Vocabulary.getInstance();
//
//		// first, check if this is a new day: update information on all keywords
//		// of this app.
//		application.syncDayIndex(creationTime);
//		// continue extracting sentences and keywords.
//		// NatureLanguageProcessor nlp = NatureLanguageProcessor.getInstance();
//		// String[] rawSentences = nlp.extractSentence(rawText);
//		List<List<String>> normalizedSentences = normalizer
//				.normalize_SplitSentence(rawText);
//		// return and don't process further if this review is non english
//		if (normalizedSentences == null) {
//			isEnglish = false;
//			return 0;
//		}
//		isEnglish = true;
//		int[][] sentences_temp = new int[normalizedSentences.size()][0];
//		int countValidSen = 0, countWord = 0;
//		for (int i = 0; i < normalizedSentences.size(); i++) {
//			List<Integer> wordIDList = new ArrayList<>();
//			List<String> wordList = normalizedSentences.get(i);
//
//			// List<String> wordList =
//			// nlp.extractWordsFromText(rawSentences[i]);
//			// if (wordList == null)
//			// return 0;
//			// List<String[]> stemmedWordsWithPOS = nlp
//			// .stem(nlp.findPosTag(wordList));
//
//			// if (stemmedWordsWithPOS != null) {
//			// for (String[] pair : stemmedWordsWithPOS) {
//			// if (pair.length != 2)
//			// continue;
//			// add into voc, get wordID as returning param
//
//			// }
//			// }
//			// if (!normalizer.isNonEnglish(wordList, 0.4, 0.5)) {
//			for (String normalizedWord : wordList) {
//				String[] pair = normalizedWord.split("_");
//				if(pair.length == 0)
//					continue;
//				if(pair[0].length() == 0 || pair[1].length() == 0)
//					continue;
//				int wordid = voc.addWord(pair[0], pair[1], application,
//						rating - 1, this);
//				wordIDList.add(wordid);
//				countWord++;
//			}
//			countValidSen++;
//			sentences_temp[i] = Util.toIntArray(wordIDList);
//			// }
//		}
//		// remove Sentences with no words
//		sentences = new int[countValidSen][0];
//		int index = 0;
//		for (int[] sen : sentences_temp) {
//			if (sen.length > 0)
//				sentences[index++] = sen;
//		}
//		if (normalizedSentences.size() == 0
//				|| countValidSen / normalizedSentences.size() < 0.6){
//			isEnglish = false;
//			return 0;
//		}
//		isEnglish = true;
//		return countWord;
//	}
}
