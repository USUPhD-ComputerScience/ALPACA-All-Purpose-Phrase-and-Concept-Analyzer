package Analyzers;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import NLP.NatureLanguageProcessor;
import NLP.WordVec;
import Utils.POSTagConverter;

public class ClusterAnalyzer {
	private static Map<String, Double> wordScore = null;
	public static final int AVERAGE_JACCARD = 1;
	public static final int AVERAGE_WORD = 2;
	private static final String SCORING_FILENAME = "D:\\projects\\ALPACA\\NSF\\wordScore\\scoreLV2.csv";

	public static void readWordsSkewness(int typeOfScore, String fileName)
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

	public static void main(String[] args) throws Throwable {
		// clusterPhrases("D:/projects/ALPACA/NSF/concepts/seeds_beta.csv",
		// "D:/projects/ALPACA/NSF/clusters/seeds_beta.csv",
		// "D:/projects/ALPACA/NSF/vectors.txt", AVERAGE_JACCARD, 0.3);
		clusterWords("D:/projects/ALPACA/NSF/clusters/seeds_beta.csv",
				"D:/projects/ALPACA/NSF/vectors.txt", AVERAGE_JACCARD);
	}

	public static void clusterWords(String outputFile, String vectorFile,
			int typeOfSimMetric) throws Throwable {
		WordVec word2vec = new WordVec(vectorFile);
		System.out.println("> Reading words from file");
		readWordsSkewness(KeywordAnalyzer.WEIBULL_FREQUENCY, SCORING_FILENAME);
		ArrayList<Item> words = loadWordsIntoItemList(wordScore, word2vec);
		cluster(new File(outputFile), words, typeOfSimMetric, word2vec);
		System.out.println("> Done!");
	}

	public static void clusterPhrases(String inputFile, String outputFile,
			String vectorFile, int typeOfSimMetric, double minScore)
			throws Throwable {
		WordVec word2vec = new WordVec(vectorFile);
		System.out.println("> Reading phrases from file: " + inputFile);
		ArrayList<Item> phrases = loadTestSet(new File(inputFile), word2vec,
				minScore);
		cluster(new File(outputFile), phrases, typeOfSimMetric, word2vec);
		System.out.println("> Done!");
	}

	private static void cluster(File file, ArrayList<Item> phrases,
			int typeOfSimMetric, WordVec word2vec) throws Throwable {
		List<List<ItemWithSim>> clusters = blurryClustering(phrases, 0.75, 500,
				0.5, typeOfSimMetric, word2vec);
		System.out.println("> Write clusters to file");
		writeClustersToFile(clusters, file);
	}

	private static void writeClustersToFile(List<List<ItemWithSim>> clusters,
			File file) throws Throwable {
		PrintWriter pw = new PrintWriter(new FileWriter(file));
		for (List<ItemWithSim> cluster : clusters) {
			if (cluster.isEmpty()) {
				continue;
			}
			ItemWithSim mainTopic = (ItemWithSim) cluster.get(0);
			pw.print(mainTopic.item.toString() + ",");
			for (ItemWithSim item : cluster) {
				ItemWithSim word = (ItemWithSim) item;
				pw.print("<" + word.item.toString() + "_" + word.sim + "> ");
			}
			pw.print("," + mainTopic.score);
			pw.println();
		}

		pw.close();
	}

	private static ArrayList<Item> loadWordsIntoItemList(
			Map<String, Double> wordScore, WordVec word2vec) throws Throwable {
		// TODO Auto-generated method stub
		int count = 0;
		ArrayList<Item> words = new ArrayList<>();
		for (Entry<String, Double> entry : wordScore.entrySet()) {
			Item item = new Item(entry.getKey(), entry.getValue(), 0, word2vec);
			if (item.getVector() != null)
				words.add(item);
			count++;
		}
		System.out.println(">> Done! Read " + count + " phrases!");
		return words;
	}

	private static ArrayList<Item> loadTestSet(File file, WordVec word2vec,
			double minScore) throws Throwable {
		// TODO Auto-generated method stub
		ArrayList<Item> words = new ArrayList<>();
		Map<String, Double> phrasesset = new HashMap<>();
		Scanner br = new Scanner(new FileReader(file));
		// String first = br.nextLine();
		int count = 0;
		while (br.hasNextLine()) {
			String[] values = br.nextLine().split(",");
			String phrase = values[0];
			double score = Double.parseDouble(values[2]);
			if (score < minScore)
				continue;
			// double score = Double.parseDouble(values[7]);
			// Double lastScore = phrasesset.get(phrase);
			// if (lastScore == null)
			// phrasesset.put(phrase, score);
			// else
			phrasesset.put(phrase, score);
		}
		for (Entry<String, Double> phrase : phrasesset.entrySet()) {
			Item item = new Item(phrase.getKey(), phrase.getValue(), 0,
					word2vec);
			if (item.getVector() != null)
				words.add(item);
			count++;
		}
		br.close();
		System.out.println(">> Done! Read " + count + " phrases!");
		return words;
	}

	public static List<List<ItemWithSim>> blurryClustering(List<Item> itemList,
			double blurryRate, int clusterMaxSize, double simThreshold,
			int typeOfSimMetric, WordVec word2vec) {
		List<List<ItemWithSim>> results = new ArrayList<>();
		System.out.println("Start clustering");
		if (clusterMaxSize < 1) {
			System.out.println(
					"Finished clustering because clusterMaxSize == 0!");
			return results;
		}
		Set<ItemWithSim> doneFor = new HashSet<>();
		for (Item item : itemList) {
			if (doneFor.contains(item))
				continue;
			if (item.vector == null)
				continue;
			List<ItemWithSim> temporaryClusterHolder = new ArrayList<>();
			ItemWithSim leader = new ItemWithSim(item, 1.0);
			leader.score = item.badScore;
			temporaryClusterHolder.add(leader);
			for (Item itemForReference : itemList) {
				if (itemForReference.vector == null
						&& typeOfSimMetric == AVERAGE_WORD)
					continue;
				double sim = 0;
				if (typeOfSimMetric == AVERAGE_WORD)
					sim = KMeanClustering.cosineSimilarityForVectors(
							item.vector, itemForReference.vector, true);
				if (typeOfSimMetric == AVERAGE_JACCARD)
					sim = phraseSimilarity_averageJaccard(itemForReference.gram,
							itemForReference.gram, word2vec, "_");
				if (sim < simThreshold)
					continue;
				ItemWithSim newItem = new ItemWithSim(itemForReference, sim);
				if (doneFor.contains(newItem))
					continue;
				if (temporaryClusterHolder.contains(newItem))
					continue;
				leader.score += itemForReference.badScore;
				temporaryClusterHolder.add(newItem);
			}
			// results.add(temporaryClusterHolder);

			// sort and pick out the not-blurry items
			temporaryClusterHolder.sort(new Comparator<ItemWithSim>() {

				@Override
				public int compare(ItemWithSim o1, ItemWithSim o2) {
					// TODO Auto-generated method stub
					double score = o2.sim - o1.sim;
					if (score > 0)
						return 1;
					if (score < 0)
						return -1;
					return 0;
				}
			});
			int count = 0;
			List<ItemWithSim> cluster = new ArrayList<>();
			for (ItemWithSim it : temporaryClusterHolder) {
				cluster.add(it);
				doneFor.add(it);
				count++;
				if (count
						/ (double) temporaryClusterHolder.size() >= blurryRate)
					break;
				if (count == clusterMaxSize)
					break;

			}
			results.add(cluster);
		}
		System.out.println("Done with " + results.size() + " clusters");
		return results;
	}

	private static class ItemForClustering {

		public double sim;
		public int firstI;
		public int secondI;

		public ItemForClustering(double si, int f, int s) {
			sim = si;
			firstI = f;
			secondI = s;
		}
	}

	private static List<ItemForClustering> buildMaximalMatrix(
			Phrase firstPhrase, Phrase secondPhrase, WordVec word2vec1) {

		String[] firstWordArray = firstPhrase.mPhrase;
		String[] secondWordArray = secondPhrase.mPhrase;
		List<ItemForClustering> sortedList = new ArrayList<>();
		for (int firstI = 0; firstI < firstWordArray.length; firstI++) {
			for (int secondI = 0; secondI < secondWordArray.length; secondI++) {
				ItemForClustering item = new ItemForClustering(
						word2vec1.cosineSimilarityForWords(
								firstWordArray[firstI],
								secondWordArray[secondI], true),
						firstI, secondI);
				sortedList.add(item);
			}
		}
		sortedList.sort(new Comparator<ItemForClustering>() {
			@Override
			public int compare(ItemForClustering o1, ItemForClustering o2) {
				if (o1.sim > o2.sim) {
					return -1;
				}
				if (o1.sim < o2.sim) {
					return 1;
				}
				return 0;
			}
		});
		Set<Integer> firstSet = new HashSet<>();
		Set<Integer> secondSet = new HashSet<>();
		List<ItemForClustering> matchedPairs = new ArrayList<>();
		for (ItemForClustering item : sortedList) {
			if (firstSet.contains(item.firstI)) {
				// System.out.println("yolo");
				continue;
			}
			if (secondSet.contains(item.secondI)) {
				// System.out.println("swag");
				continue;
			}
			matchedPairs.add(item);
			firstSet.add(item.firstI);
			secondSet.add(item.secondI);
		}
		return matchedPairs;
	}

	//////////////////// PHRASAL SIMILARITY ///////////////////////////////
	public static double phraseSimilarity_averageJaccard(String p1, String p2,
			WordVec word2vec, String regex) {
		Phrase firstPhrase = new Phrase(p1, regex);
		Phrase secondPhrase = new Phrase(p2, regex);
		List<ItemForClustering> matchedPairs = buildMaximalMatrix(firstPhrase,
				secondPhrase, word2vec);
		double similarity = 0;
		for (ItemForClustering item : matchedPairs) {
			similarity += item.sim;
		}
		int intersection = matchedPairs.size();
		// calculate compensation similarity score
		similarity /= intersection;

		double Total = (double) intersection / (firstPhrase.mPhrase.length
				+ secondPhrase.mPhrase.length - intersection);

		return similarity * Total;
	}

	private static class Phrase {

		private String[] mPhrase = null;
		// private int hash = 0;

		public Phrase(String phrase, String regex) {
			mPhrase = phrase.split(regex);
		}

		@Override
		public String toString() {
			return getPhraseString(); // To change body of generated methods,
										// choose
										// Tools | Templates.
		}

		public String getPhraseString() {
			StringBuilder str = new StringBuilder();
			for (String w : mPhrase) {
				str.append(w).append("_");
			}
			str.deleteCharAt(str.length() - 1);
			return str.toString();
		}

	}

	public static class ItemWithSim {
		Item item = null;
		double sim;
		double score = 0;

		@Override
		public int hashCode() {
			// TODO Auto-generated method stub
			return item.gram.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			// TODO Auto-generated method stub
			if (obj instanceof ItemWithSim) {
				ItemWithSim item2 = (ItemWithSim) obj;
				if (item.gram.equals(item2.item.gram))
					return true;
				return false;

			}
			return false;
		}

		public ItemWithSim(Item it, double si) {
			item = it;
			sim = si;
			// TODO Auto-generated constructor stub
		}
	}

	public static List<String> mergeExpandedPhrase(List<int[]> expandedPhrases,
			String[] wordList) {
		boolean[] indexArray = new boolean[wordList.length];
		for (int[] phraseRange : expandedPhrases) {
			Arrays.fill(indexArray, phraseRange[0], phraseRange[1] + 1, true);
			// for (int i = phraseRange[0]; i <= phraseRange[1]; i++) {
			// indexArray[i] = true;
			// }
		}
		List<String> actualPhrases = new ArrayList<>();
		StringBuilder strTemporal = null;
		for (int i = 0; i < indexArray.length; i++) {
			if (indexArray[i] == true) {
				if (strTemporal == null) {
					strTemporal = new StringBuilder();
					strTemporal.append(wordList[i]);
				} else
					strTemporal.append(" ").append(wordList[i]);
			} else {
				if (strTemporal != null) {
					actualPhrases.add(strTemporal.toString());
					strTemporal = null;
				}
			}
		}
		if (strTemporal != null) {
			actualPhrases.add(strTemporal.toString());
			strTemporal = null;
		}
		return actualPhrases;
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
			expandedPhrases = mergeExpandedPhrase(inputPhraseRanges, wordList);
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

		// weighted average for new gram vector
		public Item(String grm, double bad, double good, WordVec word2vec)
				throws Throwable {
			// TODO Auto-generated constructor stub

			if (wordScore == null)
				readWordsSkewness(KeywordAnalyzer.WEIBULL_FREQUENCY,
						SCORING_FILENAME);
			gram = grm;
			badScore = bad;
			goodScore = good;
			actualPhrase = new HashSet<>();
			if (word2vec != null) {
				POSTagConverter POSconverter = POSTagConverter.getInstance();
				String[] wordList = gram.split(" ");
				int validCount = 0;
				for (String word : wordList) {
					// if (!analyst.topicWords.contains(word))
					// continue;

					if ((byte) 0xFF != POSconverter.getCode(word))
						continue;
					Double score = wordScore.get(word);
					if (score == null)
						continue;
					float[] tempVector = word2vec.getVectorForWord(word);
					if (tempVector != null) {
						validCount++;
						if (vector == null)
							vector = new double[WordVec.VECTOR_SIZE];
						for (int i = 0; i < WordVec.VECTOR_SIZE; i++) {
							vector[i] += tempVector[i] * score;
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
