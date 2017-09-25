package Analyzers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.Set;

import Datastores.Dataset;
import Datastores.Document;
import Datastores.DocumentDatasetDB;
import NLP.NatureLanguageProcessor;
import NLP.SymSpell;
import Utils.Util;

public class DictionaryAnalyzer {
	public static void main(String[] args) throws Exception {
		DictionaryAnalyzer analyzer = new DictionaryAnalyzer();
		analyzer.analyzeRawData(
				"D:/projects/ALPACA/securityCVE/dictionaryAnalysis/wordFreq.csv",
				"D:/projects/ALPACA/securityCVE/rawdata/");
	}

	Map<String, Integer> wordFreq = null;

	public DictionaryAnalyzer() {
		// TODO Auto-generated constructor stub
		wordFreq = new HashMap<>();
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
}
