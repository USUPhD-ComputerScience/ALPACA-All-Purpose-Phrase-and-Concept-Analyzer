package Analyzers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ie.crf.*;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.util.StringUtils;
import org.apache.lucene.analysis.Analyzer;

import Datastores.Dataset;
import Datastores.Document;
import NLP.NatureLanguageProcessor;
import NLP.SymSpell;
import Utils.Util;

public class DictionaryAnalyzer {
	public static void main(String[] args) throws Exception {
		DictionaryAnalyzer analyzer = new DictionaryAnalyzer();
		// analyzer.analyzeRawData(
		// "D:/projects/ALPACA/securityCVE/dictionaryAnalysis/wordFreqPhrasedDESC.csv",
		// "D:/projects/ALPACA/securityCVE/dictionaryAnalysis/phrasedDesc/");
		////////////
		analyzer.findingNER(
				"D:/projects/ALPACA/securityCVE/dictionaryAnalysis/wordFreqNERBoth.csv",
				"D:/projects/ALPACA/securityCVE/rawdata/");
		////////////
		// buildRawfileForWord2vec(
		// "D:/projects/ALPACA/securityCVE/dictionaryAnalysis/rawtext.csv",
		// "D:/projects/ALPACA/securityCVE/desc/");
	}

	Map<String, Integer> wordFreq = null;

	public DictionaryAnalyzer() {
		// TODO Auto-generated constructor stub
		wordFreq = new HashMap<>();
	}

	public void findingNER(String outputFile, String inputDirectory)
			throws IOException {
		String serializedClassifier = "D:/projects/ALPACA/stanford-english-corenlp-2017-06-09-models/edu/stanford/nlp/models/ner/english.muc.7class.distsim.crf.ser.gz";
		AbstractSequenceClassifier classifier = CRFClassifier
				.getClassifierNoExceptions(serializedClassifier);
		Pattern DATETIME_REGEX = Pattern
				.compile("((<DATE>)|(<TIME>))(.+?)((</DATE>)|(</TIME>))");
		Set<String> dateTimeSet = new HashSet<>();

		Pattern TAG_REGEX = Pattern.compile("<[A-Z]+>(.+?)</[A-Z]+>");
		//
		long start = System.currentTimeMillis();
		int count = 0;
		List<String> fileLists = Util.listFilesForFolder(inputDirectory);
		for (String fileName : fileLists) {
			Scanner br = null;
			try {
				br = new Scanner(new FileReader(fileName));
				while (br.hasNextLine()) {
					String rawtext = br.nextLine().toLowerCase();
					String classifiedText = classifier
							.classifyWithInlineXML(rawtext);
					Matcher dateTimeMatcher = DATETIME_REGEX
							.matcher(classifiedText);
					while (dateTimeMatcher.find()) {
						String dt = dateTimeMatcher.group(1).replace("\"", "");
						dateTimeSet.add(dt);
					}
					Matcher matcher = TAG_REGEX.matcher(classifiedText);
					while (matcher.find()) {
						String ner = matcher.group(1).replace("\"", "");
						if (dateTimeSet.contains(ner))
							continue;
						if (Util.isNumeric(ner))
							continue;
						if (ner.contains(","))
							continue;
						if (ner.length() == 1)
							continue;

						Integer freq = wordFreq.get(ner);
						if (freq == null)
							wordFreq.put(ner, 1);
						else
							wordFreq.put(ner, freq + 1);
					}
				}
				count++;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (br != null)
					br.close();
			}
		}
		writeToFile(outputFile);
		System.out.println(">> processed " + count + " documents ");
		System.out.println(" Done! Analyzing took "
				+ (double) (System.currentTimeMillis() - start) / 1000 / 60
				+ "minutes");
	}

	public void analyzeRawData(String outputFile, String inputDirectory)
			throws Exception {
		Set<String> realDictionary = SymSpell.getInstance().getFullDictionary();
		long start = System.currentTimeMillis();
		int count = 0;
		// read documents for this dataset
		System.out.println(">> Reading raw documents...");
		List<String> fileLists = Util.listFilesForFolder(inputDirectory);
		for (String fileName : fileLists) {
			Scanner br = null;
			try {
				br = new Scanner(new FileReader(fileName));
				while (br.hasNextLine()) {
					String rawtext = br.nextLine().toLowerCase();
					NatureLanguageProcessor nlp = NatureLanguageProcessor
							.getInstance();
					List<String> tokens = NatureLanguageProcessor
							.wordSplit_wordOnly(rawtext);
					for (String w : tokens) {
						if (!realDictionary.contains(w)) {
							if (Util.isNumeric(w))
								continue;
							if (w.length() <= 1)
								continue;
							Integer freq = wordFreq.get(w);
							if (freq == null)
								wordFreq.put(w, 1);
							else
								wordFreq.put(w, freq + 1);
						}
					}
				}
				count++;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (br != null)
					br.close();
			}
		}
		writeToFile(outputFile);
		System.out.println(">> processed " + count + " documents ");
		System.out.println(" Done! Analyzing took "
				+ (double) (System.currentTimeMillis() - start) / 1000 / 60
				+ "minutes");
	}

	private void writeToFile(String outputFile) throws FileNotFoundException {
		PrintWriter pwFreq = new PrintWriter(new File(outputFile));
		for (Entry<String, Integer> entry : wordFreq.entrySet()) {
			pwFreq.println(entry.getKey() + "," + entry.getValue());
		}
		pwFreq.close();
	}

	private static void buildRawfileForWord2vec(String outputFile,
			String inputDirectory) throws IOException {
		List<String> fileLists = Util.listFilesForFolder(inputDirectory);
		PrintWriter pwFreq = new PrintWriter(new File(outputFile));
		for (String fileName : fileLists) {
			Scanner br = null;
			try {
				br = new Scanner(new FileReader(fileName));
				while (br.hasNextLine()) {
					String rawtext = br.nextLine().toLowerCase();
					pwFreq.println(rawtext);
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (br != null)
					br.close();
			}
		}
		pwFreq.close();
	}
}
