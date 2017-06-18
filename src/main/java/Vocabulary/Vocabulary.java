package Vocabulary;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import Datastores.Dataset;
import Datastores.DocumentDatasetDB;
import Utils.POSTagConverter;

public class Vocabulary {
	DocumentDatasetDB db;
	private Map<Integer, DBWord> VocSearchForWord = new HashMap<>();
	private Map<DBWord, Integer> VocSearchForID = new HashMap<>();
	private static Vocabulary instance = null;

	public static synchronized Vocabulary getInstance()
			throws ClassNotFoundException, SQLException {
		if (instance == null) {
			instance = new Vocabulary();
		}
		return instance;
	}

	private Vocabulary() throws ClassNotFoundException, SQLException {
		db = DocumentDatasetDB.getInstance();
	}

	public void writeWordsToFile(String fileName) throws FileNotFoundException {
		System.out.print(">>Writing Words to file");

		PrintWriter pw = new PrintWriter(fileName);
		for (DBWord word : VocSearchForWord.values()) {
			pw.println(word.getText() + "," + word.getPOS() + ","
					+ word.getCount(DocumentDatasetDB.LV1_SPELLING_CORRECTION)
					+ ","
					+ word.getCount(DocumentDatasetDB.LV2_ROOTWORD_STEMMING)
					+ "," + word.getCount(DocumentDatasetDB.LV3_OVER_STEMMING)
					+ "," + word.getCount(
							DocumentDatasetDB.LV4_ROOTWORD_STEMMING_LITE));
		}
		pw.close();

	}

	public int loadDBKeyword(Dataset dataset, int level)
			throws SQLException, ClassNotFoundException {
		List<DBWord> wordListFromDB = DocumentDatasetDB.getInstance()
				.queryWordsForADataset(dataset, level);
		for (DBWord word : wordListFromDB) {
			// add to voc
			addNewWord(word);
		}
		return wordListFromDB.size();
	}

	private void addNewWord(DBWord w) {
		int id = w.getID();
		VocSearchForID.put(w, id);
		VocSearchForWord.put(id, w);
	}

	// if a new word then add to database
	// return the id of the word
	public int addWord(String w, byte POS, int datasetID, int level)
			throws SQLException, ParseException {
		Integer wordID = VocSearchForID.get(
				new DBWord(-1, w.intern(), POS, datasetID, -1, -1, -1, -1));
		// not in voc, create a new entry for this word
		if (wordID == null) {
			// query from db
			// not in db, create new words
			wordID = db.addKeyWordToVocabulary(w.intern(),
					POSTagConverter.getInstance().getTag(POS), datasetID);
			Map<String, Integer> POSs = new HashMap<>();
			DBWord word = null;
			switch (level) {
			case DocumentDatasetDB.LV1_SPELLING_CORRECTION:
				word = new DBWord(wordID, w.intern(), POS, datasetID, 1, -1, -1,
						-1);
				break;
			case DocumentDatasetDB.LV2_ROOTWORD_STEMMING:
				word = new DBWord(wordID, w.intern(), POS, datasetID, -1, 1, -1,
						-1);
				break;
			case DocumentDatasetDB.LV3_OVER_STEMMING:
				word = new DBWord(wordID, w.intern(), POS, datasetID, -1, -1, 1,
						-1);
				break;
			case DocumentDatasetDB.LV4_ROOTWORD_STEMMING_LITE:
				word = new DBWord(wordID, w.intern(), POS, datasetID, -1, -1,
						-1, 1);
				break;
			}
			// add to voc
			addNewWord(word);
		} else {
			VocSearchForWord.get(wordID).incrementCount(level);
		}
		return wordID;
	}

	public DBWord getWord(int keywordid) {
		return VocSearchForWord.get(keywordid);
	}

	public DBWord getWordFromDB(int keywordid) throws SQLException {
		DBWord word = getWord(keywordid);
		if (word == null) {
			word = db.querySingleWord(keywordid);
			addNewWord(word);
		}
		return word;
	}

	public void updateCountToDB(int level) throws ClassNotFoundException, SQLException {
		// TODO Auto-generated method stub
		System.out.println(">> Updating word counts to Database");
		for(DBWord word : VocSearchForWord.values()){
			word.updateCountToDB(level);
		}
		System.out.println(">> Updated count for " + VocSearchForWord.size() + " words");
		
	}

}
