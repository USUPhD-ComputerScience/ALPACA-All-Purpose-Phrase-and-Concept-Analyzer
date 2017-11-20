package Analyzers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import Datastores.Dataset;
import Datastores.Document;
import NLP.NatureLanguageProcessor;
import NLP.SymSpell;
import NLP.WordVec;
import Utils.Util;
import Vocabulary.DBWord;
import Vocabulary.Vocabulary;

import java.util.Map.Entry;

import org.nd4j.shade.jackson.dataformat.yaml.snakeyaml.events.Event.ID;

public class KeywordExplorer {

	public static WordVec word2vec;
	private static double SIM_THRESHOLD = 0.7;

	// static Map<String, Double> pairSimiliarity = new HashMap<>();
	public static Set<String> expand(List<String> wordList, WordVec word2vec,
			Dataset dataset, Map<String, Double> wordScore, Double optionalThreshold, Map<String, Double> IDFWeights) throws Throwable {
		if(optionalThreshold != null)
			SIM_THRESHOLD = optionalThreshold;
		else
			SIM_THRESHOLD = 0.7; // default
		KeywordExplorer.word2vec = word2vec;
		System.out.println("Expanding words with similarity threshold of " + SIM_THRESHOLD);
		System.out.println("Initialize seeds of this topic");
		Set<VectorableToken> selection = addSingleWords(wordList, word2vec,
				dataset, IDFWeights);
		Set<String> seeds = new HashSet<>();
		System.out.println("The initialized seeds are: ");
		for (VectorableToken token : selection) {
			seeds.add(token.tokenText);
			System.out.print(token.tokenText+ ", ");
		}
		System.out.println();
		System.out.println("Begining topic expansion process");
		Set<String> results = addPhrases(wordList, word2vec, dataset, wordScore,
				selection, seeds,IDFWeights);
		System.out.println("The result of this expansion is:");
		for (String res : results) {
			System.out.println(res);
		}


		// scanner.close();
		System.out.println("Done!");
		return results;
	}

	private static Set<VectorableToken> addSingleWords(List<String> wordList,
			WordVec word2vec, Dataset dataset,Map<String,Double> IDFWeights) throws Throwable {
		Set<VectorableToken> selection = new HashSet<>();
		for (String word : wordList) {
			if (word.contains("_")) {
				String[] phrase = word.split("_");
				StringBuilder strBuilder = new StringBuilder();
				for (String w : phrase) {
					strBuilder.append(w).append(" ");
				}
				strBuilder.deleteCharAt(strBuilder.length() - 1);
				VectorableToken token = new VectorableToken(
						strBuilder.toString(), word2vec, IDFWeights);
				if (token.vector != null)
					selection.add(token);
				else
					System.err.println(
							"This phrase doesn't exist in our dictionary: "
									+ word);
			} else {
				VectorableToken token = new VectorableToken(word, word2vec, IDFWeights);
				if (token.vector != null)
					selection.add(token);
				else
					System.err.println(
							"This word doesn't exist in our dictionary: "
									+ word);
				//selection.add(token);
			}
		}

		autoExploreKeyWords(selection, SIM_THRESHOLD, dataset.getVocabulary(), IDFWeights);
		return selection;
	}

	

	private static Set<String> addPhrases(List<String> wordList,
			WordVec word2vec, Dataset dataset, Map<String, Double> wordScore,
			Set<VectorableToken> selection, Set<String> seeds,Map<String, Double> IDFWeights)
			throws Throwable {
		Collection<String> phrases = expandToPhrase(seeds, dataset, wordScore);
//		PrintWriter pw = new PrintWriter(new File("D:/projects/ALPACA/securityCVE/testPhrases.csv"));
//		for(String p : phrases)
//			pw.println(p);
//		pw.close();
		// if (phrases != null)
		// selection.addAll(phrases);
		Set<String> results = new HashSet<>();
		double averageSim = avgSimilarity(selection);
		for (String phrase : phrases) {
			VectorableToken phraseToken = new VectorableToken(phrase, word2vec,IDFWeights);
			if (phraseToken.vector == null)
				continue;
			if (cosineSimilarityForWords_Avg(selection,
					phraseToken) > SIM_THRESHOLD)
				results.add(phrase);
		}
		results.addAll(seeds);
		return results;
	}

	private static class VectorableToken {
		String tokenText = null;
		float[] vector = null;
		int length = 0;
		@Override
		public boolean equals(Object obj) {
			// TODO Auto-generated method stub
			if (obj == null)
				return false;
			if (obj == this)
				return true;
			if (!(obj instanceof VectorableToken))
				return false;
			VectorableToken wordFromObj = (VectorableToken) obj;
			if (tokenText.equals(wordFromObj.tokenText))
				return true;
			else
				return false;
		}

		@Override
		public int hashCode() {
			// TODO Auto-generated method stub
			return tokenText.hashCode();
		}

		public VectorableToken(String text, WordVec word2vec,Map<String,Double> IDFWeights) {
			// TODO Auto-generated constructor stub
			// find the vector of this text
			tokenText = text;
			String[] wordList = text.split(" ");
			int validCount = 0;
			for (String word : wordList) {
				length++;
				float[] tempVector = word2vec.getVectorForWord(word);
				if (tempVector != null) {
					validCount++;
					if (vector == null)
						vector = new float[WordVec.VECTOR_SIZE];
					Double weight = IDFWeights.get(word);
					if(weight == null)
						weight = 1d;
					for (int i = 0; i < WordVec.VECTOR_SIZE; i++) {
						vector[i] += tempVector[i]*weight;
					}
				}
			}
			// average vector
			if (vector != null)
				for (int i = 0; i < WordVec.VECTOR_SIZE; i++) {
					vector[i] /= validCount;
				}
		}
	}

	


	public static List<String> expandToPhrase(Set<String> seeds,
			Dataset dataset, Map<String, Double> wordScore) throws Throwable {
		PhraseAnalyzer analyzer = PhraseAnalyzer.getInstance();
		return analyzer.expandPhrase(dataset, seeds, dataset.getLevel(),
				wordScore);
	}

	public static double cosineSimilarity(VectorableToken word1,
			VectorableToken word2) {
		// String pair = word1 + "_" + word2;
		// Double sim = pairSimiliarity.get(pair);
		// if (sim == null) {
		// Double sim = word2vec.cosineSimilarityForWords(word1, word2, true);
		Double sim = word2vec.cosineSimilarityForVectors(word1.vector,
				word2.vector, true);

		// pairSimiliarity.put(pair, sim);
		// }
		return sim;
	}

	public static void autoExploreKeyWords(Set<VectorableToken> selection,
			double threshold, Vocabulary voc,Map<String,Double> IDFWeights) throws Throwable {
		System.out.println(">>Start expanding!");
		int count = 0;
		while (true) {
			count++;
			List<VectorableToken> results = findTopSimilarWords(selection, 10,
					voc, IDFWeights);
			// System.out.println(results.toString());
			if (selection.containsAll(results)) {
				break;
			}
			selection.add(results.get(0));
			if (avgSimilarity(selection) <= threshold)// || selection.size() >
			// 20)
			{
				break;
			}
		}

		// do printing here
//		System.out.println(
//				">> done with " + count + " iteration(s) of expansion.");

	}

	private static double avgSimilarity(Collection<VectorableToken> selection) {
		double totalSim = 0;
		int count = 0;
		for (VectorableToken word1 : selection) {
			for (VectorableToken word2 : selection) {
				if (word1 != word2) {
					totalSim += cosineSimilarity(word1, word2);
					count++;
				}
			}
		}
		return totalSim / count;
	}

	private static List<VectorableToken> findTopSimilarWords(
			Set<VectorableToken> selection, int top, Vocabulary voc,Map<String,Double> IDFWeights) {
		String[] words = new String[top];
		double[] cosineDistance = new double[top];
		NatureLanguageProcessor ntlins = NatureLanguageProcessor.getInstance();
		for (DBWord word : voc.getWordList()) {
			if (ntlins.getStopWordSet().contains(word.getText())) {
				continue;
			}
			VectorableToken word2 = new VectorableToken(word.getText(),
					word2vec,IDFWeights);

			if (selection.contains(word2)) {
				continue;
			}
			double result = cosineSimilarityForWords_Avg(selection,
					word2);
			for (int i = 0; i < top; i++) {

				// System.out.println(" for (int i = 0; i < top; i++) {" +
				// result + " - " + cosineDistance[i]);
				if (result > cosineDistance[i]) {
					double lastDistance = cosineDistance[i];
					String lastWord = words[i];
					cosineDistance[i] = result;
					words[i] = word2.tokenText;
					double currentDistance = lastDistance;
					String currentWord = lastWord;
					for (int j = i + 1; j < top; j++) {
						lastDistance = cosineDistance[j];
						lastWord = words[j];
						cosineDistance[j] = currentDistance;
						if (currentWord == null) {
							words[j] = null;
						} else {
							words[j] = currentWord.intern();
						}
						currentDistance = lastDistance;
						currentWord = lastWord;
						// System.out.println(" currentWord = lastWord;->" +
						// words[i]);
						// System.out.println(" currentWord = lastWord;->" +
						// currentWord);
					}
					break;
				} else {
					continue;
				}
			}
		}
		List<VectorableToken> results = new ArrayList<>();
		for (int i = 0; i < top; i++) {
			results.add(new VectorableToken(words[i], word2vec, IDFWeights));
		}
		return results;
	}

	public static double cosineSimilarityForWords_HarmonicAvg(
			Set<VectorableToken> words, VectorableToken word2) {
		double score = 1.0;
		for (VectorableToken word1 : words) {
			score *= (1.0 - cosineSimilarity(word1, word2));
		}
		return 1.0 - score;
	}

	public static double cosineSimilarityForWords_Avg(
			Set<VectorableToken> words, VectorableToken word2) {
		float[] avgVector = new float[word2vec.VECTOR_SIZE];
		for (VectorableToken word1 : words) {
			for (int i = 0; i < word2vec.VECTOR_SIZE; i++) {
				avgVector[i] += word1.vector[i];
			}
		}
		for (int i = 0; i < word2vec.VECTOR_SIZE; i++) {
			avgVector[i] /= words.size();
		}
		return word2vec.cosineSimilarityForVectors(avgVector, word2.vector, true);
	}
}
