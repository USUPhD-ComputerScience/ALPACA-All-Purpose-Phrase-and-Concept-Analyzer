package Analyzers;

import Datastores.DocumentDatasetDB;
import TextNormalizer.TextNormalizer;

public class ConceptAnalysis {
	public static void main(String[] args) throws Throwable {
		PhraseAnalyzer analyzer = PhraseAnalyzer.getInstance();

		TextNormalizer normalizer = TextNormalizer.getInstance();
		normalizer.readConfigINI(
				"D:\\EclipseWorkspace\\TextNormalizer\\config.INI");

		long start = System.currentTimeMillis();
		analyzer.readWordsSkewness(KeywordAnalyzer.WEIBULL_FREQUENCY, "D:\\projects\\ALPACA\\NSF\\wordScore\\scoreLV2.csv");
		analyzer.extractFeatures(1,
				"D:\\projects\\ALPACA\\NSF\\concepts\\seeds_beta.csv",
				DocumentDatasetDB.LV2_ROOTWORD_STEMMING, PhraseAnalyzer.SCORING_STANFORD_SENTIMENT);
		System.out.println(" Done! extracting features took "
				+ (double) (System.currentTimeMillis() - start) / 1000 / 60
				+ "minutes");
	}
}
