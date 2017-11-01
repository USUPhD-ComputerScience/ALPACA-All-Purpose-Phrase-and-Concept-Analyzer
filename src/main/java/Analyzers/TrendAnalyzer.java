package Analyzers;

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
import Vocabulary.Vocabulary;

public class TrendAnalyzer {
	/*
	 * Only work if data.hasTime() == true, else return null
	 * */
	public static Map<Integer, Integer> getPhraseTrend_Frequency(Dataset data,
			Set<String> selection) {
		if (data.hasTime() == false)
			return null;
		Set<Integer> bagOfWords = extractBagOfWords(data, selection);
		Map<Integer, Integer> trendOverYear = gatherFrequencyOverYear(data,
				bagOfWords);
		return trendOverYear;
	}

	private static Set<Integer> extractBagOfWords(Dataset data,
			Set<String> selection) {
		Set<Integer> bagOfWords = new HashSet<>();
		Vocabulary voc = data.getVocabulary();
		for (String word : selection) {
			if (word.contains(" ")) {
				String[] phrase = word.split(" ");
				for (String w : phrase) {
					List<Integer> wordIDs = voc.getWordIDs(w);
					if (wordIDs != null)
						bagOfWords.addAll(wordIDs);
				}
			} else {
				List<Integer> wordIDs = voc.getWordIDs(word);
				if (wordIDs != null)
					bagOfWords.addAll(wordIDs);
			}
		}
		return bagOfWords;
	}

	/*
	 * Only work if data.hasTime() == true, else return null
	 * */
	public static Map<Integer, Float> getPhraseTrend_Percentage(Dataset data,
			Set<String> selection) {
		if (data.hasTime() == false)
			return null;
		Set<Integer> bagOfWords = extractBagOfWords(data, selection);
		Map<Integer, Integer> trendOverYear = gatherFrequencyOverYear(data,
				bagOfWords);
		Map<Integer, Integer> totalDocOverYear = new HashMap<>();
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

		}
		Map<Integer, Float> percentageTrends = new HashMap<>();
		for (Entry<Integer, Integer> entry : trendOverYear.entrySet()) {
			int totalDoc = totalDocOverYear.get(entry.getKey());
			percentageTrends.put(entry.getKey(),
					(float) entry.getValue() / totalDoc);
		}
		return percentageTrends;
	}

	private static Map<Integer, Integer> gatherFrequencyOverYear(Dataset data,
			Set<Integer> bagOfWords) {
		Map<Integer, Integer> trendOverYear = new HashMap<>();
		for (Document doc : data.getDocumentSet()) {
			int[][] sentences = doc.getSentences();
			if (containsTopic(bagOfWords, sentences, 0.4, 0.5)) {
				Date date = new Date(doc.getTime());
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				int year = cal.get(Calendar.YEAR);
				Integer count = trendOverYear.get(year);
				if (count == null)
					trendOverYear.put(year, 1);
				else
					trendOverYear.put(year, count + 1);
			}
		}
		return trendOverYear;
	}

	public static boolean containsTopic(Set<Integer> topicWord,
			int[][] sentences, double biproportionThreshold,
			double uniproportionThreshold) {
		double score = 1.0;
		boolean previousInDic = false;
		double totalScore = 0, bigramScore = 0, unigramScore = 0;
		if (sentences == null)
			return false;
		for (int[] sen : sentences) {
			for (int wordID : sen) {
				if (topicWord.contains(wordID)) {
					// score /= Math.log(wCount);
					unigramScore += score;
					if (previousInDic)
						bigramScore += score;
					previousInDic = true;
				} else
					previousInDic = false;
				totalScore += score;
			}
		}

		if (totalScore == 0)
			return false;
		double biproportion = bigramScore / totalScore;
		double uniproportion = unigramScore / totalScore;
		if (biproportion < biproportionThreshold
				&& uniproportion < uniproportionThreshold)
			return false;
		return true;

	}
}
