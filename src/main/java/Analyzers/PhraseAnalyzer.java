package Analyzers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import Datastores.Dataset;
import Datastores.Document;
import NLP.NatureLanguageProcessor;
import NLP.WordVec;
import StanfordNLP.StanfordNLPProcessing;
import TextNormalizer.TextNormalizer;
import Utils.POSTagConverter;
import Utils.Util;
import Vocabulary.DBWord;
import Vocabulary.Vocabulary;

import java.util.Map.Entry;

import com.google.common.collect.ImmutableList;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class PhraseAnalyzer {
	private static boolean DEBUG = false;
	public static final int SCORING_USER = 1;
	public static final int SCORING_STANFORD_SENTIMENT = 2;
	public static final int SCORING_REVIEW_CONTRAST = 3;
	private static int GOOGLE = 1;
	private static int TEMPLATE = 2;
	private static PhraseAnalyzer instance = null;
	private static int PARSETREE = 3;
	private static int TEMPLATE_NEW = 4;
	private static int TEMPLATE_Len_Sent_Contrast = 5;
	private static int NGRAM_LENGTH = 8;
	private List<Set<String>> categories = new ArrayList<>();
	private static final HashSet<String> POSTAG_OF_VOCABULARY = new HashSet<>(
			Arrays.asList(new String[] { "NN", "ADJP", "JJ", "VBG", "VBP",
					"VBN", "VBZ", "VB", "NP", "VP", "NNS", "@VP" }));
	// dist[6]: first 5 for ratings, 6th for rank of rating bad, 7th for rank of
	// rating good
	// <monthYear, <ngram, dist>>
	private Map<Integer, Map<String, int[]>> ngramAll = new HashMap<>();
	private Set<Byte> interestingPOS = new HashSet<>();
	private Set<String> connectors = new HashSet<>();
	private Set<String> sentimentors = new HashSet<>();
	private Set<String> negations = new HashSet<>();
	private Set<String> intensifiers = new HashSet<>();
	private Set<String> topicWords = new HashSet<>();
	private Set<String> others = new HashSet<>();
	private Set<String> interestingWords = new HashSet<>();
	private Set<String> essentialWords = new HashSet<>();
	private Set<String> connNegInt = new HashSet<>();
	private static final HashSet<String> POSTAG_OF_NOUN = new HashSet<>(
			Arrays.asList(new String[] { "NN", "NNS", "NP" }));
	private static final HashSet<String> POSTAG_OF_ADJECTIVE = new HashSet<>(
			Arrays.asList(new String[] { "ADJP", "JJ" }));
	private static final HashSet<String> POSTAG_OF_VERB = new HashSet<>(
			Arrays.asList(new String[] { "VBG", "VBP", "VB", "VP", "@VP", "VBZ",
					"VBN" }));
	private Map<String, Double> wordScore = null;
	private static final String SCORING_FILENAME = "D:\\projects\\ALPACA\\NSF\\wordScore\\scoreLV2.csv";
	Set<Long> posPattern = null;
	private static final Map<ImmutableList<Integer>, Integer> TRAINING_DATA_INSTANCE_FREQ = new HashMap<>();
	static final Map<ImmutableList<Integer>, Integer> TESTING_DATA_INSTANCE_FREQ = new HashMap<>();

	public static PhraseAnalyzer getInstance() throws Throwable {
		if (instance == null) {
			instance = new PhraseAnalyzer();
		}
		return instance;
	}

	private PhraseAnalyzer() throws Throwable {
		// TODO Auto-generated constructor stub
		POSTagConverter POSconverter = POSTagConverter.getInstance();
		readCategories();
		for (String pos : POSTAG_OF_ADJECTIVE) {
			interestingPOS.add(POSconverter.getCode(pos));
		}
		for (String pos : POSTAG_OF_VERB) {
			interestingPOS.add(POSconverter.getCode(pos));
		}
		for (String pos : POSTAG_OF_NOUN) {
			interestingPOS.add(POSconverter.getCode(pos));
		}
		TextNormalizer.getInstance();
		connectors.addAll(
				loadWordsSet(new File(TextNormalizer.getDictionaryDirectory()
						+ "baseword/misc/connectors.txt")));
		TextNormalizer.getInstance();
		negations.addAll(
				loadWordsSet(new File(TextNormalizer.getDictionaryDirectory()
						+ "baseword/misc/negations.txt")));
		TextNormalizer.getInstance();
		intensifiers.addAll(
				loadWordsSet(new File(TextNormalizer.getDictionaryDirectory()
						+ "baseword/misc/intensifiers.txt")));
		connNegInt.addAll(connectors);
		connNegInt.addAll(negations);
		connNegInt.addAll(intensifiers);
		interestingWords.addAll(connectors);
		interestingWords.addAll(negations);
		interestingWords.addAll(intensifiers);
		sentimentors.addAll(loadWordsSet(
				new File("D:\\projects\\concernsReviews\\ADJ\\badWords.csv")));
		sentimentors.addAll(loadWordsSet(
				new File("D:\\projects\\concernsReviews\\ADJ\\goodWords.csv")));
		others.addAll(loadWordsSet(
				new File("D:\\projects\\concernsReviews\\ADJ\\others.txt")));
		interestingWords.addAll(sentimentors);
		interestingWords.addAll(others);
		List<String> topicFiles = Util
				.listFilesForFolder("D:\\projects\\concernsReviews\\topics");
		for (String topicFile : topicFiles) {
			topicWords.addAll(loadWordsSet(new File(topicFile)));
		}
		connNegInt.addAll(others);
		POSconverter.extendPOSLIST(connNegInt);
		posPattern = getPOSpatterns(
				"D:\\projects\\concernsReviews\\POSpatterns\\posPattern.csv");
		interestingWords.addAll(topicWords);
		essentialWords.addAll(topicWords);
		essentialWords.addAll(sentimentors);
		essentialWords.addAll(others);

		// readData(minReviews, appMan,
		// "D:\\projects\\concernsReviews\\appCategories\\" + TOPIC
		// + ".txt");
		// updatePOSTAG_DB();
	}

	public Set<Long> getPOSpatterns(String infile)
			throws FileNotFoundException {
		Set<Long> results = new HashSet<>();
		Scanner br = new Scanner(new FileReader(
				"D:\\projects\\concernsReviews\\POSpatterns\\posPattern.csv"));
		br.nextLine();
		while (br.hasNextLine()) {
			String[] words = br.nextLine().split(",");
			long pattern = POSTagConverter.getInstance().string2long(words[0]);
			results.add(pattern);
			// int length = words[0].split("_").length;
			// int freq = Integer.parseInt(words[1]);
			// double score = 1.0
			// / (1.0 / (double) length + 1.0 / (1.0 + Util.log(freq, 2)));
			// POS_PATTERN_SCORE.put(pattern, score);
		}
		br.close();
		return results;
	}

	public static Set<String> loadWordsSet(File testDataFile)
			throws FileNotFoundException {
		// TODO Auto-generated method stub
		Set<String> results = new HashSet<>();
		Scanner br = new Scanner(new FileReader(testDataFile));
		while (br.hasNextLine()) {
			String[] words = br.nextLine().split(",");
			results.add(words[0]);
		}
		br.close();
		return results;
	}

	private void readCategories() throws FileNotFoundException {
		categories.add(loadWordsSet(new File(
				"D:\\projects\\concernsReviews\\appCategories\\entertainment.txt")));
		categories.add(loadWordsSet(new File(
				"D:\\projects\\concernsReviews\\appCategories\\messagers.txt")));
		categories.add(loadWordsSet(new File(
				"D:\\projects\\concernsReviews\\appCategories\\multimedia.txt")));
		categories.add(loadWordsSet(new File(
				"D:\\projects\\concernsReviews\\appCategories\\utilities.txt")));
		categories.add(loadWordsSet(new File(
				"D:\\projects\\concernsReviews\\appCategories\\work.txt")));
	}

	public void readWordsSkewness(int typeOfScore, String fileName)
			throws Throwable {
		wordScore = new HashMap<>();
		CSVReader reader = new CSVReader(new FileReader(fileName), ',',
				CSVWriter.DEFAULT_ESCAPE_CHARACTER);
		String[] line = reader.readNext();
		Set<String> stopWord = NatureLanguageProcessor.getInstance()
				.getStopWordSet();
		while ((line = reader.readNext()) != null) {
			if (stopWord.contains(line[0]))
				continue;
			double score = Double.valueOf(line[typeOfScore]);
			wordScore.put(line[0], score);
		}
		reader.close();
	}

	public void extractFeatures(Dataset dataset, String seedFileOutput,
			int level, int scoring) throws Throwable {

		// <monthYear, <ngram, dist>>
		Map<Integer, Map<String, int[]>> ngramAll_temp = new HashMap<>();
		// <monthYear, <app,<ngram, dist>>>
		Map<Integer, Map<String, Map<String, int[]>>> ngramEach_temp = new HashMap<>();
		// List<Application> appList = appMan.getAppList();
		PrintWriter seedFile = new PrintWriter(new File(seedFileOutput));
		int docCount = 0;
		for (Document doc : dataset.getDocumentSet()) {
			if (!doc.isEnglish())
				continue;
			doc.populatePreprocessedDataFromDB(level, dataset);
			int rating = doc.getRating();

			// String revString = doc.toString(false);
			// wordCount += rev.toString().split(" ").length;
			Set<Item> nGrams = null;
			nGrams = extractSequence_ByPattern_LENSENTCON(doc.getSentences(),
					posPattern, true, scoring, dataset.getVocabulary());
			if (nGrams == null)
				continue;
			for (Item gram : nGrams) {
				seedFile.println(gram.gram + "," + doc.getRawTextFileName()
						+ "," + gram.badScore);
			}
			// int month = Util.getMonth(doc.getTime());
			// continue;
			// updateAllDist(rating, nGrams, month, ngramAll_temp);

			docCount++;
			if (docCount % 1000 == 0)
				System.out.println("Processed " + docCount);
		}

		System.out.println("Processed " + docCount);
		// calculate distributions
		// ngramAll.putAll(ngramAll_temp);
		System.out.println("Done extracting phrases");
		seedFile.close();
	}

	private void updateAllDist(int rating, Set<Item> nGrams, int month,
			Map<Integer, Map<String, int[]>> ngramAll_temp) {
		Map<String, int[]> nGramDistByTime = ngramAll_temp.get(month);
		if (nGramDistByTime == null) {
			nGramDistByTime = new HashMap<>();
		}
		for (Item gram : nGrams) {
			int[] dist = nGramDistByTime.get(gram.gram);
			if (dist == null) {
				dist = new int[5];
			}
			dist[rating - 1]++;
			nGramDistByTime.put(gram.gram, dist);
		}
		ngramAll_temp.put(month, nGramDistByTime);
	}

	// choosing phrase: log(length) * sentiment (stanford) * log(skewness).
	public Set<Item> extractSequence_ByPattern_LENSENTCON(int[][] sentences,
			Set<Long> patternSet, boolean actualString, int scoring,
			Vocabulary voc) throws Throwable {
		Set<Item> sequenceSet = new HashSet<>();

		if (sentences == null) {
			// System.out.println( // maybe put the original sentence already
			// // splitted here to by pass this.
			// "ERROR: Cannot map sentence with POS. Ignore this review");
			return sequenceSet;
		}
		for (int s = 0; s < sentences.length; s++) {
			int[] wordList = sentences[s];
			List<Seed> results = getsequence_LocalOptimal(wordList, patternSet,
					voc);
			if (results == null)
				continue;
			for (Seed seed : results) {
				StringBuilder strBld = new StringBuilder();
				for (int i = seed.start; i <= seed.end; i++) {
					strBld.append(voc.getWordFromDB(wordList[i]).getText())
							.append(" ");
				}
				Item item = new Item(strBld.toString(),
						Util.round(seed.score, 4), 0, null);
				sequenceSet.add(item);
			}
			// SequenceScore result = calculateMaxSequenceScores(wordList,
			// patternSet, scoring);
			// if (result != null && result.score != 0)
			// sequenceSet.addAll(getRepresentableSequences_EXPANDED(wordList,
			// result, actualString));
			// else
			// nullCount++;
		}
		// System.out.println(
		// "number of sentences without a any extractable phrase: "
		// + nullCount);
		return sequenceSet;
	}

	public Set<Item> getRepresentableSequences_EXPANDED(int[] wordList,
			SequenceScore seqInfo, boolean actualString, Vocabulary voc)
			throws Throwable {
		Set<Item> sequenceSet = new HashSet<>();
		Item firstItem = null;
		POSTagConverter POSconverter = POSTagConverter.getInstance();
		List<int[]> expandedPhrases = new ArrayList<>();
		for (int i = 0; i < seqInfo.mStart.size(); i++) {
			int start = seqInfo.mStart.get(i);
			int end = seqInfo.mEnd.get(i);

			StringBuilder choosenSequence = new StringBuilder();
			StringBuilder choosenActualStr = new StringBuilder();

			for (int w = start; w <= end; w++) {
				String word = voc.getWordFromDB(wordList[w]).getText();
				byte POS = voc.getWordFromDB(wordList[w]).getPOS();
				String representativeToken = POSconverter.getTag(POS);
				if (representativeToken == null || interestingPOS.contains(POS)
						|| interestingWords.contains(word))
					representativeToken = word;
				choosenSequence.append(representativeToken).append("_");
				choosenActualStr.append(word).append(" ");
			}

			// add next word to see how it's goin
			int[] rangeResult = { start, end };
			expandedPhrases.add(rangeResult);
			StringBuilder str = new StringBuilder();
			for (int j = rangeResult[0]; j <= rangeResult[1]; j++) {
				str.append(wordList[j]).append(" ");
			}
			String seq = choosenSequence
					.deleteCharAt(choosenSequence.length() - 1).toString();

			if (DEBUG) {
				System.out.println(seq);
				System.out.println(str.toString());
			}
			if (firstItem != null) {
				Item it = new Item(seq, 0, 0, null);
				if (actualString)
					it.actualPhrase.add(str.toString());
				sequenceSet.add(it);
			} else {
				firstItem = new Item(seq, 0, 0, null);
				if (actualString)
					firstItem.actualPhrase.add(str.toString());
				sequenceSet.add(firstItem);
			}
		}

		return sequenceSet;
	}

	private static class Seed {
		private int start;
		private int end;
		private boolean isTemplated;
		private double score;

		public Seed(int s, int e) {
			start = s;
			end = e;
			isTemplated = false;
			score = -1;
		}

		public void calculateScore(Vocabulary voc, int[] wordList,
				Map<String, Double> wordScore) throws Exception {
			calculateScore_beta(voc, wordList, wordScore);
		}

		private void calculateScore_alpha(Vocabulary voc, int[] wordList,
				Map<String, Double> wordScore) throws Exception {
			double sum = 0;
			for (int i = start; i <= end; i++) {
				Double value = wordScore
						.get(voc.getWordFromDB(wordList[i]).getText());
				if (value == null)
					continue;
				sum += value;
			}
			score = Util.log(sum + 1, 10) / (end - start + 1);
		}

		// score = sum(all individual words) * log(length)/length
		private void calculateScore_beta(Vocabulary voc, int[] wordList,
				Map<String, Double> wordScore) throws Exception {
			double sum = 0;
			for (int i = start; i <= end; i++) {
				Double value = wordScore
						.get(voc.getWordFromDB(wordList[i]).getText());
				if (value == null)
					continue;
				sum += value;
			}
			int length = (end - start + 1);
			score = sum * Util.log(length, 10) / length;
		}
	}

	// modified matrix chain order to find all possible parenthesization.
	public List<Seed> getsequence_LocalOptimal(int[] wordList,
			Set<Long> patternSet, Vocabulary voc) throws Throwable {
		// step 1: find seeds (a sequence of Verb/Noun/ADJ)
		// step 2: find seed with max score.
		// score = sum(all individual words) * log(length)/length
		// step 3: expand and compare to get the one with better score.
		// ==> create a new set of seeds
		// step 4: check the new set if any seed matches with the templates,
		// store them and discard the previous level matches
		Set<Seed> templatedSeed = new HashSet<>();
		// step1, 2
		int i = 0;
		int start = -1;
		Seed biggestSeed = null;
		int biggestSeedLocation = -1;
		List<Seed> seedList = new ArrayList<>();
		while (i < wordList.length) {
			DBWord word = voc.getWordFromDB(wordList[i]);
			String POS = POSTagConverter.getInstance().getTag(word.getPOS());
			if (POSTAG_OF_VOCABULARY.contains(POS)) {
				if (start == -1) {
					start = i;
				}
				if (i == wordList.length - 1) {
					Seed newSeed = new Seed(start, i - 1);
					newSeed.calculateScore(voc, wordList, wordScore);
					if (isTemplated(wordList, patternSet, newSeed.start,
							newSeed.end, voc)) {
						newSeed.isTemplated = true;
						templatedSeed.add(newSeed);
					}
					seedList.add(newSeed);
					start = -1;
					if (biggestSeed == null) {
						biggestSeed = newSeed;
						biggestSeedLocation = seedList.size() - 1;
					} else {
						if (newSeed.score > biggestSeed.score) {
							biggestSeed = newSeed;
							biggestSeedLocation = seedList.size() - 1;
						}
					}
				}
			} else {
				if (start != -1) {
					Seed newSeed = new Seed(start, i - 1);

					newSeed.calculateScore(voc, wordList, wordScore);
					if (isTemplated(wordList, patternSet, newSeed.start,
							newSeed.end, voc)) {
						newSeed.isTemplated = true;
						templatedSeed.add(newSeed);
					}
					seedList.add(newSeed);
					start = -1;
					if (biggestSeed == null) {
						biggestSeed = newSeed;
						biggestSeedLocation = seedList.size() - 1;

					} else {
						if (newSeed.score > biggestSeed.score) {
							biggestSeed = newSeed;
							biggestSeedLocation = seedList.size() - 1;
						}
					}
				}
			}
			i++;
		}
		if (biggestSeed == null)
			return null;
		int biggestStart = -1, biggestEnd = -1;
		if (biggestSeed.isTemplated) {
			biggestStart = biggestSeed.start;
			biggestEnd = biggestSeed.end;
		}

		while (true) { // break conditions are inside, includes:
						// no more right and left seed to expand
						// the expanded seeds score less than previous seed

			// step 3
			// expand biggest seed to the right and left
			Seed leftNewSeed = new Seed(-1, -1),
					rightNewSeed = new Seed(-1, -1);
			if (biggestSeedLocation != -1) { // found some seeds
				if ((biggestSeedLocation - 1) >= 0) {// left
					leftNewSeed = new Seed(
							seedList.get(biggestSeedLocation - 1).start,
							biggestSeed.end);
					if (isTemplated(wordList, patternSet, leftNewSeed.start,
							leftNewSeed.end, voc)) {
						leftNewSeed.isTemplated = true;
					}
					leftNewSeed.calculateScore(voc, wordList, wordScore);

				}
				if ((biggestSeedLocation + 1) < seedList.size()) {// right
					rightNewSeed = new Seed(biggestSeed.start,
							seedList.get(biggestSeedLocation + 1).end);
					if (isTemplated(wordList, patternSet, rightNewSeed.start,
							rightNewSeed.end, voc))
						rightNewSeed.isTemplated = true;
					rightNewSeed.calculateScore(voc, wordList, wordScore);
				}
			} else {
				break; // no seed
			}
			if (leftNewSeed.start == -1 && rightNewSeed.start == -1)
				break; // can't find adjacent seeds
			Seed maxSeed = biggestSeed;
			if (leftNewSeed.score > maxSeed.score)
				maxSeed = leftNewSeed;
			if (rightNewSeed.score > maxSeed.score)
				maxSeed = rightNewSeed;
			if (maxSeed == biggestSeed)
				break; // both expanded seeds are smaller than last seed
			if (maxSeed.isTemplated) {
				templatedSeed.add(maxSeed);
				biggestStart = maxSeed.start;
				biggestEnd = maxSeed.end;
			}
			// update current seedlist and the new biggest seed
			List<Seed> tempList = new ArrayList<>();
			for (int s = 0; s < seedList.size(); s++) {
				if (maxSeed == leftNewSeed && s == (biggestSeedLocation - 1)) {
					tempList.add(maxSeed);
					biggestSeedLocation = tempList.size() - 1;
					biggestSeed = maxSeed;
					s++; // jump over one
				}
				if (maxSeed == rightNewSeed && s == (biggestSeedLocation)) {
					tempList.add(maxSeed);
					biggestSeedLocation = tempList.size() - 1;
					biggestSeed = maxSeed;
					s++; // jump over one
				}
				tempList.add(seedList.get(s));
			}

			if (tempList.size() == 4 && biggestSeedLocation == 6
					&& seedList.size() == 8)
				System.err.println();
			seedList = tempList;
		}
		// step 4: choose non over-lapping longer templated seeds
		List<Seed> results = new ArrayList<>();
		for (Seed seed : templatedSeed) {
			if (seed.start >= biggestStart && seed.end <= biggestEnd) {
				// belongs to the killed range
			} else {
				if (seed.score > 0)
					results.add(seed);
			}
		}
		return results;
	}

	// modified matrix chain order to find all possible parenthesization.
	public SequenceScore calculateMaxSequenceScores(int[] wordList,
			Set<Long> patternSet, int scoring, Vocabulary voc)
			throws Throwable {
		int n = wordList.length;
		double[][] maxCost = new double[n][n];
		int[][] split = new int[n][n];

		for (int i = 0; i < n; i++) // init the single words' cost
		{
			switch (scoring) {
			case SCORING_USER:
				maxCost[i][i] = computeScoreForSequence_SKEWNESS(wordList,
						patternSet, i, i, voc);
				break;
			case SCORING_STANFORD_SENTIMENT:
				maxCost[i][i] = computeScoreForSequence_STANFORDS_SENTIMENT(
						wordList, patternSet, i, i, voc);
				break;
			}
			split[i][i] = -1; // no split
		}
		for (int lenMinusOne = 1; lenMinusOne < n; lenMinusOne++) {
			for (int i = 0; i < n - lenMinusOne; i++) {
				int j = i + lenMinusOne;

				switch (scoring) {
				case SCORING_USER:
					maxCost[i][j] = computeScoreForSequence_SKEWNESS(wordList,
							patternSet, i, j, voc); // cost of the
					break;
				case SCORING_STANFORD_SENTIMENT:
					maxCost[i][j] = computeScoreForSequence_STANFORDS_SENTIMENT(
							wordList, patternSet, i, j, voc); // cost of the
					break;
				}
				// whole
				// sequence
				split[i][j] = -1; // no split
				for (int k = i; k < j; k++) {
					double cost = maxCost[i][k] + maxCost[k + 1][j];
					if (cost > maxCost[i][j]) { // use smaller sequence's cost
												// to calculate longer
												// sequences.
						maxCost[i][j] = cost;
						split[i][j] = k;
					}
				}
			}
		}

		return jumpStartGetSequence(maxCost, split);
	}

	public SequenceScore jumpStartGetSequence(double[][] maxCost,
			int[][] split) {
		int begin = 0;
		int end = maxCost[begin].length - 1;
		SequenceScore result = getSequences(maxCost, split, begin, end);
		return result;
	}

	private SequenceScore getSequences(double[][] maxCost, int[][] split,
			int begin, int end) {
		// base case: can't split anymore
		int k = split[begin][end];
		// base case: maxCost is zero, we don't care
		if (maxCost[begin][end] == 0.0d)
			return null;
		// base case: no split, just return this sequence
		if (k == -1)
			if (end + 1 - begin > NGRAM_LENGTH)
				return null; // no split but also longer than max length
			else
				return new SequenceScore(maxCost[begin][end], begin, end);
		SequenceScore firstHalf = getSequences(maxCost, split, begin, k);
		SequenceScore secondHalf = getSequences(maxCost, split, k + 1, end);
		if (firstHalf == null)
			if (secondHalf == null) {
				return new SequenceScore(maxCost[begin][end], begin, end);
			}
		if (firstHalf == null)
			if (secondHalf != null)
				return secondHalf;
		if (secondHalf == null)
			if (firstHalf != null)
				return firstHalf;
		firstHalf.addSomeNewSequences(secondHalf.mStart, secondHalf.mEnd);
		return firstHalf;
	}

	// private double computeScoreForSequence_OLDWAY(int[] wordList,
	// Set<Long> patternSet, int start, int end) {
	// double score1 = 0.0d;
	// if (end - start >= 8)
	// return score1;
	//
	// ImmutableList<Integer> sequence = getImmutableSequence(wordList, start,
	// end, Vocabulary.getInstance(), interestingPOS,
	// interestingWords);
	// int len = sequence.size();
	// Integer freq_prior = TRAINING_DATA_INSTANCE_FREQ.get(sequence);
	// Integer freq_curr = TESTING_DATA_INSTANCE_FREQ.get(sequence);
	// if (freq_curr == null && freq_prior == null)
	// return score1;
	// if (freq_prior == null)
	// freq_prior = 1; // for log
	// if (freq_curr == null)
	// freq_curr = 1;
	//
	// score1 = 1 / (1 / (double) len + 1 / (1 + Util.log(freq_prior, 2))
	// + 1 / (1 + Util.log(freq_curr, 2)));
	// return score1;
	// }

	// private static ImmutableList<Integer> getImmutableSequence(int[]
	// wordList,
	// int begin, int end, Vocabulary voc, Set<Byte> interestingPOS,
	// Set<String> interestingWords) {
	// ArrayList<Integer> list = new ArrayList<>();
	// for (int i = begin; i <= end; i++) {
	// int id = voc.addToUniversalVoc(getPresentativeToken(wordList, i,
	// interestingPOS, interestingWords));
	// list.add(id);
	// }
	// ImmutableList<Integer> sequence = ImmutableList.copyOf(list);
	// return sequence;
	// }
	private boolean isTemplated(int[] wordList, Set<Long> patternSet, int start,
			int end, Vocabulary voc) throws Exception {
		if (end - start >= 8)
			return false;
		long tagSeq1 = getTagSeq(wordList, start, end, voc);
		boolean interesting = false;
		for (int w = start; w <= end; w++) {
			if (essentialWords
					.contains(voc.getWordFromDB(wordList[w]).getText())) {
				// this sequence is interesting
				interesting = true;
				break;
			}
		}

		if (patternSet.contains(tagSeq1) && interesting == true) {
			return true;
		}
		return false;
	}

	private long getTagSeq(int[] wordList, int start, int end, Vocabulary voc)
			throws Exception {
		POSTagConverter POSconverter = POSTagConverter.getInstance();
		long tagSeq1 = 0l;
		for (int w = start; w <= end; w++) {
			String wordText = voc.getWordFromDB(wordList[w]).getText();
			int internalIndex = w - start;
			if (connNegInt.contains(wordText)) {
				byte word = POSconverter.getCode(wordText);
				tagSeq1 = POSconverter.setTagAt(tagSeq1, internalIndex, word);
			} else {
				byte pos = voc.getWordFromDB(wordList[w]).getPOS();
				tagSeq1 = POSconverter.setTagAt(tagSeq1, internalIndex, pos);
			}
		}
		return tagSeq1;
	}

	// length * log(skewness).
	private double computeScoreForSequence_SKEWNESS(int[] wordList,
			Set<Long> patternSet, int start, int end, Vocabulary voc)
			throws Throwable {
		if (wordScore == null)
			readWordsSkewness(KeywordAnalyzer.WEIBULL_FREQUENCY,
					SCORING_FILENAME);
		double score1 = 0.0d;
		if (end - start >= 8)
			return score1;
		if (!isTemplated(wordList, patternSet, start, end, voc))
			return score1;
		// StanfordNLPProcessing nlp = StanfordNLPProcessing.getInstance();
		// ImmutableList<Integer> sequence = getImmutableSequence(wordList,
		// POSList, start, end, Vocabulary.getInstance(), interestingPOS,
		// interestingWords);

		int len = end - start;
		StringBuilder phrase = new StringBuilder();
		phrase.append(voc.getWordFromDB(wordList[start]).getText());
		Double strongestNegativeSkew = wordScore
				.get(voc.getWordFromDB(wordList[start]).getText());
		if (strongestNegativeSkew == null)
			strongestNegativeSkew = 0.0;
		for (int i = start + 1; i <= end; i++) {
			String w = voc.getWordFromDB(wordList[i]).getText();
			phrase.append(" " + w);
			Double skew = wordScore.get(w);
			if (skew == null)
				skew = 0.0;
			if (strongestNegativeSkew > skew)
				strongestNegativeSkew = skew;
		}
		// String strPhrase = phrase.toString();
		// int sentimentScore = nlp.findMainSentiment_sentence(strPhrase,true);
		// len: 1-8
		// sentimentScore: 0-2
		// strongestSkew from 1 to aroud 70
		// score1 = len*(sentimentScore+1)*Util.log(strongestNegativeSkew, 2);
		score1 = len * strongestNegativeSkew;
		// the bigger the number is, the stronger in sentiment, skew and length
		return score1;
	}

	// length * (sentimentScore+1);
	private double computeScoreForSequence_STANFORDS_SENTIMENT(int[] wordList,
			Set<Long> patternSet, int start, int end, Vocabulary voc)
			throws Throwable {
		double score1 = 0.0d;
		if (end - start >= 8)
			return score1;
		if (!isTemplated(wordList, patternSet, start, end, voc))
			return score1;
		StanfordNLPProcessing nlp = StanfordNLPProcessing.getInstance();
		// ImmutableList<Integer> sequence = getImmutableSequence(wordList,
		// POSList, start, end, Vocabulary.getInstance(), interestingPOS,
		// interestingWords);

		int len = end - start;
		StringBuilder phrase = new StringBuilder();
		phrase.append(voc.getWordFromDB(wordList[start]).getText());
		for (int i = start + 1; i <= end; i++) {
			String w = voc.getWordFromDB(wordList[i]).getText();
			phrase.append(" " + w);
		}
		String strPhrase = phrase.toString();
		int sentimentScore = nlp.findMainSentiment_sentence(strPhrase, true);
		// len: 1-8
		// sentimentScore: 0-2
		score1 = len * (sentimentScore + 1);
		// the bigger the number is, the stronger in sentiment, skew and length
		return score1;
	}

	public Set<Item> extractSequence_ByPattern(int[][] wordIDList,
			Set<Long> patternSet, boolean actualString, Vocabulary voc)
			throws Throwable {
		// text = text.replace("ca n't", "can not").replace("can't", "can not");
		POSTagConverter postagConv = POSTagConverter.getInstance();
		Set<Item> sequenceSet = new HashSet<>();
		for (int s = 0; s < wordIDList.length; s++) {
			int[] sentence = wordIDList[s];
			int lastAcceptedIndex = -1;
			for (int i = 0; i < sentence.length; i++) {
				int maxIndexToGo = NGRAM_LENGTH;
				if ((sentence.length - i) < NGRAM_LENGTH)
					maxIndexToGo = sentence.length - i;
				long[] nGramPOS = new long[maxIndexToGo];
				boolean[] interesting = new boolean[maxIndexToGo];
				for (int index = 0; index < maxIndexToGo; index++) {
					interesting[index] = false;
				}
				for (int w = i; w < (i + maxIndexToGo); w++) {
					int internalIndex = w - i;
					String wordW = voc.getWordFromDB(sentence[w]).getText();
					if (connNegInt.contains(wordW)) {
						byte pos = voc.getWordFromDB(sentence[i]).getPOS();
						if (internalIndex == 0)
							nGramPOS[internalIndex] = postagConv.setTagAt(
									nGramPOS[internalIndex], internalIndex,
									pos);
						else
							nGramPOS[internalIndex] = postagConv.setTagAt(
									nGramPOS[internalIndex - 1], internalIndex,
									pos);
					} else {
						if (internalIndex == 0)
							nGramPOS[internalIndex] = postagConv.setTagAt(
									nGramPOS[internalIndex], internalIndex,
									voc.getWordFromDB(sentence[w]).getPOS());
						else
							nGramPOS[internalIndex] = postagConv.setTagAt(
									nGramPOS[internalIndex - 1], internalIndex,
									voc.getWordFromDB(sentence[w]).getPOS());
					}

					if (essentialWords.contains(wordW))
						// all sequences contain this word must be interesting
						for (int j = internalIndex; j < maxIndexToGo; j++)
						interesting[j] = true;
				}
				int choosenLen = -1;
				int backwardLimit = lastAcceptedIndex - i;
				if (backwardLimit < 0)
					backwardLimit = -1;
				for (int index = maxIndexToGo
						- 1; index > backwardLimit; index--) {
					if (patternSet.contains(nGramPOS[index])
							&& interesting[index] == true) {
						choosenLen = index + 1;
						break;
					}
				}
				if (choosenLen == -1)
					continue;
				lastAcceptedIndex = i + choosenLen - 1;
				StringBuilder choosenSequence = new StringBuilder();
				StringBuilder choosenActualStr = new StringBuilder();
				for (int w = i; w < (i + choosenLen); w++) {
					String wordW = voc.getWordFromDB(sentence[w]).getText();
					String representativeToken = postagConv
							.getTag(voc.getWordFromDB(sentence[w]).getPOS());
					if (interestingPOS
							.contains(voc.getWordFromDB(sentence[w]).getPOS())
							|| interestingWords.contains(wordW))
						representativeToken = wordW;
					choosenSequence.append(representativeToken).append("_");
					choosenActualStr.append(wordW).append(" ");
				}
				String seq = choosenSequence
						.deleteCharAt(choosenSequence.length() - 1).toString();
				String str = choosenActualStr
						.deleteCharAt(choosenActualStr.length() - 1).toString();
				if (DEBUG) {
					System.out.println(seq);
					System.out.println(str);
				}
				Item it = new Item(seq, 0, 0, null);
				if (actualString)
					it.actualPhrase.add(str + "," + s);
				sequenceSet.add(it);
			}
		}
		return sequenceSet;
	}

	// start and end are the exact indexes
	private static class SequenceScore {
		double score;
		List<Integer> mStart = new ArrayList<Integer>();
		List<Integer> mEnd = new ArrayList<Integer>();

		public SequenceScore(double score, int start, int end) {
			// TODO Auto-generated constructor stub
			this.score = score;
			mStart.add(start);
			mEnd.add(end);
		}

		public SequenceScore(double score, List<Integer> start,
				List<Integer> end) {
			// TODO Auto-generated constructor stub
			this.score = score;
			mStart.addAll(start);
			mEnd.addAll(end);
		}

		public void addAnotherSequence(int start, int end) {
			mStart.add(start);
			mEnd.add(end);
		}

		public void addSomeNewSequences(List<Integer> start,
				List<Integer> end) {
			mStart.addAll(start);
			mEnd.addAll(end);
		}

	}

	public static class Item extends Clusterable {
		double[] vector = null;
		private String gram;
		private double badScore;
		private double goodScore;
		private Set<String> actualPhrase;
		boolean change = false;
		private List<String> expandedPhrases = null;

		public void setExpandedPhrases(List<int[]> inputPhraseRanges,
				String[] wordList) {
			// TODO Auto-generated method stub
			// expandedPhrases = mergeExpandedPhrase(inputPhraseRanges,
			// wordList);
		}

		@Override
		public double[] getVector() {
			// TODO Auto-generated method stub
			return vector;
		}

		@Override
		public int getFrequency() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void setChange(boolean isChanged) {
			// TODO Auto-generated method stub
			change = isChanged;
		}

		@Override
		public boolean isChanged() {
			// TODO Auto-generated method stub
			return change;
		}

		public String toString() {
			return gram;
		}

		public Item(String grm, double bad, double good, WordVec word2vec)
				throws Throwable {
			// TODO Auto-generated constructor stub
			gram = grm;
			badScore = bad;
			goodScore = good;
			actualPhrase = new HashSet<>();
			if (word2vec != null) {
				POSTagConverter POSconverter = POSTagConverter.getInstance();
				String[] wordList = gram.split("_");
				int validCount = 0;
				for (String word : wordList) {
					// if (!analyst.topicWords.contains(word))
					// continue;

					if ((byte) 0xFF != POSconverter.getCode(word))
						continue;

					float[] tempVector = word2vec.getVectorForWord(word);
					if (tempVector != null) {
						validCount++;
						if (vector == null)
							vector = new double[WordVec.VECTOR_SIZE];
						for (int i = 0; i < WordVec.VECTOR_SIZE; i++) {
							vector[i] += tempVector[i];
						}
					}
				}
				if (vector != null)
					for (int i = 0; i < WordVec.VECTOR_SIZE; i++) {
						vector[i] /= validCount;
					}
			}

		}

	}
}
