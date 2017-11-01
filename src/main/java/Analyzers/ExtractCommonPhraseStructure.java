package Analyzers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import MainTasks.PreprocesorMain;

import java.util.Scanner;
import java.util.Set;

import NLP.NatureLanguageProcessor;
import StanfordNLP.StanfordNLPProcessing;
import TextNormalizer.TextNormalizer;
import Utils.Util;
import edu.stanford.nlp.trees.Tree;

public class ExtractCommonPhraseStructure {
	private static final boolean DEBUG = false;
	private static final int THRESHOLD = 5;
	private Map<String, Integer> POSStat = new HashMap<>();
	private Map<String, String> POSExample = new HashMap<>();
	private Set<String> connectors = new HashSet<>();
	private Set<String> whs = new HashSet<>();
	private Set<String> others = new HashSet<>();
	private Set<String> negations = new HashSet<>();
	private Set<String> intensifiers = new HashSet<>();
	private Set<String> interestingWords = new HashSet<>();

	public ExtractCommonPhraseStructure()
			throws Throwable {
		connectors.addAll(loadWordsSet(new File(TextNormalizer.getDictionaryDirectory()
				+ "baseword/misc/connectors.txt")));
		negations.addAll(loadWordsSet(
				new File(TextNormalizer.getDictionaryDirectory()
						+ "baseword/misc/negations.txt")));
		intensifiers.addAll(loadWordsSet(new File(TextNormalizer.getDictionaryDirectory()
				+ "baseword/misc/intensifiers.txt")));
		whs.addAll(loadWordsSet(new File(TextNormalizer.getDictionaryDirectory()
				+ "baseword/misc/wh.txt")));
		others.addAll(loadWordsSet(new File(TextNormalizer.getDictionaryDirectory()
				+ "baseword/misc/others.txt")));
		interestingWords.addAll(connectors);
		interestingWords.addAll(negations);
		interestingWords.addAll(intensifiers);
		interestingWords.addAll(whs);
		interestingWords.addAll(others);
		//processPhrases(directory, sentenceThreshold,  levelOfStemming, fileOutput);
		// TODO Auto-generated constructor stub
	}

	public void processPhrases(String directory, int sentenceThreshold, int levelOfStemming, String fileOutput) throws Throwable {
		System.out.println("Start extracting templates from input: " + directory);
		TextNormalizer normalizer = TextNormalizer.getInstance();
		StanfordNLPProcessing nlpStan = StanfordNLPProcessing.getInstance();
		NatureLanguageProcessor nlp = NatureLanguageProcessor.getInstance();
		List<String> fileList = Util.listFilesForFolder(directory);
		int processCount = 0, lineCount = 0, totalfile = fileList.size(),
				sentenceCount = 0;
		for (String fileInput : fileList) {
			 if (lineCount == sentenceThreshold)
			 break;
			Scanner br = new Scanner(new FileReader(fileInput));
			while (br.hasNextLine()) {
				 if (lineCount == sentenceThreshold)
				 break;
				String line = br.nextLine();
				List<List<String>> sentenceList = normalizer
						.normalize_SplitSentence(line,
								PreprocesorMain.LV1_SPELLING_CORRECTION, false);
				if(sentenceList == null)
					continue;
				for (List<String> sentence : sentenceList) {
					// apply stanford phrasal on LV1 normalized sentence.
					StringBuilder sentenceBld = new StringBuilder();
					for(String token : sentence){
						sentenceBld.append(token).append(" ");
					}
					sentenceBld.deleteCharAt(sentenceBld.length()-1);
					
					List<String> phrases = nlpStan.getPhrasalTokens(sentenceBld.toString());
					for (String phrase : phrases) {
						// find POS tag for this phrase
						List<List<String>> tempList = normalizer
								.normalize_SplitSentence(line,
										levelOfStemming, true);
						if(tempList.isEmpty())
							continue;
						List<String> words = tempList.get(0);
						StringBuilder POS = new StringBuilder();
						StringBuilder stemmedText = new StringBuilder();
						for (String word : words) {
							String[] w = word.split("_");
							if (w.length == 2 && nlp.POSSET.contains(w[1])) {
								if (interestingWords.contains(w[0]))
									POS.append(w[0] + "_");
								else
									POS.append(w[1] + "_");
							}
							stemmedText.append(word).append(" ");
						}
						if (POS.length() == 0)
							continue;
						POS.deleteCharAt(POS.length() - 1);
						stemmedText.deleteCharAt(stemmedText.length() - 1);
						String posstr = POS.toString();
						Integer count = POSStat.get(posstr);
						if (count == null){
							POSStat.put(posstr, 1);
							POSExample.put(posstr, stemmedText+","+phrase);
						}else
							POSStat.put(posstr, count + 1);
						if (DEBUG)
							System.out.println(POS.toString());
						processCount++;
						if (processCount % 10000 == 0)
							System.out.println("Extracted " + processCount
									+ " phrases from " + sentenceCount
									+ " sentences from" + lineCount
									+ " reviews.");
					}
					sentenceCount++;
				}
				lineCount++;
				if(lineCount % 10000 == 0)
					System.out.println("processed " + lineCount + " sentences");
//				if (lineCount % 50000 == 0) {
//					writeToFile(
//							fileOutput);
//					System.err.println("Backed up to file");
//				}
			}
			br.close();

			System.out.println("processed " + lineCount + " documents");
			writeToFile(
					fileOutput);
			// if (sentenceCount == sentenceThreshold)
			// break;
		}

		System.err.println("Extracted " + processCount + " phrases from "
				+ sentenceCount + " sentences from" + lineCount + " reviews.");
	}

	private static Set<String> loadWordsSet(File testDataFile)
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

	public void writeToFile(String output) throws FileNotFoundException {
		PrintWriter outputAll = new PrintWriter(output);
		outputAll.println("POS,count");
		for (Entry<String, Integer> entry : POSStat.entrySet()) {
			int count = entry.getValue();
			String example = POSExample.get(entry.getKey());
			if (count >= THRESHOLD)
				outputAll.println(entry.getKey() + "," + entry.getValue() +"," +example);
		}
		outputAll.close();
	}

	private static void readFile(String filename) throws FileNotFoundException {

		// TODO Auto-generated method stub
		Scanner br = new Scanner(new FileReader(new File(filename)));
		while (br.hasNextLine()) {
			System.out.println(br.nextLine());
		}
		br.close();
	}

	public static void main(String[] args) throws Throwable {
		// readFile("D:\\projects\\concernsReviews\\POSpatterns\\AmazonreviewsNew\\reviewsNew.txt");
//		ExtractCommonPhraseStructure ecps = new ExtractCommonPhraseStructure(
//				"D:\\projects\\concernsReviews\\POSpatterns\\AmazonreviewsNew\\",
//				6000000);
//		ecps.writeToFile(
//				"D:\\projects\\concernsReviews\\POSpatterns\\posPattern.csv");
		System.out.println("done!");
	}
}
