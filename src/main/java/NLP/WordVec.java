package NLP;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.Scanner;
import java.util.Set;

import TextNormalizer.TextNormalizer;
import Utils.Util;

public class WordVec {

	private final Map<String, float[]> wordVector;
	private static Map<String, float[]> phraseVector = new HashMap<>();
	public static final int VECTOR_SIZE = 200;
	public static final String VECTOR_FILE = "D:/projects/ALPACA/NSF/vectors.txt";

	public static void main(String[] args) throws IOException {
		// normalizeWord2vec();
		// reformClusterELKI();
		// DBScanCluster();
		// filterForELKIDensityClustering();
		collectData("D:\\projects\\ALPACA\\NSF\\preprocessed_LV2\\");
	}

	private static void collectData(String directory) throws IOException {

		List<String> fileLists = Util.listFilesForFolder(directory);
		Scanner br = null;
		PrintWriter pw = new PrintWriter(
				new FileWriter(directory + "corpusTrainingData.txt"));
		for (String fileName : fileLists) {
			try {
				br = new Scanner(new FileReader(fileName));
				String line = br.nextLine();
				pw.println(line);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (br != null)
					br.close();
			}

		}
		pw.close();
	}

	public double cosineSimilarityForVectors(float[] vector1, float[] vector2,
			boolean normalize) {
		double sim = 0, square1 = 0, square2 = 0;
		if (vector1 == null || vector2 == null) {
			return 0;
		}
		for (int i = 0; i < vector1.length; i++) {
			square1 += vector1[i] * vector1[i];
			square2 += vector2[i] * vector2[i];
			sim += vector1[i] * vector2[i];
		}
		if (!normalize) {
			return sim / Math.sqrt(square1) / Math.sqrt(square2);
		} else {
			return (1 + sim / Math.sqrt(square1) / Math.sqrt(square2)) / 2;
		}
	}
	public double cosineSimilarityForVectors(double[] vector1, double[] vector2,
			boolean normalize) {
		double sim = 0, square1 = 0, square2 = 0;
		if (vector1 == null || vector2 == null) {
			return 0;
		}
		for (int i = 0; i < vector1.length; i++) {
			square1 += vector1[i] * vector1[i];
			square2 += vector2[i] * vector2[i];
			sim += vector1[i] * vector2[i];
		}
		if (!normalize) {
			return sim / Math.sqrt(square1) / Math.sqrt(square2);
		} else {
			return (1 + sim / Math.sqrt(square1) / Math.sqrt(square2)) / 2;
		}
	}
	private static void reformClusterELKI() throws IOException {
		String directory = "D:\\EclipseWorkspace\\MARK\\lib\\dictionary\\word2vecTrainingData\\clusterDensity\\";
		File fcheckExist = new File(directory);
		if (!fcheckExist.exists()) {
			System.err.println("File not found: " + directory);
			return;
		}
		List<String> fileLists = Util.listFilesForFolder(directory);
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileWriter(
					new File(directory + "eyeFriendlyCluster.csv")));
			for (String fileName : fileLists) {
				String[] fileTokens = fileName.split("\\\\");
				if (!fileTokens[fileTokens.length - 1].contains("cluster"))
					continue;
				Scanner br = null;
				try {

					br = new Scanner(new FileReader(fileName));
					br.nextLine(); // comment
					br.nextLine();// comment
					br.nextLine();// comment
					br.nextLine();// comment
					while (br.hasNextLine()) {
						String line = br.nextLine();
						String[] tokens = line.split("\\s");
						String word = tokens[tokens.length - 1];
						pw.print(word + " ");
					}

				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					if (br != null)
						br.close();
				}
				pw.println();
			}
		} finally {
			if (pw != null)
				pw.close();
		}
	}

	private static void filterForELKIDensityClustering() throws IOException {
		System.err.println("begin filtering word2vec words");
		Scanner br = null;
		PrintWriter pw = null;
		TextNormalizer normalizer = TextNormalizer.getInstance();
		normalizer.readConfigINI(
				"D:\\EclipseWorkspace\\TextNormalizer\\config.INI");
		Set<String> stopWords = NLP.NatureLanguageProcessor.getInstance()
				.getStopWordSet();
		try {
			br = new Scanner(new FileReader(
					"D:\\EclipseWorkspace\\MARK\\lib\\dictionary\\word2vecTrainingData\\ReviewVectors.txt"));
			pw = new PrintWriter(new FileWriter(new File(
					"D:\\EclipseWorkspace\\MARK\\lib\\dictionary\\word2vecTrainingData\\ReviewVectors_norm.txt")));
			int vocabSize = br.nextInt();
			int layer1Size = br.nextInt();
			Map<String, float[]> vectorSet = new HashMap<>();
			NumberFormat formatter = new DecimalFormat("#0.000000");
			for (int a = 0; a < vocabSize; a++) {
				String word = br.next();
				// remove numbers
				if (Util.isNumeric(word) || stopWords.contains(word)
						|| Utils.Util.hasSpecialCharacters(word)) {
					for (int b = 0; b < layer1Size; b++) {
						br.nextFloat();
					}
					continue;
				}

				float[] vector = new float[layer1Size];
				for (int b = 0; b < layer1Size; b++) {
					vector[b] = br.nextFloat();
				}
				vectorSet.put(word, vector);
			}
			for (Entry<String, float[]> entry : vectorSet.entrySet()) {
				String word = entry.getKey();
				float[] vector = entry.getValue();
				for (int b = 0; b < layer1Size; b++) {
					pw.print(formatter.format(vector[b]) + " ");
				}
				pw.println(word);
			}

		} finally {
			if (br != null)
				br.close();
			if (pw != null)
				pw.close();

		}
		System.out.println("done filtering!");
	}

	private static void normalizeWord2vec() throws IOException {
		System.err.println("begin normalizing word2vec words");
		Scanner br = null;
		PrintWriter pw = null;
		try {
			br = new Scanner(new FileReader(
					"D:\\EclipseWorkspace\\MARK\\lib\\dictionary\\word2vecTrainingData\\ReviewVectors.txt"));
			pw = new PrintWriter(new FileWriter(new File(
					"D:\\EclipseWorkspace\\MARK\\lib\\dictionary\\word2vecTrainingData\\ReviewVectors_norm.txt")));
			int vocabSize = br.nextInt();
			int layer1Size = br.nextInt();
			Set<String> wordSet = new HashSet<>();
			NumberFormat formatter = new DecimalFormat("#0.000000");
			for (int a = 0; a < vocabSize; a++) {
				String word = br.next();
				if (!wordSet.contains(word)) {
					wordSet.add(word);
					for (int b = 0; b < layer1Size; b++) {

						pw.print(formatter.format(br.nextFloat()) + " ");
					}
					pw.print(word.replaceAll("'", "SGLQUOTE").replace("</s>",
							"WHITECHARACTER"));
					pw.println();
				} else {
					for (int b = 0; b < layer1Size; b++) {
						br.nextFloat();
					}
				}
			}

		} finally {
			if (br != null)
				br.close();
			if (pw != null)
				pw.close();

		}
		System.out.println("done normalizing!");
	}

	public WordVec() {
		wordVector = new HashMap<>();
		try {
			System.out.print("Loading Word Vectors");
			loadTextModel(VECTOR_FILE);
			System.out.println("-Done!");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public WordVec(String vectorFile) {
		wordVector = new HashMap<>();
		try {
			System.out.print("Loading Word Vectors from " + vectorFile);
			loadTextModel(vectorFile);
			System.out.println("-Done!");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public float[] getVectorForPhrase(String phrase) {
		float[] vector = phraseVector.get(phrase);
		if (vector != null)
			return vector;
		vector = new float[VECTOR_SIZE];
		String[] words1 = phrase.split(" ");
		for (String word : words1) {
			float[] wordvec = wordVector.get(word);
			if (wordvec != null) {
				for (int i = 0; i < vector.length; i++) {
					vector[i] += wordvec[i];
				}
			} else {
				return null;
			}

		}
		phraseVector.put(phrase, vector);

		return vector;
	}

	public double cosineSimilarityForPhrases(String phrase1, String phrase2,
			boolean normalize) {

		double sim = 0, square1 = 0, square2 = 0;
		float[] vector1 = getVectorForPhrase(phrase1);
		float[] vector2 = getVectorForPhrase(phrase2);

		if (vector1 == null || vector2 == null)
			return 0;
		for (int i = 0; i < vector1.length; i++) {
			square1 += vector1[i] * vector1[i];
			square2 += vector2[i] * vector2[i];
			sim += vector1[i] * vector2[i];
		}
		if (sim == 0)
			return 0;
		if (!normalize)
			return sim / Math.sqrt(square1) / Math.sqrt(square2);
		else
			return (1 + sim / Math.sqrt(square1) / Math.sqrt(square2)) / 2;
	}

	public Map<String, float[]> getWordVector() {
		return wordVector;
	}

	public float[] getVectorForWord(String word) {
		return wordVector.get(word);
	}

	public void loadTextModel(String filename) throws FileNotFoundException {
		Scanner br = null;
		try {
			br = new Scanner(new FileReader(filename));
			br.nextLine(); // read the first useless line
			while(br.hasNext()){
				String word = br.next();
				float[] vector = new float[VECTOR_SIZE];
				for (int b = 0; b < VECTOR_SIZE; b++) {
					vector[b] = br.nextFloat();
				}
				wordVector.put(word, vector);
			}

		} finally {
			if (br != null)
				br.close();

		}
	}

	public double cosineSimilarityForWords(String word1, String word2,
			boolean normalize) {
		double sim = 0, square1 = 0, square2 = 0;
		float[] vector1 = wordVector.get(word1);
		float[] vector2 = wordVector.get(word2);
		if (vector1 == null || vector2 == null)
			return 0;
		for (int i = 0; i < vector1.length; i++) {
			square1 += vector1[i] * vector1[i];
			square2 += vector2[i] * vector2[i];
			sim += vector1[i] * vector2[i];
		}
		if (!normalize)
			return sim / Math.sqrt(square1) / Math.sqrt(square2);
		else
			return (1 + sim / Math.sqrt(square1) / Math.sqrt(square2)) / 2;
	}

}
