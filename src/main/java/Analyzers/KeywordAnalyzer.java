package Analyzers;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.distribution.WeibullDistribution;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import Datastores.Dataset;
import Datastores.Document;
import Datastores.DocumentDatasetDB;
import NLP.NatureLanguageProcessor;
import Utils.Util;
import Vocabulary.Vocabulary;

public class KeywordAnalyzer {
	public static void main(String[] args) throws Exception {
		KeywordAnalyzer analyzer = new KeywordAnalyzer();
		analyzer.calculateAndWriteKeywordScore(1,
				DocumentDatasetDB.LV2_ROOTWORD_STEMMING,
				"D:\\projects\\ALPACA\\NSF\\wordScore\\scoreLV2.csv", 1000);
	}

	public static final int TFIDF = 3; // tf * log(1/N+1)
	public static final int MEAN = 2; // average score of a term based on doc
										// rating/score
	public static final int FREQUENCY = 1;
	public static final int WEIBULL_FREQUENCY = 4;
	public static final int CONTRAST_SCORE = 5; // refer to our publication with
												// MARK, this is just for app
												// reviews

	public void calculateAndWriteKeywordScore(int datasetID, int level,
			String outputFilename, int numberOfPieces) throws Exception {
		Map<Integer, Integer> termCount = new HashMap<>(); // total time a term
															// appeared
		Map<Integer, Integer> documentCount = new HashMap<>(); // count the
																// number of doc
																// a term
																// appeared in
		Map<Integer, Integer> termTotalScore = new HashMap<>(); // total score
																// of the docs
																// contain this
																// term
		Map<Integer, Double> frequencyPercentile = new HashMap<>();
		Map<Integer, int[]> data4ContrastScore = null;
		if (level == CONTRAST_SCORE)
			data4ContrastScore = new HashMap<>();
		// List<Application> appList = appMan.getAppList();
		DocumentDatasetDB db = DocumentDatasetDB.getInstance();
		Dataset dataset = db.queryDatasetInfo(datasetID);
		db.queryRawTextForSingleDataset(dataset);
		// counting statistic
		for (Document doc : dataset.getDocumentSet()) {
			if (!doc.isEnglish())
				continue;
			// int docID = doc.getDocumentID();
			doc.populatePreprocessedDataFromDB(level);
			int[][] sentences = doc.getSentences();
			int rating = doc.getRating();
			countWord(termTotalScore, rating, termCount, documentCount,
					sentences, data4ContrastScore);
		}
		// computing and printing to file
		Set<String> stopwords = NatureLanguageProcessor.getInstance()
				.getStopWordSet();
		countFrequencyPercentile(frequencyPercentile, termCount,
				numberOfPieces);
		PrintWriter outputFile = new PrintWriter(new File(outputFilename));
		int totalNumberOfDoc = dataset.getDocumentSet().size();
		Vocabulary voc = Vocabulary.getInstance();
		outputFile.println(
				"\"term\",\"frequency\",\"mean score\",\"idf\",\"weibull(freq)\",\"contrast\"");
		for (Entry<Integer, Integer> totalScoreEntry : termTotalScore
				.entrySet()) {
			int term = totalScoreEntry.getKey();
			String word = voc.getText(term);
			if (Util.isNumeric(word))
				continue;
			if (stopwords.contains(word))
				continue;
			Integer freq = termCount.get(term);
			Integer docFreq = documentCount.get(term);
			Integer totalScore = totalScoreEntry.getValue();
			double idf = Util.log(totalNumberOfDoc, 10) - Util.log(docFreq, 10);
			if (level == CONTRAST_SCORE) {
				int[] ratingCounts = data4ContrastScore.get(term);
				outputFile.println("\"" + word + "\"," + "\"" + freq + "\","
						+ "\"" + (double) totalScore / docFreq + "\"," + "\""
						+ idf + "\"," + "\"" + frequencyPercentile.get(term)
						+ "\","
						+ contrast(ratingCounts[0], ratingCounts[1],
								ratingCounts[2], ratingCounts[3],
								ratingCounts[4], true)
						+ "\"");
			} else {
				outputFile.println("\"" + word + "\"," + "\"" + freq + "\","
						+ "\"" + (double) totalScore / docFreq + "\"," + "\""
						+ idf + "\"," + "\"" + frequencyPercentile.get(term)
						+ "\"," + "null" + "\"");
			}
		}
		outputFile.close();
	}

	private static double[] contrast(int x1, int x2, int x3, int x4, int x5,
			boolean negative) {
		double bad = x1 + x2 + 1;
		double good = x4 + x5 + 1;
		if (negative) {
			double score = bad * (bad - good) / good;
			double ratio = bad / good;
			return new double[] { ratio, score };
		} else {
			double score = good * (good - bad) / bad;
			double ratio = good / bad;
			return new double[] { ratio, score };

		}

	}

	private static void countFrequencyPercentile(
			Map<Integer, Double> frequencyPercentile,
			Map<Integer, Integer> termCount, int numberOfPieces) {
		List<Integer> sortedFreq = new ArrayList<>(termCount.values());
		Collections.sort(sortedFreq); // ascending order
		int range = sortedFreq.size() / numberOfPieces;
		List<Integer> rangeList = new ArrayList<>();
		for (int i = 1; i < numberOfPieces; i++) {
			rangeList.add(sortedFreq.get(i * range));
		}
		double smallestPercentile = 1f / numberOfPieces;
		for (Entry<Integer, Integer> entry : termCount.entrySet()) {
			int freq = entry.getValue();
			int wordID = entry.getKey();
			for (int i = 0; i < rangeList.size(); i++) {
				int upperVal = rangeList.get(i);
				if (freq < upperVal) {
					frequencyPercentile.put(wordID, smallestPercentile * i);
					break;
				}
			}
			if (frequencyPercentile.get(wordID) == null)
				frequencyPercentile.put(wordID, 1.0);
		}
		WeibullDistribution distribution = new WeibullDistribution(1.5, 0.78);

		for (Entry<Integer, Double> entry : frequencyPercentile.entrySet()) {
			double percentile = entry.getValue();
			entry.setValue(distribution.density(percentile));
		}
	}

	private static void countWord(Map<Integer, Integer> termMeanScore,
			int score, Map<Integer, Integer> termCount,
			Map<Integer, Integer> documentCount, int[][] sentences,
			Map<Integer, int[]> fiveRatingWordCount) throws Exception {
		Set<Integer> terms = new HashSet<>();
		for (int s = 0; s < sentences.length; s++) {
			for (int w = 0; w < sentences[s].length; w++) {
				Integer count = termCount.get(sentences[s][w]);
				if (count == null) {
					termCount.put(sentences[s][w], 1);
				} else {
					termCount.put(sentences[s][w], count + 1);
				}
				terms.add(sentences[s][w]);
				// only for 5 rating scheme
				if (fiveRatingWordCount != null) {
					if (score >= 5 || score < 0)
						throw new Exception(
								"not a 5 ratings scheme, value = " + score);
					int[] ratingCount = fiveRatingWordCount
							.get(sentences[s][w]);
					if (ratingCount == null) {
						ratingCount = new int[5];
						ratingCount[score] = 1;
					} else {
						ratingCount[score] += 1;
					}
					fiveRatingWordCount.put(sentences[s][w], ratingCount);
				}
			}
		}

		for (int term : terms) {
			Integer count = documentCount.get(term);
			if (count == null) {
				documentCount.put(term, 1);
			} else {
				documentCount.put(term, count + 1);
			}
			Integer scoreTotal = termMeanScore.get(term);
			if (scoreTotal == null) {
				termMeanScore.put(term, score);
			} else {
				termMeanScore.put(term, scoreTotal + score);
			}
		}
	}
}
