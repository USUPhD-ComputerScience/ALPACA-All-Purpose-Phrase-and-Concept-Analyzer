package Datastores;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.xalan.xsltc.compiler.sym;

import TextNormalizer.TextNormalizer;
import Utils.POSTagConverter;
import Utils.Util;
import Vocabulary.DBWord;
import Vocabulary.Vocabulary;

public class Document {
	private String mRawText_fileName = null;
	private String mAuthor_fileName = null;
	private int mRating = -1;
	private long mTime = -1;
	private int[][] sentences;
	private int mLevel; // lv1, 2 or 3 of cleaned text
	private boolean isEnglish = false;

	public boolean isEnglish() {
		return isEnglish;
	}

	public void setLevel(int level) {
		mLevel = level;
	}

	public Document(String raw_text, int rating, long time, boolean english,
			String author_fn) throws Exception {
		// TODO Auto-generated constructor stub
		mRawText_fileName = raw_text;
		mAuthor_fileName = author_fn;
		mRating = rating;
		mTime = time;
		isEnglish = english;
	}

	public String getAuthorFileName(){
		return mAuthor_fileName;
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
		if (mRawText_fileName.equals(docFromObj.mRawText_fileName))
			return true;
		else
			return false;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return mRawText_fileName.hashCode();
	}


	public String getRawTextFileName() {
		// TODO Auto-generated method stub
		return mRawText_fileName;
	}

	public int getRating() {
		// TODO Auto-generated method stub
		return mRating;
	}

	public int getLevel() {
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

	/**
	 * 1. split raw text into sentences 2. normalize the sentences according to
	 * a level of normalization 3. save all the newly discovered words and
	 * sentences into the database 4. return true if the process was done
	 * nicely, return false if this is not an english document 4.a also update
	 * isEnglish to the newly discovered english status of the document 4.b also
	 * update english status to database
	 * 
	 * @param fullSentence
	 *            - The sentence to extract words from
	 * @return TRUE if it successfully extracted some words, FALSE otherwise
	 * @throws Exception
	 */
	public boolean preprocess(int level, String directory, Vocabulary voc) throws Exception {
		TextNormalizer normalizer = TextNormalizer.getInstance();
		POSTagConverter posconverter = POSTagConverter.getInstance();
		// NatureLanguageProcessor nlp = NatureLanguageProcessor.getInstance();
		// String[] rawSentences = nlp.extractSentence(rawText);
		String rawtext = readRawTextFromDirectory(directory);
		List<List<String>> normalizedSentences = normalizer
				.normalize_SplitSentence(rawtext, level);
		// return and don't process further if this review is non english
		if (normalizedSentences == null) {
			isEnglish = false;
			return false;
		}
		// update to English status
		isEnglish = true;
		int[][] sentences_temp = new int[normalizedSentences.size()][0];
		int countValidSen = 0, countWord = 0;
		for (int i = 0; i < normalizedSentences.size(); i++) {
			List<Integer> wordIDList = new ArrayList<>();
			List<String> wordList = normalizedSentences.get(i);
			for (String normalizedWord : wordList) {
				String[] pair = normalizedWord.split("_");
				if (pair.length == 0)
					continue;
				if (pair[0].length() == 0 || pair[1].length() == 0)
					continue;
				int wordid = voc.addWord(pair[0], posconverter.getCode(pair[1]));
				wordIDList.add(wordid);
				countWord++;
			}
			countValidSen++;
			sentences_temp[i] = Util.toIntArray(wordIDList);
			// }
		}
		// remove Sentences with no words
		sentences = new int[countValidSen][0];
		int index = 0;
		for (int[] sen : sentences_temp) {
			if (sen.length > 0)
				sentences[index++] = sen;
		}
		if (normalizedSentences.size() == 0
				|| countValidSen / normalizedSentences.size() < 0.6) {
			isEnglish = false;
			return false;
		}
		isEnglish = true;
		return true;
	}

	/**
	 * Similar to preprocess, but instead of preprocessing data, this function
	 * just read whatever were processed in database out to populate the
	 * document
	 * 
	 * @param fullSentence
	 *            - The sentence to extract words from
	 * @return TRUE if it successfully extracted some words, FALSE otherwise
	 * @throws Exception
	 */
	public void populatePreprocessedDataFromDB(int level, Dataset dataset) throws Exception {
		FileDataAdapter.getInstance().readCleansedText(this, dataset, level);
	}

	public String readRawTextFromDirectory(String directory)
			throws FileNotFoundException {
		Scanner rawtextFile = new Scanner(
				new File(directory + "rawData//" + mRawText_fileName));
		StringBuilder strbd = new StringBuilder();
		try {

			while (rawtextFile.hasNextLine()) {
				strbd.append(rawtextFile.nextLine());
			}
		} finally {
			rawtextFile.close();
		}
		return strbd.toString();
	}

	public String toString(boolean withPOS, Vocabulary voc)
			throws ClassNotFoundException, SQLException, UnsupportedEncodingException, IOException {
		if (sentences == null)
			return "";

		StringBuilder strBld = new StringBuilder();
		String prefix = "";
		for (int[] sentence : sentences) {
			for (int wordID : sentence) {
				DBWord w;
				w = voc.getWordFromDB(wordID);
				if (w != null) {
					strBld.append(prefix);
					if (withPOS)
						strBld.append(w.toString());
					else
						strBld.append(w.getText());
					prefix = " ";
				}
			}
			strBld.append(". ");
		}
		return strBld.toString();
	}

	public String toPOSString(Vocabulary voc) throws ClassNotFoundException, SQLException, UnsupportedEncodingException, IOException {
		if (sentences == null)
			return "";

		StringBuilder strBld = new StringBuilder();
		String prefix = "";
		for (int[] sentence : sentences) {
			for (int wordID : sentence) {
				DBWord w;
				w = voc.getWordFromDB(wordID);
				if (w != null) {
					strBld.append(prefix);
					strBld.append(
							POSTagConverter.getInstance().getTag(w.getPOS()));
					prefix = " ";
				}
			}
			strBld.append(". ");
		}
		return strBld.toString();
	}
}
