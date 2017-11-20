package Analyzers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import Datastores.Dataset;
import Datastores.Document;
import NLP.NatureLanguageProcessor;
import Utils.Util;
import Vocabulary.Vocabulary;

public class TrendAnalyzer {
	public static final boolean CONTAIN_TOPIC = false;
	public static final boolean MAJOR_TOPIC = true;

	/*
	 * Only work if data.hasTime() == true, else return null
	 * */
	public static Map<Integer, Integer> getPhraseTrend_Frequency(Dataset data,
			Set<String> selection, boolean isCountingMajorTopicDocument)
			throws UnsupportedEncodingException, SQLException, IOException {
		if (data.hasTime() == false)
			return null;
		Set<String> unigrams = new HashSet<>();
		Set<String> bigrams = new HashSet<>();
		Set<String> trigrams = new HashSet<>();
		Set<String> topicSequence = new HashSet<>();

		extractBagOfGrams(data, selection, unigrams, bigrams, trigrams,
				topicSequence);
		// debug--------------------
		PrintWriter pw = new PrintWriter(
				new File("D:/projects/ALPACA/NSF/testGrams.csv"));
		for (String p : unigrams)
			pw.println(p);
		for (String p : bigrams)
			pw.println(p);
		for (String p : trigrams)
			pw.println(p);
		pw.close();
		// --------------------------

		Map<Integer, Integer> trendOverYear = gatherFrequencyOverYear(data,
				unigrams, bigrams, trigrams, data.getVocabulary(),
				isCountingMajorTopicDocument, topicSequence);
		return trendOverYear;
	}

	private static Set<Integer> extractBagOfWords(Dataset data,
			Set<String> selection) {
		Set<String> stopWords = NatureLanguageProcessor.getInstance()
				.getStopWordSet();
		Set<Integer> bagOfWords = new HashSet<>();
		Vocabulary voc = data.getVocabulary();
		for (String word : selection) {
			if (word.contains(" ")) {
				String[] phrase = word.split(" ");
				for (String w : phrase) {
					if (stopWords.contains(w))
						continue;
					List<Integer> wordIDs = voc.getWordIDs(w);
					if (wordIDs != null) {
						bagOfWords.addAll(wordIDs);
					}
				}
			} else {
				List<Integer> wordIDs = voc.getWordIDs(word);
				if (wordIDs != null)
					bagOfWords.addAll(wordIDs);
			}
		}
		return bagOfWords;
	}

	// 1 gram, 2 grams, 3 grams
	private static void extractBagOfGrams(Dataset data, Set<String> selection,
			Set<String> unigrams, Set<String> bigrams, Set<String> trigrams,
			Set<String> topicSequence) {
		Set<String> stopWords = NatureLanguageProcessor.getInstance()
				.getStopWordSet();
		Vocabulary voc = data.getVocabulary();
		for (String word : selection) {
			// if (word.contains(" ")) {
			// String[] phrase = word.split(" ");
			// for (int i = 0; i < phrase.length; i++) {
			// // unigram, bigram and trigram can't end with a stopword
			// if (stopWords.contains(phrase[i]))
			// continue;
			// if (i > 0) { // it is possible to get bigram
			// if (!stopWords.contains(phrase[i - 1])) {
			// // bigram can't start with a stopword
			// bigrams.add(phrase[i - 1] + " " + phrase[i]);
			// }
			// if (i > 1) { // it is possible to get trigram
			// if (!stopWords.contains(phrase[i - 2]))
			// // trigram can't start with a stopword
			// trigrams.add(phrase[i - 2] + " " + phrase[i - 1]
			// + " " + phrase[i]);
			// }
			// }
			// }
			// } else {
			// // unigram is only for original topic keywords
			// unigrams.add(word);
			// }
			//
			String[] phrase = word.split(" ");
			if (phrase.length == 1)
				unigrams.add(word);
			else {
				topicSequence.add(word);
			}
			if (phrase.length == 2)
				bigrams.add(word);
			if (phrase.length == 3)
				trigrams.add(word);
			for (int i = 0; i < phrase.length; i++) {
				// unigram, bigram and trigram can't end with a stopword
				if (stopWords.contains(phrase[i]))
					continue;
				if (i > 0) { // it is possible to get bigram
					if (!stopWords.contains(phrase[i - 1])) {
						if (unigrams.contains(phrase[i])
								|| unigrams.contains(phrase[i - 1]))
							// bigram can't start with a stopword
							bigrams.add(phrase[i - 1] + " " + phrase[i]);
					}
					if (i > 1) { // it is possible to get trigram
						if (!stopWords.contains(phrase[i - 2]))
							if (unigrams.contains(phrase[i])
									|| unigrams.contains(phrase[i - 1])
									|| unigrams.contains(phrase[i - 2]))
								// trigram can't start with a stopword
								trigrams.add(phrase[i - 2] + " " + phrase[i - 1]
										+ " " + phrase[i]);
					}
				}
			}
		}
	}

	/*
	 * Only work if data.hasTime() == true, else return null
	 * */
	public static Map<Integer, Float> getPhraseTrend_Percentage(Dataset data,
			Set<String> selection, boolean isCountingMajorTopicDocument)
			throws UnsupportedEncodingException, SQLException, IOException {
		if (data.hasTime() == false)
			return null;
		Set<String> unigrams = new HashSet<>();
		Set<String> bigrams = new HashSet<>();
		Set<String> trigrams = new HashSet<>();
		Set<String> topicSequence = new HashSet<>();

		extractBagOfGrams(data, selection, unigrams, bigrams, trigrams,
				topicSequence);
		Map<Integer, Integer> trendOverYear = gatherFrequencyOverYear(data,
				unigrams, bigrams, trigrams, data.getVocabulary(),
				isCountingMajorTopicDocument, topicSequence);
		Map<Integer, Integer> totalDocOverYear = new HashMap<>();

		double percentageCompleted = 0, docCompleted = 0;
		int totalDoc = data.getDocumentSet().size();
		Util.printProgress(percentageCompleted);
		for (Document doc : data.getDocumentSet()) {
			Date date = new Date(doc.getTime());
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			int year = cal.get(Calendar.YEAR);
			Integer count = totalDocOverYear.get(year);
			if (count == null)
				totalDocOverYear.put(year, 1);
			else
				totalDocOverYear.put(year, count + 1);

			docCompleted++;

			double newPercentage = Util.round(100 * docCompleted / totalDoc, 2);
			if (newPercentage > percentageCompleted) {
				percentageCompleted = newPercentage;
				Util.printProgress(percentageCompleted);
			}

		}
		Map<Integer, Float> percentageTrends = new HashMap<>();
		for (Entry<Integer, Integer> entry : trendOverYear.entrySet()) {
			percentageTrends.put(entry.getKey(), (float) entry.getValue()
					/ totalDocOverYear.get(entry.getKey()));
		}
		return percentageTrends;
	}

	/*
	 * Only work if there is an accumulatble rating, such as money
	 * */
	public static Map<Integer, Integer> getPhraseTrend_valueAccumulation(
			Dataset data, Set<String> selection,
			boolean isCountingMajorTopicDocument)
			throws UnsupportedEncodingException, SQLException, IOException {
		if (data.hasTime() == false)
			return null;
		Set<String> unigrams = new HashSet<>();
		Set<String> bigrams = new HashSet<>();
		Set<String> trigrams = new HashSet<>();
		Set<String> topicSequence = new HashSet<>();

		extractBagOfGrams(data, selection, unigrams, bigrams, trigrams,
				topicSequence);
		Map<Integer, Integer> trendOverYear = gatherValueAcuccumulationOverYear(
				data, unigrams, bigrams, trigrams, data.getVocabulary(),
				isCountingMajorTopicDocument, topicSequence);

		return trendOverYear;
	}

	public static Map<Integer, Integer> gatherFrequencyOverYearAll(
			Dataset data)
			throws UnsupportedEncodingException, SQLException, IOException {
		Map<Integer, Integer> trendOverYear = new HashMap<>();
		System.out.println("Counting total documents over the year for the whole dataset..");
		double percentageCompleted = 0, docCompleted = 0;
		int totalDoc = data.getDocumentSet().size();
		Util.printProgress(percentageCompleted);
		for (Document doc : data.getDocumentSet()) {
			int[][] sentences = doc.getSentences();
			Date date = new Date(doc.getTime());
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			int year = cal.get(Calendar.YEAR);
			Integer count = trendOverYear.get(year);
			if (count == null)
				trendOverYear.put(year, 1);
			else
				trendOverYear.put(year, count + 1);

			docCompleted++;
			double newPercentage = Util.round(100 * docCompleted / totalDoc, 2);
			if (newPercentage > percentageCompleted) {
				percentageCompleted = newPercentage;
				Util.printProgress(percentageCompleted);

			}
		}

		System.out.println();
		return trendOverYear;
	}

	private static Map<Integer, Integer> gatherFrequencyOverYear(Dataset data,
			Set<String> unigrams, Set<String> bigrams, Set<String> trigrams,
			Vocabulary voc, boolean isCountingMajorTopicDocument,
			Set<String> topicSequence)
			throws UnsupportedEncodingException, SQLException, IOException {
		Map<Integer, Integer> trendOverYear = new HashMap<>();

		double percentageCompleted = 0, docCompleted = 0;
		int totalDoc = data.getDocumentSet().size();
		Util.printProgress(percentageCompleted);
		for (Document doc : data.getDocumentSet()) {
			int[][] sentences = doc.getSentences();
			boolean countable = false;
			if (isCountingMajorTopicDocument)
				countable = containsTopicAsMajorTopic(unigrams, bigrams,
						trigrams, sentences, 0.5, 0.4, 0.3, voc);
			else {
				// countable = containsTopic(unigrams, bigrams, trigrams,
				// sentences, voc);
				countable = containsTopic(topicSequence, sentences, voc);
			}
			docCompleted++;
			if (countable) {
				Date date = new Date(doc.getTime());
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				int year = cal.get(Calendar.YEAR);
				Integer count = trendOverYear.get(year);
				if (count == null)
					trendOverYear.put(year, 1);
				else
					trendOverYear.put(year, count + 1);

				double newPercentage = Util.round(100 * docCompleted / totalDoc,
						2);
				if (newPercentage > percentageCompleted) {
					percentageCompleted = newPercentage;
					Util.printProgress(percentageCompleted);
				}
			}
		}

		System.out.println();
		return trendOverYear;
	}

	private static Map<Integer, Integer> gatherValueAcuccumulationOverYear(
			Dataset data, Set<String> unigrams, Set<String> bigrams,
			Set<String> trigrams, Vocabulary voc,
			boolean isCountingMajorTopicDocument, Set<String> topicSequence)
			throws UnsupportedEncodingException, SQLException, IOException {
		Map<Integer, Integer> trendOverYear = new HashMap<>();

		double percentageCompleted = 0, docCompleted = 0;
		int totalDoc = data.getDocumentSet().size();
		Util.printProgress(percentageCompleted);
		for (Document doc : data.getDocumentSet()) {
			int[][] sentences = doc.getSentences();
			boolean countable = false;
			if (isCountingMajorTopicDocument)
				countable = containsTopicAsMajorTopic(unigrams, bigrams,
						trigrams, sentences, 0.5, 0.4, 0.3, voc);
			else {
				// countable = containsTopic(unigrams, bigrams, trigrams,
				// sentences, voc);
				countable = containsTopic(topicSequence, sentences, voc);
			}
			docCompleted++;
			if (countable) {
				Date date = new Date(doc.getTime());
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				int year = cal.get(Calendar.YEAR);
				Integer count = trendOverYear.get(year);
				if (doc.getRating() != -1)
					if (count == null)
						trendOverYear.put(year, doc.getRating());
					else
						trendOverYear.put(year, count + doc.getRating());

				double newPercentage = Util.round(100 * docCompleted / totalDoc,
						2);
				if (newPercentage > percentageCompleted) {
					percentageCompleted = newPercentage;
					Util.printProgress(percentageCompleted);
				}
			}
		}
		System.out.println();
		return trendOverYear;
	}

	// naive containing
	public static boolean containsTopic(Set<String> topicSequences,
			int[][] sentences, Vocabulary voc)
			throws UnsupportedEncodingException, SQLException, IOException {
		if (sentences == null)
			return false;
		for (int[] sen : sentences) {
			StringBuilder strBuilder = new StringBuilder();
			for (int wordID : sen) {
				String word = voc.getWordFromDB(wordID).getText();
				strBuilder.append(word).append(" ");
			}
			String sentence = strBuilder.toString();
			for (String sequence : topicSequences) {
				if (sentence.contains(sequence)) {
					return true;
				}
			}

			// // check if the vocabulary of this sentence contains a keyword
			// if (unigramScore >= 1) {
			// // if the ratio of bigram over possible bigrams is good enough
			// // then this is a topic sentence.
			// if (bigramScore / (sen.length - 1) >= biproportionThreshold)
			// return true;
			// // same for trigram
			// if (trigramScore / (sen.length - 2) >= tripropotionThreshold)
			// return true;
			// }
		}
		return false;

	}

	// as long as the document mention this topic somewhere, it returns true
	public static boolean containsTopic(Set<String> topic1Grams,
			Set<String> topic2Grams, Set<String> topic3Grams, int[][] sentences,
			Vocabulary voc)
			throws UnsupportedEncodingException, SQLException, IOException {
		if (sentences == null)
			return false;
		for (int[] sen : sentences) {
			StringBuilder strBuilder = new StringBuilder();
			for (int wordID : sen) {
				String word = voc.getWordFromDB(wordID).getText();
				strBuilder.append(word).append(" ");
				if (topic1Grams.contains(word))
					return true;
			}
			String sentence = strBuilder.toString();
			for (String gram : topic2Grams) {
				if (sentence.contains(gram)) {
					return true;
				}
			}
			for (String gram : topic3Grams) {
				if (sentence.contains(gram)) {
					return true;
				}
			}

			// // check if the vocabulary of this sentence contains a keyword
			// if (unigramScore >= 1) {
			// // if the ratio of bigram over possible bigrams is good enough
			// // then this is a topic sentence.
			// if (bigramScore / (sen.length - 1) >= biproportionThreshold)
			// return true;
			// // same for trigram
			// if (trigramScore / (sen.length - 2) >= tripropotionThreshold)
			// return true;
			// }
		}
		return false;

	}

	// the document has to heavily mention a topic to belong to a topic
	public static boolean containsTopicAsMajorTopic(Set<String> topic1Grams,
			Set<String> topic2Grams, Set<String> topic3Grams, int[][] sentences,
			double uniproportionThreshold, double biproportionThreshold,
			double tripropotionThreshold, Vocabulary voc)
			throws UnsupportedEncodingException, SQLException, IOException {
		if (sentences == null)
			return false;
		double totalWord = 0;
		int totalUni = 0, totalBi = 0, totalTri = 0;
		for (int[] sen : sentences) {
			StringBuilder strBuilder = new StringBuilder();
			for (int wordID : sen) {
				String word = voc.getWordFromDB(wordID).getText();
				strBuilder.append(word).append(" ");
				if (topic1Grams.contains(word))
					totalUni++;
			}
			String sentence = strBuilder.toString();
			for (String gram : topic2Grams) {
				if (sentence.contains(gram)) {
					totalBi++;
				}
			}
			for (String gram : topic3Grams) {
				if (sentence.contains(gram)) {
					totalTri++;
				}
			}
			totalWord += sen.length;
			// // check if the vocabulary of this sentence contains a keyword
			// if (unigramScore >= 1) {
			// // if the ratio of bigram over possible bigrams is good enough
			// // then this is a topic sentence.
			// if (bigramScore / (sen.length - 1) >= biproportionThreshold)
			// return true;
			// // same for trigram
			// if (trigramScore / (sen.length - 2) >= tripropotionThreshold)
			// return true;
			// }
		}
		if (totalUni / totalWord >= uniproportionThreshold)
			return true;
		if (totalBi / (totalWord - 1) >= biproportionThreshold)
			return true;
		if (totalTri / (totalWord - 2) >= tripropotionThreshold)
			return true;
		return false;

	}
}
