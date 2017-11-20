package MainTasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;

import com.budhash.cliche.Command;
import com.budhash.cliche.Param;
import com.budhash.cliche.Shell;
import com.budhash.cliche.ShellFactory;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import Analyzers.ClusterAnalyzer;
import Analyzers.ExtractCommonPatterns;
import Analyzers.KeywordAnalyzer;
import Analyzers.KeywordExplorer;
import Analyzers.PhraseAnalyzer;
import Analyzers.TrendAnalyzer;
import Datastores.Dataset;
import Datastores.Document;
import Datastores.FileDataAdapter;
import NLP.NatureLanguageProcessor;
import NLP.WordVec;
import TextNormalizer.TextNormalizer;
import Utils.Util;
import nu.xom.jaxen.expr.AdditiveExpr;

public class Alpaca {
	private String dataDirectory = null;
	private Map<String, List<String>> wordTopics = null;
	private Map<String, String> variables = null;
	private WordVec word2vec = null;
	private String additionalTextFile = null;
	private String wordScoreFile = null;
	private Dataset currentDataset = null;
	private int currentLevel = -1;
	private int scoringScheme = -1;
	private Double optional_Similarity_Threshold = null;
	private Map<String, Double> wordScore = null;
	private Map<String, Double> IDFWeights = null;

	public void readWordsSkewness(int typeOfScore, String fileName)
			throws Throwable {
		wordScore = new HashMap<>();
		IDFWeights = new HashMap<>();
		CSVReader reader = new CSVReader(new FileReader(fileName), ',',
				CSVWriter.DEFAULT_ESCAPE_CHARACTER);
		String[] line = reader.readNext();
		while ((line = reader.readNext()) != null) {
			double score = Double.valueOf(line[typeOfScore]);
			double idf = Double.valueOf(line[KeywordAnalyzer.TFIDF]);
			wordScore.put(line[0], score);
			IDFWeights.put(line[0], idf);
		}
		reader.close();
	}

	public Alpaca() {
		// TODO Auto-generated constructor stub
		wordTopics = new HashMap<>();
		variables = new HashMap<>();
	}

	@Command(description = "analyze and extract phrase templates", name = "templates", abbrev = "temp")
	public void extractTemplates(String directory, String fileoutput,
			int numberOfSentence, boolean strict) throws Throwable {
		if (currentLevel == -1) {
			System.out.println("Please choose stemmer level first");
			return;
		}
		String realInput = variables.get(directory);
		if (realInput != null)
			directory = realInput;
		String realOutput = variables.get(fileoutput);
		if (realOutput != null)
			fileoutput = realOutput;
		ExtractCommonPatterns ecps = new ExtractCommonPatterns();
		ecps.processPhrases(directory, numberOfSentence,
				currentLevel, fileoutput, strict);
	}

	@Command(description = "change the source of POS patterns", name = "change_patterns", abbrev = "patt")
	public void changePOSpatterns(String fileInput)
			throws FileNotFoundException, Throwable {
		String realInput = variables.get(fileInput);
		if (realInput != null)
			fileInput = realInput;
		PhraseAnalyzer.getInstance().changePOSpatterns(fileInput);
	}

	@Command
	public String help() {
		return "Show you all the help you might need <UNDER CONSTRUCTION>!";
	}

	@Command(description = "type exit to exit Alpaca.")
	public void exit() {
	}

	@Command(description = "create a new variable", name = "createVariable", abbrev = "var")
	public void createVariable(String... args) {
		if (args.length != 2) {
			System.out.println(
					"Please follow the format: var <variable name> <value>");
			return;
		}
		variables.put(args[0], args[1]);
		System.out.println("Added " + args[0] + " = " + args[1]);

	}

	@Command(description = "create a new topic", name = "createTopic", abbrev = "topic")
	public void createTopic(String... args) {
		List<String> topic = wordTopics.get(args[0]);
		topic = new ArrayList<>();
		for (int i = 1; i < args.length; i++) {
			topic.add(args[i]);
		}
		wordTopics.put(args[0], topic);
		System.out.print("Words in topic <" + args[0] + "> are: ");
		for (String w : topic) {
			System.out.print(w + ", ");
		}
		System.out.println();
	}

	@Command(description = "add word to topic", name = "add2Topic", abbrev = "addt")
	public void addWord2Topic(String... args) {
		List<String> topic = wordTopics.get(args[0]);
		if (topic == null)
			topic = new ArrayList<>();
		for (int i = 1; i < args.length; i++) {
			topic.add(args[i]);
		}
		wordTopics.put(args[0], topic);

		wordTopics.put(args[0], topic);
		System.out.print("Words in topic <" + args[0] + "> are: ");
		for (String w : topic) {
			System.out.print(w + ", ");
		}
		System.out.println();
	}

	@Command(description = "set up data folder", name = "data_folder", abbrev = "dataf")
	public void setupDataFolder(String folder) {
		if (!folder.endsWith("/") && !folder.endsWith("\\"))
			folder += "/";
		dataDirectory = folder;
		System.out.println("The current data folder is " + dataDirectory);
		word2vec = null;
		wordScoreFile = null;
		currentDataset = null;
		currentLevel = -1;

	}

	@Command(description = "set up threshold for similarity when expanding words", name = "threshold")
	public void setupThreshold(double threashold) {
		if (threashold <= 0 || threashold > 1) {
			System.err.println(
					"Threshold can't be less or equal to zero nor it can be bigger than 1");
			return;
		}
		optional_Similarity_Threshold = threashold;
		System.out.println(
				"The current threshold for similarity when expanding words is "
						+ optional_Similarity_Threshold);
	}

	@Command(description = "set up additional text file", name = "text_file", abbrev = "textf")
	public void setupAdditionalTextfFile(String file) {
		additionalTextFile = file;
		System.out.println(
				"The current additional text file is " + additionalTextFile);
	}

	@Command(description = "set up wordscore file", name = "wordscore_file", abbrev = "scowf")
	public void setupWordscoreFile(String file) {
		wordScoreFile = file;
		System.out.println("The current word scoring file is " + wordScoreFile);
	}

	@Command(description = "set up scoring scheme", name = "scoring_scheme", abbrev = "score")
	public void setupScoringScheme(int scr) {
		switch (scr) {
		case KeywordAnalyzer.WEIBULL_FREQUENCY:
			System.out.println("Current scoring scheme is: WEIBULL_FREQUENCY");
			scoringScheme = KeywordAnalyzer.WEIBULL_FREQUENCY;
			break;
		case KeywordAnalyzer.CONTRAST_SCORE:
			System.out.println(
					"Current scoring scheme is: CONTRAST_SCORE (NOTE: Only works on 5 stars review system)");
			scoringScheme = KeywordAnalyzer.CONTRAST_SCORE;
			break;
		case KeywordAnalyzer.MEAN:
			System.out.println("Current scoring scheme is: MEAN_SCORE");
			scoringScheme = KeywordAnalyzer.MEAN;
			break;
		case KeywordAnalyzer.TFIDF:
			System.out.println("Current scoring scheme is: IDF");
			scoringScheme = KeywordAnalyzer.TFIDF;
			break;
		case KeywordAnalyzer.FREQUENCY:
			System.out.println("Current scoring scheme is: FREQUENCY");
			scoringScheme = KeywordAnalyzer.FREQUENCY;
			break;
		default:
			System.out.println("We don't support this scoring scheme");
			break;
		}
	}

	@Command(description = "set up stemmer level", name = "stemmer_level", abbrev = "level")
	public void setupLevel(int level) {
		switch (level) {
		case PreprocesorMain.LV1_SPELLING_CORRECTION:
			System.out.println(
					"Current stemmer level is: LV1_SPELLING_CORRECTION");
			currentLevel = PreprocesorMain.LV1_SPELLING_CORRECTION;
			break;
		case PreprocesorMain.LV2_ROOTWORD_STEMMING:
			System.out
					.println("Current stemmer level is: LV2_ROOTWORD_STEMMING");
			currentLevel = PreprocesorMain.LV2_ROOTWORD_STEMMING;
			break;
		case PreprocesorMain.LV3_OVER_STEMMING:
			System.out.println("Current stemmer level is: LV3_OVER_STEMMING");
			currentLevel = PreprocesorMain.LV3_OVER_STEMMING;
			break;
		case PreprocesorMain.LV4_ROOTWORD_STEMMING_LITE:
			System.out.println(
					"Current stemmer level is: LV4_ROOTWORD_STEMMING_LITE");
			currentLevel = PreprocesorMain.LV4_ROOTWORD_STEMMING_LITE;
			break;
		default:
			System.out.println("We don't support this level of preprocessing");
			break;
		}
	}

	@Command(description = "set up vector file", name = "vector_file", abbrev = "vectorf")
	public void setupVectorFile(String file) {

		word2vec = new WordVec(file);

	}

	@Command(description = "cluster words from a file", name = "cluster_from_file", abbrev = "clf")
	public void clusterFromFile(String inputfileName, String outputFileName,
			int scoring) throws Throwable {
		if (wordScoreFile == null) {
			System.out.println("you have to set up a wordscore file first!");
			return;
		}
		if (word2vec == null) {
			System.out.println("you have to set up a vector file first!");
			return;
		}
		String realInput = variables.get(inputfileName);
		if (realInput != null)
			inputfileName = realInput;
		String realOutput = variables.get(outputFileName);
		if (realOutput != null)
			outputFileName = realOutput;

		// String directory = dataDirectory + "clusters/";
		// File fDirectory = new File(directory);
		// if (!fDirectory.exists()) {
		// fDirectory.mkdirs();
		// // If you require it to make the entire directory path including
		// // parents,
		// // use directory.mkdirs(); here instead.
		// }
		// outputFileName = directory + outputFileName;
		ClusterAnalyzer.clusterWords(outputFileName, word2vec,
				ClusterAnalyzer.AVERAGE_JACCARD, inputfileName, wordScoreFile,
				scoring);
	}

	private void print2Files(Dataset dataset, Set<String> results)
			throws FileNotFoundException {
		String answer = "";
		Scanner scanner = new Scanner(System.in);
		while (!answer.equalsIgnoreCase("N")) {
			System.out
					.println("Do you want to print these words to file? (Y/N)");
			answer = scanner.nextLine();
			if (answer.equalsIgnoreCase("Y")) {
				System.out.println("Please type in the file name:");
				String fileName = scanner.nextLine();
				String realInput = variables.get(fileName);
				if (realInput != null)
					fileName = realInput;
				PrintWriter pw = new PrintWriter(new File(fileName));
				for (String w : results) {
					pw.println(w);
				}
				System.out.println("done printing");
				pw.close();
				break;
			}
		}

	}

	@Command(description = "analyze trends and write to file", name = "trends", abbrev = "trd")
	public void analyzeTrends(String inputFile, String directory) throws Exception {
		// current version only support non major topic
		boolean onlyAsMajorTopic = false;
		String realInput = variables.get(inputFile);
		if (realInput != null)
			inputFile = realInput;
		String realOutput = variables.get(directory);
		if (realOutput != null)
			directory = realOutput;
		if (!directory.endsWith("/") && !directory.endsWith("\\"))
			directory += "/";
		Set<String> termset = new HashSet<>();
		Scanner scn = new Scanner(new File(inputFile));
		while (scn.hasNext()) {
			termset.add(scn.nextLine());
		}
		scn.close();

		File fDirectory = new File(directory);
		if (!fDirectory.exists()) {
			fDirectory.mkdirs();
		}

		if (currentDataset == null)
			currentDataset = Alpaca.readProcessedData(dataDirectory,
					currentLevel);
		// just frequency
		System.out.println(
				"Analyzing raw frequency trends (appearance over years)");
		Map<Integer, Integer> rawFtrends = null;
		if (onlyAsMajorTopic)
			rawFtrends = TrendAnalyzer.getPhraseTrend_Frequency(currentDataset,
					termset, true);
		else
			rawFtrends = TrendAnalyzer.getPhraseTrend_Frequency(currentDataset,
					termset, false);

		System.out.println(
				"Analyzing portional frequency trends (percentage of appearance over total reviews over years)");
		Map<Integer, Float> porFtrends = null;

		if (onlyAsMajorTopic)
			porFtrends = TrendAnalyzer.getPhraseTrend_Percentage(currentDataset,
					termset, true);
		else
			porFtrends = TrendAnalyzer.getPhraseTrend_Percentage(currentDataset,
					termset, false);
		Map<Integer, Integer> valueTrends = null;

		if (onlyAsMajorTopic)
			valueTrends = TrendAnalyzer.getPhraseTrend_valueAccumulation(
					currentDataset, termset, true);
		else
			valueTrends = TrendAnalyzer.getPhraseTrend_valueAccumulation(
					currentDataset, termset, false);
		Map<Integer, Integer> totalCount = TrendAnalyzer
				.gatherFrequencyOverYearAll(currentDataset);
		PrintWriter pw = new PrintWriter(
				new File(directory + "portionalFrequencyTrends.csv"));
		pw.println(
				"year,percentage of documents, frequency, accumulated value/rating, total documents of this dataset");
		for (Entry<Integer, Float> entry : porFtrends.entrySet()) {
			int key = entry.getKey();
			Integer rawFreq = rawFtrends.get(key);
			Integer value = valueTrends.get(key);
			Integer total = totalCount.get(key);
			if (value == null)
				value = 0;
			pw.println(key + "," + entry.getValue() + "," + rawFreq + ","
					+ value + "," + total);
		}
		pw.close();
		System.out.println("done printing");

	}

	@Command(description = "expand words from a topic to phrases", name = "expand", abbrev = "expt")
	public void expandWords_topic(String... args) throws Throwable {
		List<String> words = wordTopics.get(args[0]);
		if (words == null) {
			System.out.println("You haven't defined this topic yet!");
			return;
		}
		if (word2vec == null) {
			System.out.println("you have to set up a vector file first!");
			return;
		}
		if (wordScoreFile == null) {
			System.out.println("you have to set up a word scoring file first!");
			return;
		}
		if (args.length == 2)
			setupThreshold(Double.parseDouble(args[1]));
		readWordsSkewness(scoringScheme, wordScoreFile);
		if (currentDataset == null)
			currentDataset = Alpaca.readProcessedData(dataDirectory,
					currentLevel);
		Set<String> result = KeywordExplorer.expand(words, word2vec,
				currentDataset, wordScore, optional_Similarity_Threshold,
				IDFWeights);
		print2Files(currentDataset, result);
		System.out.println();

	}

	@Command(description = "expand words from a file to phrases", name = "expand_from_file", abbrev = "expf")
	public void expandWords_file(String... args) throws Throwable {
		String realInput = variables.get(args[0]);
		if (realInput != null)
			args[0] = realInput;
		if (word2vec == null) {
			System.out.println("you have to set up a vector file first!");
			return;
		}
		if (wordScoreFile == null) {
			System.out.println("you have to set up a word scoring file first!");
			return;
		}
		if (args.length == 2)
			setupThreshold(Double.parseDouble(args[1]));
		readWordsSkewness(scoringScheme, wordScoreFile);
		if (currentDataset == null)
			currentDataset = Alpaca.readProcessedData(dataDirectory,
					currentLevel);
		List<String> termset = new ArrayList<>();
		Scanner scn = new Scanner(new File(args[0]));
		while (scn.hasNext()) {
			termset.add(scn.nextLine());
		}
		scn.close();
		Set<String> result = KeywordExplorer.expand(termset, word2vec,
				currentDataset, wordScore, optional_Similarity_Threshold,
				IDFWeights);
		print2Files(currentDataset, result);
		System.out.println();

	}

	@Command(description = "preprocessing data", name = "preprocess", abbrev = "prep")
	public void preprocessing() {
		if (dataDirectory == null) {
			System.out.println("Please set up you data folder first");
			return;
		}
		if (currentLevel == -1) {
			System.out.println("Please choose stemmer level first");
			return;
		}

		if (additionalTextFile == null) {
			System.out.println(
					"Notice: No additional text file chosen, we will train with just text from this dataset");
		} else {
			System.out.println(
					"Notice: we will train with text from both this dataset and additional file at: "
							+ additionalTextFile);
		}
		try {

			System.out.println(
					"Begin processing the dataset in: " + dataDirectory);
			currentDataset = PreprocesorMain.processDBData(dataDirectory,
					currentLevel, additionalTextFile);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Command(description = "extract and score keywords", name = "score_keyword", abbrev = "keyw")
	public void analyzeKeyword(boolean isFiveStarReview) throws Exception {
		if (currentLevel == -1) {
			System.out.println("Please choose stemmer level first");
			return;
		}
		if (currentDataset == null)
			currentDataset = Alpaca.readProcessedData(dataDirectory,
					currentLevel);
		KeywordAnalyzer analyzer = new KeywordAnalyzer();
		analyzer.calculateAndWriteKeywordScore(currentDataset, currentLevel,
				isFiveStarReview);
	}

	@Command(description = "run configuration script", name = "scripting", abbrev = "script")
	public void readConfigScript(String fileName)
			throws FileNotFoundException, Throwable {
		Scanner scn = null;
		String dataf = null;
		String conf = null;
		String scowf = null;
		String textf = null;
		String patt = null;
		String[] args = null;
		int level = -1;
		int score = -1;
		double threshold = -1;
		String vectorf = null;
		try {
			scn = new Scanner(new File(fileName));
			int lineCount = 1;
			while (scn.hasNext()) {
				String[] line = scn.nextLine().split(" ");
				if(line[0].charAt(0) == '%')
					continue;
						
				switch (line[0]) {
				case "dataf":
					if (line.length != 2) {
						System.out
								.println("Error reading at line " + lineCount);
						return;
					}
					dataf = line[1];
					break;
				case "conf":
					if (line.length != 2) {
						System.out
								.println("Error reading at line " + lineCount);
						return;
					}
					conf = line[1];
					break;
				case "level":
					if (line.length != 2) {
						System.out
								.println("Error reading at line " + lineCount);
						return;
					}
					level = Integer.parseInt(line[1]);
					break;
				case "vectorf":
					if (line.length != 2) {
						System.out
								.println("Error reading at line " + lineCount);
						return;
					}
					vectorf = line[1];
					break;
				case "scowf":
					if (line.length != 2) {
						System.out
								.println("Error reading at line " + lineCount);
						return;
					}
					scowf = line[1];
					break;
				case "topic":

					if (line.length <= 2) {
						System.out
								.println("Error reading at line " + lineCount);
						return;
					}
					args = new String[line.length - 1];
					for (int i = 1; i < line.length; i++)
						args[i - 1] = line[i];
					createTopic(args);
					break;
				case "addt":

					if (line.length <= 2) {
						System.out
								.println("Error reading at line " + lineCount);
						return;
					}
					args = new String[line.length - 1];
					for (int i = 1; i < line.length; i++)
						args[i - 1] = line[i];
					addWord2Topic(args);
					break;
				case "score":
					if (line.length != 2) {
						System.out
								.println("Error reading at line " + lineCount);
						return;
					}
					score = Integer.parseInt(line[1]);
					break;
				case "threshold":
					if (line.length != 2) {
						System.out
								.println("Error reading at line " + lineCount);
						return;
					}
					threshold = Double.parseDouble(line[1]);
					break;
				case "textf":
					if (line.length != 2) {
						System.out
								.println("Error reading at line " + lineCount);
						return;
					}
					textf = line[1];
					break;
				case "patt":
					if (line.length != 2) {
						System.out
								.println("Error reading at line " + lineCount);
						return;
					}
					patt = line[1];
					break;
				case "var":
					if (line.length != 3) {
						System.out
								.println("Error reading at line " + lineCount);
						return;
					}
					createVariable(new String[] { line[1], line[2] });
					break;
				default:
					System.out.println("Can't intepret line " + lineCount);
					break;
				}
				lineCount++;
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			System.out.println("This file doesnt exist!");
			return;
		} catch (NumberFormatException e) {
			System.out.println(
					"The number format for stemmer level is not right");
			return;
		} finally {
			if (scn != null)
				scn.close();
		}
		if (dataf != null)
			setupDataFolder(dataf);
		if (level != -1)
			setupLevel(level);
		if (score != -1)
			setupScoringScheme(score);
		if (vectorf != null)
			setupVectorFile(vectorf);
		if (conf != null)
			readConfigINI(conf);
		if (textf != null)
			setupAdditionalTextfFile(textf);
		if (scowf != null)
			setupWordscoreFile(scowf);
		if (patt != null)
			changePOSpatterns(patt);
		if (threshold != -1)
			setupThreshold(threshold);
	}

	@Command(description = "setup the config file for text analyzer module", name = "config_file", abbrev = "conf")
	public void readConfigINI(String fileName) {
		TextNormalizer normalizer = TextNormalizer.getInstance();
		try {
			normalizer.readConfigINI(fileName);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.err.println("Cannot find the file specified.");
		}
	}

	public static void main(String[] args) throws IOException {
		ShellFactory
				.createConsoleShell("Alpaca ",
						"Enter '?list' to list all commands", new Alpaca())
				.commandLoop();
	}

	@Command(name = "cat", abbrev = "", header = "The concatenation is:")
	public String concatenate(String... strings) {
		String result = "";
		for (String s : strings) {
			result += s;
		}
		return result;
	}

	private static Dataset loadDataset(String directory, int level)
			throws Exception {
		Dataset dataset = PreprocesorMain.readRawData(directory, level);
		return dataset;
	}

	public static Dataset readProcessedData(String directory, int level)
			throws Exception {
		System.out.println("Reading preprocessed data for the first time");
		String cleansedTextLocationDir = FileDataAdapter.getLevelLocationDir(
				FileDataAdapter.CLEANSED_SUBDIR, directory, level);

		String metaDataFileName = cleansedTextLocationDir + "metadata.csv";

		Dataset dataset = null;
		File fcheckExist = new File(metaDataFileName);
		if (!fcheckExist.exists()) {
			throw new FileNotFoundException(
					"This file can't be found: " + metaDataFileName);
		}
		
		int totalDoc = Util.listFilesForFolder(directory + "rawData//").toArray(new String[]{}).length;
		double percentageCompleted = 0, docCompleted = 0;
		CSVReader reader = null;
		int count = 0;
		try {
			reader = new CSVReader(new FileReader(metaDataFileName), ',',
					CSVWriter.DEFAULT_ESCAPE_CHARACTER);
			String[] line = reader.readNext(); // read first line to get dataset
												// info
			if (line != null) {

				String name = line[0];
				String description = line[1];
				boolean has_rating = Boolean.parseBoolean(line[2]);
				boolean has_time = Boolean.parseBoolean(line[3]);
				boolean has_author = Boolean.parseBoolean(line[4]);
				String otherMetadata = line[5];
				// add to database, get id back
				dataset = new Dataset(name, description, has_time, has_rating,
						has_author, otherMetadata, directory, level);
				while ((line = reader.readNext()) != null) {
					String rawtext_fileName = line[0];
					int rating = -1;
					if (has_rating)
						rating = Integer.parseInt(line[1]);
					long time = -1;
					if (has_time)
						time = Long.parseLong(line[2]);
					String author = null;
					if (has_author)
						author = line[3];
					boolean isEnglish = Boolean.parseBoolean(line[4]);
					if (rawtext_fileName == null)
						throw new Exception("Line " + count
								+ ": no raw text data file, aborting");
					// add to dataset
					Document doc = new Document(rawtext_fileName, rating, time,
							isEnglish, author);
					dataset.addDocument(doc);
					doc.populatePreprocessedDataFromDB(level, dataset);
					count++;
					// if (count % 100 == 0)
					// System.out.println("read in " + count + " documents");
					// if (count % 51000 == 0)
					// System.out.println("read in " + count + " documents");
					docCompleted++;
					double newPercentage = Util.round(100 * docCompleted / totalDoc,
							2);
					if (newPercentage > percentageCompleted) {
						percentageCompleted = newPercentage;
						Util.printProgress(percentageCompleted);
					}
				}
				dataset.getVocabulary().loadDBKeyword();
				System.out.println("read in " + count + " documents");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw e;
		} finally {
			if (reader != null)
				reader.close();
		}
		return dataset;
	}
}
