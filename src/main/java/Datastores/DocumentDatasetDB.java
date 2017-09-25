package Datastores;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

import com.opencsv.CSVWriter;

import NLP.NatureLanguageProcessor;
import Utils.POSTagConverter;
import Utils.PostgreSQLConnector;
import Vocabulary.DBWord;
import Vocabulary.Word;

public class DocumentDatasetDB {
	public static final String DBLOGIN = "postgres";
	public static final String DBPASSWORD = "phdcs2014";
	public static final String CONCEPTDB = "conceptdb";
	public static final String DATASETS_TABLE = "datasets";
	public static final String RAWDATA_TABLE = "rawdata";
	public static final String CLEANEDTEXT_TABLE = "cleanedText";
	public static final String VOCABULARY_TABLE = "vocabulary";
	public static final int LV1_SPELLING_CORRECTION = 1;
	public static final int LV2_ROOTWORD_STEMMING = 2;
	public static final int LV3_OVER_STEMMING = 3;
	public static final int LV4_ROOTWORD_STEMMING_LITE = 4;
	private PostgreSQLConnector dbconnector;
	private static DocumentDatasetDB instance = null;
	// this has to be hard code in here to avoid user messing up with database
	private static final String[] createTableSQL = new String[] {
			"CREATE TABLE IF NOT EXISTS datasets(ID SERIAL PRIMARY KEY NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL, has_time boolean, has_rating boolean, has_author boolean, metadata TEXT);	",
			"CREATE TABLE IF NOT EXISTS rawdata(ID SERIAL PRIMARY KEY NOT NULL, datasetid INT	references datasets(ID), raw_data_file_name TEXT NOT NULL, rating INT, creation_time BIGINT, author_file_name TEXT, is_english boolean );",
			"CREATE TABLE IF NOT EXISTS cleanedText( ID SERIAL PRIMARY KEY NOT NULL, rawdataID INT	references rawdata(ID), spelling_correction_LV1	TEXT, rootword_stemming_LV2 TEXT, over_stemming_LV3	TEXT, rootword_stemming_lite_LV4 TEXT);",
			"CREATE TABLE vocabulary( ID SERIAL PRIMARY KEY NOT NULL, text TEXT NOT NULL, datasetid INT	references datasets(ID), POS TEXT NOT NULL, total_count_LV1 INT, total_count_LV2 INT, total_count_LV3 INT, total_count_LV4 INT, UNIQUE (text, datasetid,POS));",
			"CREATE TABLE IF NOT EXISTS word2vec( word TEXT PRIMARY KEY NOT NULL, vector TEXT);" };

	public static synchronized DocumentDatasetDB getInstance()
			throws ClassNotFoundException, SQLException {
		if (instance == null) {
			instance = new DocumentDatasetDB();
			instance.dbconnector = new PostgreSQLConnector(DBLOGIN, DBPASSWORD,
					CONCEPTDB, createTableSQL);
		}
		return instance;
	}

	private DocumentDatasetDB() {

	}

	// specify which datasets you want to pull
	public DatasetManager queryMultipleDatasetsInfo(String condition)
			throws SQLException {
		DatasetManager datasetMan = DatasetManager.getInstance();
		String fields[] = { "ID", "name", "description", "has_time",
				"has_rating", "has_author", "metadata" };

		ResultSet results;
		results = dbconnector.select(DATASETS_TABLE, fields, condition);

		while (results.next()) {
			int ID = results.getInt("ID");
			String name = results.getString("name");
			String description = results.getString("description");
			boolean has_time = results.getBoolean("has_time");
			boolean has_rating = results.getBoolean("has_rating");
			boolean has_author = results.getBoolean("has_author");
			String metadata = results.getString("metadata");
			datasetMan.addDataset(new Dataset(ID, name, description, has_time,
					has_rating, has_author, metadata));
		}
		return datasetMan;
	}

	public Dataset queryDatasetInfo(int datasetID) throws SQLException {
		String fields[] = { "ID", "name", "description", "has_time",
				"has_rating", "has_author", "metadata" };
		String condition = "ID='" + datasetID + "'";
		// condition = // "count>1000";

		ResultSet results;
		results = dbconnector.select(DATASETS_TABLE, fields, condition);
		while (results.next()) {
			String name = results.getString("name");
			String description = results.getString("description");
			boolean has_time = results.getBoolean("has_time");
			boolean has_rating = results.getBoolean("has_rating");
			boolean has_author = results.getBoolean("has_author");
			String metadata = results.getString("metadata");
			return new Dataset(datasetID, name, description, has_time,
					has_rating, has_author, metadata);
		}
		return null;
	}

	public Dataset queryRawTextForSingleDataset(Dataset dataset)
			throws Exception {
		String fields[] = { "ID", "raw_data_file_name", "rating",
				"creation_time", "is_english", "author_file_name" };
		String condition = "datasetid='" + dataset.getID() + "'";
		// condition = // "count>1000";

		ResultSet results;
		results = dbconnector.select(RAWDATA_TABLE, fields, condition);
		while (results.next()) {
			int id = results.getInt("ID");
			String raw_data_file_name = results.getString("raw_data_file_name");
			int rating = -1;
			if (dataset.hasRating())
				rating = results.getInt("rating");
			long time = -1;
			if (dataset.hasTime())
				time = results.getLong("creation_time");
			boolean is_english = results.getBoolean("is_english");
			String author_file_name = null;
			if (dataset.hasAuthor())
				author_file_name = results.getString("author_file_name");
			Document doc = new Document(id, raw_data_file_name, rating, time,
					is_english, dataset.getID(), author_file_name);
			dataset.addDocument(doc);
		}
		return null;
	}
	public int[][] queryPreprocessedData(int documentID, int level)
			throws Exception {
		String fields[] = {  "spelling_correction_LV1", "rootword_stemming_LV2",
				"over_stemming_LV3", "rootword_stemming_lite_LV4" };
		String condition = "rawdataID='" + documentID + "'";
		// condition = // "count>1000";

		ResultSet results;
		results = dbconnector.select(CLEANEDTEXT_TABLE, fields, condition);
		while (results.next()) {
			int[][]sens = null;
			switch (level) {

			case LV1_SPELLING_CORRECTION:
				sens = text2Int2D(results.getString("spelling_correction_LV1"));
				break;
			case LV2_ROOTWORD_STEMMING:
				sens = text2Int2D(results.getString("rootword_stemming_LV2"));
				break;
			case LV3_OVER_STEMMING:
				sens = text2Int2D(results.getString("over_stemming_LV3"));
				break;
			case LV4_ROOTWORD_STEMMING_LITE:
				sens = text2Int2D(results.getString("rootword_stemming_lite_LV4"));
			}
			return sens;
		}
		return null;
	}
	public boolean isDatasetIDexist(String datasetID) throws SQLException {
		String fields[] = { "ID" };
		String condition = "ID='" + datasetID + "'";

		ResultSet results;
		results = dbconnector.select(DATASETS_TABLE, fields, condition);

		while (results.next()) {
			return true;
		}
		return false;
	}

	// public String getName(String appid) throws SQLException {
	// String fields[] = { "name" };
	// String condition = "appid='" + appid + "'";
	// // condition = // "count>1000";
	//
	// ResultSet results;
	// results = dbconnector.select(APPS_TABLE, fields, condition);
	//
	// while (results.next()) {
	// return results.getString("name");
	// }
	// return null;
	// }

	// public void updateReviewNumberForApp(int revCount, String appid)
	// throws SQLException {
	// String fields[] = { "count" };
	// String condition = "appid='" + appid + "'";
	// // condition = // "count>1000";
	//
	// ResultSet results;
	// results = dbconnector.select(APPS_TABLE, fields, condition);
	// int returnCount = 0;
	// while (results.next()) {
	// returnCount = results.getInt("count");
	// }
	// returnCount += revCount;
	// dbconnector.update(APPS_TABLE, "count=" + returnCount, condition);
	// }
	public int insertRawData(String raw_text_filename, int rating, long time,
			String author_fileName, int datasetId) throws SQLException {
		String values[] = new String[6];

		values[0] = datasetId + "";
		values[1] = raw_text_filename;
		if (rating == -1)
			values[2] = "null";
		else
			values[2] = rating + "";
		if (time == -1)
			values[3] = "null";
		else
			values[3] = time + "";
		values[4] = author_fileName;
		values[5] = "FALSE";
		int arrays[] = new int[] { 1, 0, 1, 2, 0, 7 };
		try {
			return dbconnector.insert(RAWDATA_TABLE, values, arrays, true,
					true);
		} catch (SQLException e) {
		}
		return -1;
	}

	/**
	 * Use this function to make a raw document to English status, default is
	 * not english
	 * 
	 * @param ID
	 *            of the raw document in db
	 * @throws SQLException
	 */
	public void updateEnglishStatus(int rawID) throws SQLException {
		// TODO Auto-generated method stub
		String condition = "ID = '" + rawID + "'";
		String updateContent = "is_english = TRUE";

		dbconnector.update(RAWDATA_TABLE, updateContent, condition);
	}

	// public boolean insertDocument(Document doc, String datasetId)
	// throws SQLException {
	// String values[] = new String[4];
	//
	// values[0] = datasetId;
	// values[1] = doc.getRawTextFileName(); // appid
	// if (doc.getRating() == -1)
	// values[2] = "null";
	// else
	// values[2] = doc.getRating() + "";
	// if (doc.getTime() == -1)
	// values[3] = "null";
	// else
	// values[3] = doc.getTime() + "";
	// int arrays[] = new int[] { 1, 0, 1, 2 };
	// int id = 0;
	// try {
	// id = dbconnector.insert(RAWDATA_TABLE, values, arrays, true, true);
	// } catch (SQLException e) {
	// }
	// if (id == 0)
	// return false;
	// return true;
	// }

	public int addNewDataset(String name, String description, boolean hasRating,
			boolean hasTime, boolean hasAuthor, String otherMetadata)
			throws SQLException {

		String values[] = new String[6];
		values[0] = name;
		values[1] = description;
		values[2] = String.valueOf(hasRating);
		values[3] = String.valueOf(hasTime);
		values[4] = String.valueOf(hasAuthor);
		values[5] = otherMetadata;
		int arrays[] = new int[] { 0, 0, 7, 7, 7, 0 };
		return dbconnector.insert(DATASETS_TABLE, values, arrays, true, true);

	}

	public void close() throws SQLException {
		dbconnector.close();
	}

	public int addKeyWordToVocabulary(String w, String POS, int datasetID)
			throws SQLException {
		String values[] = new String[7];
		values[0] = w; // appid
		values[1] = datasetID + "";
		values[2] = POS;
		values[3] = "-1";
		values[4] = "-1";
		values[5] = "-1";
		values[6] = "-1";
		int arrays[] = new int[] { 0, 1, 0, 1, 1, 1, 1 };
		return dbconnector.insert(VOCABULARY_TABLE, values, arrays, true, true);
	}

	/*UPDATE weather SET temp_lo = temp_lo+1, temp_hi = temp_lo+15, prcp = DEFAULT
	  WHERE city = 'San Francisco' AND date = '2003-07-03'
	  RETURNING temp_lo, temp_hi, prcp;*/
	public int updateKeyWordCount(int wordid, int totalCount, int lv)
			throws SQLException {
		String update = "";
		if (lv == LV1_SPELLING_CORRECTION) {
			update = "total_count_LV1=" + totalCount;
		}
		if (lv == LV2_ROOTWORD_STEMMING) {
			update = "total_count_LV2=" + totalCount;
		}
		if (lv == LV3_OVER_STEMMING) {
			update = "total_count_LV3=" + totalCount;
		}
		if (lv == LV4_ROOTWORD_STEMMING_LITE) {
			update = "total_count_LV4=" + totalCount;
		}
		return dbconnector.update(VOCABULARY_TABLE, update, "ID=" + wordid);
	}

	// public int updateKeyWord(Word word, String appid) throws SQLException {
	// int[][] timeseries = word.getTimeSeriesByRating();
	// int[] tem = new int[timeseries[0].length - 1];
	// for (int k = 0; k < tem.length; k++)
	// tem[k] = timeseries[0][k];
	// String rate1Update = "rate1_byday='" + int1D2Text(tem) + "'";
	// for (int k = 0; k < tem.length; k++)
	// tem[k] = timeseries[1][k];
	// String rate2Update = "rate2_byday='" + int1D2Text(tem) + "'";
	// for (int k = 0; k < tem.length; k++)
	// tem[k] = timeseries[2][k];
	// String rate3Update = "rate3_byday='" + int1D2Text(tem) + "'";
	// for (int k = 0; k < tem.length; k++)
	// tem[k] = timeseries[3][k];
	// String rate4Update = "rate4_byday='" + int1D2Text(tem) + "'";
	// for (int k = 0; k < tem.length; k++)
	// tem[k] = timeseries[4][k];
	// String rate5Update = "rate5_byday='" + int1D2Text(tem) + "'";
	//
	// String POSUpdate = "POS='" + map2Text(word.getPOSSet()) + "'";
	// String updateFields = rate1Update + ", " + rate2Update + ", "
	// + rate3Update + ", " + rate4Update + ", " + rate5Update + ", "
	// + POSUpdate;
	//
	// return dbconnector.update(KEYWORDS_TABLE, updateFields,
	// "ID=" + word.getWordID() + " AND " + "appid='" + appid + "'");
	//
	// }

	// private int copyCountByDayToDB(CopyManager cpManager, PushbackReader
	// reader,
	// int countInsert, int keyword_id, StringBuilder sb, int type,
	// Map<Long, Integer> countByDay) throws IOException, SQLException {
	// for (Entry<Long, Integer> dayN : countByDay.entrySet()) {
	// long date = dayN.getKey();
	// int count = dayN.getValue();
	// sb.append(date).append(",").append(keyword_id).append(",")
	// .append(type).append(",").append(count).append("\n");
	// countInsert++;
	// if (countInsert % 200 == 0) {
	// reader.unread(sb.toString().toCharArray());
	// cpManager.copyIn(
	// "COPY days(date,keyword_id,type,count) FROM STDIN WITH CSV",
	// reader);
	// sb.delete(0, sb.length());
	// }
	// }
	// return countInsert;
	// }

	// public Word querySingleWord(int DBID, Application app) throws
	// SQLException {
	// String fields[] = { "ID", "appid", "keyword", "rate1_byday",
	// "rate2_byday", "rate3_byday", "rate4_byday", "rate5_byday",
	// "POS" };
	// String condition = "ID=" + DBID + " AND appid='" + app.getAppID() + "'";
	// ResultSet results;
	// results = dbconnector.select(KEYWORDS_TABLE, fields, condition);
	// Word word = null;
	// while (results.next()) {
	// int ID = results.getInt("ID");
	// int[][] ratesByDays = new int[5][];
	// ratesByDays[0] = text2Int1D(results.getString("rate1_byday"));
	// ratesByDays[1] = text2Int1D(results.getString("rate2_byday"));
	// ratesByDays[2] = text2Int1D(results.getString("rate3_byday"));
	// ratesByDays[3] = text2Int1D(results.getString("rate4_byday"));
	// ratesByDays[4] = text2Int1D(results.getString("rate5_byday"));
	// Map<String, Integer> POSs = text2Map(results.getString("POS"));
	// word = new Word(DBID, results.getString("keyword"), POSs, app,
	// ratesByDays, ratesByDays[0].length);
	// }
	// return word;
	// }
	public DBWord querySingleWord(int DBID) throws SQLException {
		String fields[] = { "ID", "text", "datasetid", "POS", "total_count_LV1",
				"total_count_LV2", "total_count_LV3", "total_count_LV4" };
		String condition = "ID=" + DBID;
		ResultSet results;
		results = dbconnector.select(VOCABULARY_TABLE, fields, condition);
		DBWord word = null;
		while (results.next()) {
			byte POS = POSTagConverter.getInstance()
					.getCode(results.getString("POS"));
			String text = results.getString("text");
			int datasetID = results.getInt("datasetid");
			int total_count_LV1 = results.getInt("total_count_LV1");
			int total_count_LV2 = results.getInt("total_count_LV2");
			int total_count_LV3 = results.getInt("total_count_LV3");
			int total_count_LV4 = results.getInt("total_count_LV4");
			word = new DBWord(DBID, text, POS, datasetID, total_count_LV1,
					total_count_LV2, total_count_LV3, total_count_LV4);
		}
		return word;
	}

	public List<DBWord> queryWordsForADataset(Dataset dataset, int level)
			throws SQLException {
		List<DBWord> wordList = new ArrayList<>();
		String fields[] = { "ID", "text", "datasetid", "POS", "total_count_LV1",
				"total_count_LV2", "total_count_LV3", "total_count_LV4" };

		String condition = "datasetid='" + dataset.getID() + "'";
		switch (level) {
		case LV1_SPELLING_CORRECTION:
			condition += " AND total_count_LV1 > 0";
			break;
		case LV2_ROOTWORD_STEMMING:
			condition += " AND total_count_LV2 > 0";
			break;
		case LV3_OVER_STEMMING:
			condition += " AND total_count_LV3 > 0";
			break;
		case LV4_ROOTWORD_STEMMING_LITE:
			condition += " AND total_count_LV4 > 0";
		}
		ResultSet results;
		results = dbconnector.select(VOCABULARY_TABLE, fields, condition);
		DBWord word = null;
		while (results.next()) {

			int ID = results.getInt("ID");
			String text = results.getString("text");
			byte POS = POSTagConverter.getInstance()
					.getCode(results.getString("POS"));
			int total_count_LV1 = results.getInt("total_count_LV1");
			int total_count_LV2 = results.getInt("total_count_LV2");
			int total_count_LV3 = results.getInt("total_count_LV3");
			int total_count_LV4 = results.getInt("total_count_LV4");
			word = new DBWord(ID, text, POS, dataset.getID(), total_count_LV1,
					total_count_LV2, total_count_LV3, total_count_LV4);
			wordList.add(word);
		}
		return wordList;
	}

	// public Word queryWordByKey(String key, Application app)
	// throws SQLException {
	// String fields[] = { "ID", "appid", "keyword", "rate1_byday",
	// "rate2_byday", "rate3_byday", "rate4_byday", "rate5_byday",
	// "POS" };
	// String condition = "appid='" + app.getAppID() + "' AND keyword='" + key
	// + "'";
	// ResultSet results;
	// results = dbconnector.select(KEYWORDS_TABLE, fields, condition);
	// Word word = null;
	// while (results.next()) {
	// int ID = results.getInt("ID");
	// int[][] ratesByDays = new int[5][];
	// ratesByDays[0] = text2Int1D(results.getString("rate1_byday"));
	// ratesByDays[1] = text2Int1D(results.getString("rate2_byday"));
	// ratesByDays[2] = text2Int1D(results.getString("rate3_byday"));
	// ratesByDays[3] = text2Int1D(results.getString("rate4_byday"));
	// ratesByDays[4] = text2Int1D(results.getString("rate5_byday"));
	// Map<String, Integer> POSs = text2Map(results.getString("POS"));
	// word = new Word(ID, key, POSs, app, ratesByDays,
	// ratesByDays[0].length);
	// }
	// return word;
	// }

	// public void updatePOStagforAnApp(Application app) throws SQLException {
	// NatureLanguageProcessor nlp = NatureLanguageProcessor.getInstance();
	// for (ReviewForAnalysis review : app.getReviewList()) {
	// String str = review.toProperString();
	// StringBuilder POS_TAG = new StringBuilder();
	// String[] sentenceList = str.split("\\.+");
	// for (String sentence : sentenceList) {
	// String tagged = nlp.findPosTag(sentence);
	// String[] words = tagged.split(" "); // word_tag word_tag ..
	// for (int i = 0; i < words.length; i++) {
	// String[] w = words[i].split("_");
	// // if (!stopWordSet.contains(w[0]))
	// if (w.length == 2 && nlp.POSSET.contains(w[1])) {
	// POS_TAG.append(w[1]).append(" ");
	// } else {
	// POS_TAG.append("UNK").append(" ");
	// }
	// }
	// POS_TAG.deleteCharAt(POS_TAG.length() - 1);
	// POS_TAG.append(".");
	// }
	//
	// String condition = "reviewid = '" + review.getReviewId() + "'";
	// dbconnector.update(REVIEWS_TABLE,
	// "postag ='" + POS_TAG.toString() + "'", condition);
	// }
	// }

	// first update then if no row is affected, insert!
	public void updateCleansedText(Document doc) throws SQLException {
		// TODO Auto-generated method stub
		String cleansedText = int2D2Text(doc.getSentences());
		String condition = "rawdataID = '" + doc.getDocumentID() + "'";
		if (doc.isEnglish()) {
			String updateContent = null;
			switch (doc.getLevel()) {
			case LV1_SPELLING_CORRECTION:
				updateContent = "spelling_correction_LV1 ='" + cleansedText
						+ "'";
				break;
			case LV2_ROOTWORD_STEMMING:
				updateContent = "rootword_stemming_LV2 ='" + cleansedText + "'";
				break;
			case LV3_OVER_STEMMING:
				updateContent = "over_stemming_LV3 ='" + cleansedText + "'";
				break;
			case LV4_ROOTWORD_STEMMING_LITE:
				updateContent = "rootword_stemming_lite_LV4 ='" + cleansedText
						+ "'";
				break;
			}
			int result = dbconnector.update(CLEANEDTEXT_TABLE, updateContent,
					condition);
			if (result == 0) { // 0 row affected
				String values[] = new String[5];
				values[0] = doc.getDocumentID() + ""; // appid
				switch (doc.getLevel()) {
				case LV1_SPELLING_CORRECTION:
					values[1] = cleansedText;
					values[2] = "null";
					values[3] = "null";
					values[4] = "null";
					break;
				case LV2_ROOTWORD_STEMMING:
					values[1] = "null";
					values[2] = cleansedText;
					values[3] = "null";
					values[4] = "null";
					break;
				case LV3_OVER_STEMMING:
					values[1] = "null";
					values[2] = "null";
					values[3] = cleansedText;
					values[4] = "null";
					break;
				case LV4_ROOTWORD_STEMMING_LITE:
					values[1] = "null";
					values[2] = "null";
					values[3] = "null";
					values[4] = cleansedText;
					break;
				}
				int arrays[] = new int[] { 1, 0, 0, 0, 0 };
				dbconnector.insert(CLEANEDTEXT_TABLE, values, arrays, true,
						true);
			}
		}
	}

	// return the total bug retrieved
	// public int queryMultipleProductInfo(int minBugs, boolean cleansed)
	// throws SQLException, ParseException {
	// // TODO Auto-generated method stub
	// ResultSet results;
	// StringBuilder idListstr = new StringBuilder();
	// if (!cleansed) {
	// ArrayList<Integer> idList = new ArrayList<>();
	// String query = "appid, count(*) as count from apps group by product_id";
	// results = dbconnectorBugEclipse.select(query);
	// while (results.next()) {
	// int product_id = results.getInt("product_id");
	// int bugcount = results.getInt("bugcount");
	// if (bugcount > minBugs)
	// idList.add(product_id);
	// }
	// for (int i = 0; i < idList.size(); i++) {
	// idListstr.append(idList.get(i));
	// if (i < (idList.size() - 1))
	// idListstr.append(",");
	// }
	// } else {
	// ArrayList<String> idList = new ArrayList<>();
	// String query = "product_id, bug_count from products";
	// results = dbconnectorBugExt.select(query);
	// while (results.next()) {
	// String product_id = results.getString("product_id");
	// int bugcount = results.getInt("bug_count");
	// if (bugcount > minBugs)
	// idList.add(product_id);
	// }
	// for (int i = 0; i < idList.size(); i++) {
	// idListstr.append("'" + idList.get(i) + "'");
	// if (i < (idList.size() - 1))
	// idListstr.append(",");
	// }
	// }
	// return getBugsByIDs(idListstr.toString(), cleansed);
	// }

	// // return the total bug retrieved
	// public void queryMultipleProductInfo(List<String> idList)
	// throws SQLException, ParseException {
	// // TODO Auto-generated method stub
	// StringBuilder idListstr = new StringBuilder();
	// int productCount = 0;
	// ProductManager productMan = ProductManager.getInstance();
	// for (int i = 0; i < idList.size(); i++) {
	// idListstr.append("'" + idList.get(i) + "'");
	// if (i < (idList.size() - 1))
	// idListstr.append(",");
	// }
	// ResultSet results;
	// String query = "product_id,name,version,description,start_date,"
	// + "classification_id,day_index from products"
	// + " where product_id IN (" + idListstr.toString() + ")";
	// results = dbconnectorBugExt.select(query);
	// while (results.next()) {
	// String product_id = results.getString("product_id");
	// String product_name = results.getString("name");
	// String product_version = results.getString("version");
	// String product_description = results.getString("description");
	// int product_classification_id = results.getInt("classification_id");
	// int dayIndex = results.getInt("day_index");
	// long startDate = results.getLong("start_date");
	// Product thisProduct = productMan.getProductByID(product_id);
	//
	// if (thisProduct == null) {
	// productCount++;
	// thisProduct = productMan.addProduct(product_id, product_version,
	// product_name, product_description,
	// product_classification_id, startDate, dayIndex);
	// System.out.println(
	// productCount + ". Adding new product: " + product_id);
	// }
	// }
	// }

	// public int getBugsByIDs(String idListstr, boolean cleansed)
	// throws SQLException, ParseException {
	// ProductManager productMan = ProductManager.getInstance();
	// int bugCount = 0;
	// int productCount = 0;
	// if (!cleansed) {
	// SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.S");
	// String query = "bugs.creation_ts," + "bugs.version,bugs.product_id,"
	// + "products.name,products.description,products.classification_id"
	// + " from bugs,products where "
	// + "bugs.product_id = products.id "
	// + "AND bugs.product_id IN (" + idListstr + ")";
	// // System.out.println(query);
	// ResultSet results = dbconnectorBugEclipse.select(query);
	// while (results.next()) {
	// long creation_ts = f
	// .parse(results.getString("bugs.creation_ts")).getTime();
	// String version = results.getString("bugs.version");
	// int product_id = results.getInt("bugs.product_id");
	// String product_name = results.getString("products.name");
	// String product_description = results
	// .getString("products.description");
	// int product_classification_id = results
	// .getInt("products.classification_id");
	//
	// Product thisProduct = productMan.getProductByID(
	// ProductManager.generateID(product_name, version));
	//
	// if (thisProduct == null) {
	// productCount++;
	// thisProduct = productMan.addProduct(version, product_name,
	// product_description, product_classification_id,
	// creation_ts, -1);
	// thisProduct.setDBID(product_id);
	// System.out.println(productCount + ". Adding new product: "
	// + product_name + "-"
	// + util.Util.extractVersion(version));
	// } else {
	// // this version is already added
	// }
	//
	// // thisProduct.addBug(bug_id, bug_severity, priority,
	// // creation_ts,
	// // short_desc, fullText, null, null);
	// // bugCount++;
	// }
	// } else {
	// String query = "bugs.bug_id,bugs.severity,bugs.creation_time,"
	// + "bugs.short_desc,bugs.mPriority,bugs.product_id,"
	// + "products.name,products.description,products.classification_id,"
	// + "bugs.comments,bugs.nonidentifier_words,bugs.identifier_words,"
	// + "products.day_index from bugs,products where "
	// + "bugs.product_id = products.product_id "
	// + "AND bugs.product_id IN (" + idListstr + ")";
	//
	// // System.out.println(query);
	// ResultSet results = dbconnectorBugExt.select(query);
	// while (results.next()) {
	// int bug_id = results.getInt("bug_id");
	// String bug_severity = results.getString("severity");
	// long creation_ts = results.getLong("creation_time");
	// String short_desc = results.getString("short_desc");
	// String priority = results.getString("mPriority");
	// String product_id = results.getString("product_id");
	// String product_name = results.getString("name");
	// String product_description = results.getString("description");
	// int product_classification_id = results
	// .getInt("classification_id");
	// String fullText = results.getString("comments");
	//
	// List<Integer> nonidentifier_words = text2Int1DList(
	// results.getString("nonidentifier_words"));
	// List<Integer> identifier_words = text2Int1DList(
	// results.getString("identifier_words"));
	// int day_index = results.getInt("day_index");
	// Product thisProduct = productMan.getProductByID(product_id);
	//
	// if (thisProduct == null) {
	// productCount++;
	// thisProduct = productMan.addProduct(product_id,
	// product_name, product_description,
	// product_classification_id, creation_ts, day_index);
	// System.out.println(productCount + ". Adding new product: "
	// + product_id);
	// }
	//
	// thisProduct.addBug(bug_id, bug_severity, priority, creation_ts,
	// short_desc, fullText, identifier_words,
	// nonidentifier_words);
	// bugCount++;
	// }
	// }
	// return bugCount;
	// }

	// public int queryReviewsforAProduct(Application app)
	// throws SQLException, ParseException {
	// // TODO Auto-generated method stub
	// int bugCount = 0;
	// SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.S");
	// String query = "reviews.reviewid,reviews.rating,reviews.creation_time,"
	// + "reviews.title,reviews.appid,"
	// + "bugs_fulltext.comments from bugs,bugs_fulltext where "
	// + "bugs.bug_id = bugs_fulltext.bug_id AND bugs.product_id = "
	// + product.getDBID();
	// // System.out.println(query);
	// ResultSet results = dbconnectorBugEclipse.select(query);
	// Set<String> severity_type = new HashSet<>();
	// Set<String> priority_type = new HashSet<>();
	// while (results.next()) {
	// int bug_id = results.getInt("bugs.bug_id");
	// String bug_severity = results.getString("bugs.bug_severity");
	// severity_type.add(bug_severity);
	// long creation_ts = f.parse(results.getString("bugs.creation_ts"))
	// .getTime();
	// String short_desc = results.getString("bugs.short_desc");
	// String priority = results.getString("bugs.priority");
	// priority_type.add(priority);
	// String fullText = results.getString("bugs_fulltext.comments");
	//
	// String version = results.getString("bugs.version");
	//
	// if (ProductManager.generateID(product.getName(), version)
	// .equals(product.getID())) {
	// product.addBug(bug_id, bug_severity, priority, creation_ts,
	// short_desc, fullText, null, null);
	// bugCount++;
	// }
	// }
	// System.out.println("Severity type:");
	// System.out.println(severity_type);
	// System.out.println("Priority type:");
	// System.out.println(priority_type);
	// return bugCount;
	// }

	// /////////////////////////////////////////

	private String int1D2Text(int[] int1D) {
		if (int1D == null || int1D.length == 0)
			return "null";
		StringBuilder strBuilder = new StringBuilder();
		String prefix = "";
		for (int i : int1D) {
			strBuilder.append(prefix + i);
			prefix = ",";
		}
		return strBuilder.toString();
	}

	private String int1D2Text(List<Integer> int1D) {
		if (int1D == null || int1D.size() == 0)
			return "null";
		StringBuilder strBuilder = new StringBuilder();
		String prefix = "";
		for (int i : int1D) {
			strBuilder.append(prefix + i);
			prefix = ",";
		}
		return strBuilder.toString();
	}

	private String int2D2Text(int[][] int2D) {
		if (int2D == null || int2D.length == 0)
			return "null";
		StringBuilder strBuilder = new StringBuilder();
		String prefix = "";
		for (int[] i : int2D) {
			strBuilder.append(prefix);
			prefix = "";
			for (int j : i) {
				strBuilder.append(prefix + j);
				prefix = ",";
			}
			prefix = ";";
		}
		return strBuilder.toString();
	}

	private int[] text2Int1D(String text) {
		int[] argInt = null;
		if (text != null) {
			if (!text.equals("null")) {
				String[] ints = text.split(",");
				argInt = new int[ints.length];
				for (int j = 0; j < argInt.length; j++)
					argInt[j] = Integer.parseInt(ints[j]);
			}
		}
		return argInt;
	}

	private List<Integer> text2Int1DList(String text) {
		List<Integer> argInt = new ArrayList<>();
		if (text != null) {
			if (!text.equals("null")) {
				String[] ints = text.split(",");
				for (int j = 0; j < ints.length; j++)
					argInt.add(Integer.parseInt(ints[j]));
			}
		}
		return argInt;
	}

	private int[][] text2Int2D(String text) {
		int[][] argIntArray = null;

		if (text != null) {
			if (!text.equals("null")) {
				// "1,2,3;1,2,3;;1,2,3"
				String[] intArray = text.split(";");
				argIntArray = new int[intArray.length][];
				for (int j = 0; j < argIntArray.length; j++) {
					if (intArray[j].equals("") || intArray[j].length() == 0) {
						argIntArray[j] = new int[0];
						continue;
					}
					String[] arr = intArray[j].split(",");
					argIntArray[j] = new int[arr.length];
					for (int k = 0; k < arr.length; k++)
						argIntArray[j][k] = Integer.parseInt(arr[k]);
				}
			}
		}
		return argIntArray;
	}

	private Map<String, Integer> text2Map(String text) {
		Map<String, Integer> daMap = null;
		if (text != null) {
			if (!text.equals("null")) {
				// "pos,i;pos,i;pos,i"
				daMap = new HashMap<>();
				String[] entries = text.split(";");

				for (int j = 0; j < entries.length; j++) {
					String[] arr = entries[j].split(",");
					daMap.put(arr[0], Integer.parseInt(arr[1]));
				}
			}
		}
		return daMap;
	}

	private String map2Text(Map<String, Integer> daMap) {
		// "pos,i;pos,i;pos,i"
		if (daMap == null || daMap.isEmpty())
			return "null";

		StringBuilder strBuilder = new StringBuilder();
		String prefix = "";
		for (Entry<String, Integer> entry : daMap.entrySet()) {
			strBuilder.append(prefix + entry.getKey() + "," + entry.getValue());
			prefix = ";";
		}

		return strBuilder.toString();
	}

	private Long[] text2long1D(String text) {
		Long[] argLong = null;
		if (text != null) {
			if (!text.equals("null")) {
				String[] bigints = text.split(",");
				argLong = new Long[bigints.length];
				for (int j = 0; j < argLong.length; j++)
					argLong[j] = Long.parseLong(bigints[j]);
			}
		}
		return argLong;
	}

	private String long1D2Text(int[] long1D) {
		if (long1D == null || long1D.length == 0)
			return "null";
		StringBuilder strBuilder = new StringBuilder();
		String prefix = "";
		for (long i : long1D) {
			strBuilder.append(prefix + i);
			prefix = ",";
		}
		return strBuilder.toString();
	}

}
