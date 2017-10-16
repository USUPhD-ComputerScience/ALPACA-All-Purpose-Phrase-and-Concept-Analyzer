package Analyzers;

import Datastores.Dataset;
import MainTasks.Alpaca;
import MainTasks.PreprocesorMain;
import TextNormalizer.TextNormalizer;

public class ConceptAnalysis {
	public static void main(String[] args) throws Throwable {
		PhraseAnalyzer analyzer = PhraseAnalyzer.getInstance();

		TextNormalizer normalizer = TextNormalizer.getInstance();
		normalizer.readConfigINI(
				"D:\\EclipseWorkspace\\TextNormalizer\\config.INI");

		long start = System.currentTimeMillis();
		analyzer.readWordsSkewness(KeywordAnalyzer.WEIBULL_FREQUENCY,
				"D:\\projects\\ALPACA\\NSF\\wordScore\\scoreLV2.csv");
		Dataset data = Alpaca.readProcessedData("D:/projects/ALPACA/NSF/",
				PreprocesorMain.LV2_ROOTWORD_STEMMING);
		analyzer.extractFeatures(data,
				"D:\\projects\\ALPACA\\NSF\\concepts\\seeds_beta.csv",
				PreprocesorMain.LV2_ROOTWORD_STEMMING,
				PhraseAnalyzer.SCORING_STANFORD_SENTIMENT);
		System.out.println(" Done! extracting features took "
				+ (double) (System.currentTimeMillis() - start) / 1000 / 60
				+ "minutes");
	}

	public static void explorePhrase(Dataset data, int typeOfScore,
			String scoringFile, String outputFile, int level, int phraseScoring)
			throws Throwable {
		PhraseAnalyzer analyzer = PhraseAnalyzer.getInstance();

		TextNormalizer normalizer = TextNormalizer.getInstance();
		normalizer.readConfigINI(
				"D:\\EclipseWorkspace\\TextNormalizer\\config.INI");

		long start = System.currentTimeMillis();
		analyzer.readWordsSkewness(typeOfScore, scoringFile);
		analyzer.extractFeatures(data, outputFile, level, phraseScoring);
		System.out.println(" Done! extracting features took "
				+ (double) (System.currentTimeMillis() - start) / 1000 / 60
				+ "minutes");
	}
}
