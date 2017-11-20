package StanfordNLP;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.util.Version;

import Utils.Util;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.parser.lexparser.EnglishTreebankParserParams;
import edu.stanford.nlp.parser.lexparser.Options;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.sentiment.SentimentModel;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;

public class StanfordNLPProcessing {
	private static StanfordNLPProcessing instance = null;
	// adding a couple extra terms to standard lucene list to test against
	// private static String customStopWordList = "";
	private Properties customProps;
	private static StanfordCoreNLP customPipeline;
	// private static Set<?> stopWords;
	private static final int PHRASE_MAX_SIZE = 30;
	private static final String[] SENTIMENT_MEANING = { "Very Negative",
			"Negative", "Neutral", "Positive", "Very Positive" };
	private static final HashSet<String> POSTAG_OF_NOUN = new HashSet<>(
			Arrays.asList(new String[] { "NN", "NNS", "NP" }));
	private static final HashSet<String> POSTAG_OF_ADJECTIVE = new HashSet<>(
			Arrays.asList(new String[] { "ADJP", "JJ" }));
	private static final HashSet<String> POSTAG_OF_VERB = new HashSet<>(
			Arrays.asList(new String[] { "VBG", "VBP", "VB", "VP", "@VP", "VBZ",
					"VBN" }));
	private static final HashSet<String> POSTAG_OF_VERB_COMPLEMENT = new HashSet<>(
			Arrays.asList(new String[] { "ADVP", "PP", "PRT", "ADJP", "S" }));
	private static final HashSet<String> POSTAG_OF_VOCABULARY = new HashSet<>(
			Arrays.asList(new String[] { "NN", "ADJP", "JJ", "VBG", "VBP",
					"VBN", "VBZ", "VB", "NP", "VP", "NNS" }));
	private Options mOp;

	// private static void readStopWordsFromFile() {
	// System.out.println("Read StopWords from file - english.stop");
	// StringBuilder text = new StringBuilder();
	// CSVReader reader = null;
	// try {
	// reader = new CSVReader(new FileReader("english.stop"));
	// String[] row = null;
	// String prefix = "";
	// while ((row = reader.readNext()) != null) {
	// text.append(prefix);
	// prefix = ",";
	// text.append(row[0]);
	// }
	// customStopWordList = text.toString();
	// } catch (FileNotFoundException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } finally {
	// if (reader != null)
	// try {
	// reader.close();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	// }

	protected StanfordNLPProcessing() {
		// Exists only to defeat instantiation.
		// readStopWordsFromFile();
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization,
		// NER, parsing, and coreference resolution
		customProps = new Properties();

		mOp = new Options(new EnglishTreebankParserParams());
		customProps.put("annotators",
				"tokenize, ssplit, pos,lemma, ner, parse, dcoref,sentiment");
		customProps.setProperty("customAnnotatorClass.stopword",
				"StanfordNLP.StopwordAnnotator");
		// customProps.setProperty(StopwordAnnotator.STOPWORDS_LIST,
		// customStopWordList);
		customPipeline = new StanfordCoreNLP(customProps);
		// get the custom stopword set
		// stopWords = StopwordAnnotator.getStopWordList(Version.LUCENE_36,
		// customStopWordList, true);
		// get the standard lucene stopword set
		// Set<?> stopWords = StopAnalyzer.ENGLISH_STOP_WORDS_SET;

		/*
		 * // read some text in the text variable String text =
		 * "I'm not sure if you are suppose to fight her, but if you do here are the"
		 * +
		 * "strategies. The game's Bonus menu lists her stats so I'm assuming that she is "
		 * +
		 * "a boss, but I never really fought her before. This battle is basically the "
		 * +
		 * "same as above except this time you will have to use Fire attacks obviously "
		 * +
		 * "and equip the FLAME SABRE in this battle. After you defeat her Ifrit will "
		 * +
		 * "come in and realize that you hold Ramuh's powers. But here are 100 potatoes."
		 * ;
		 * 
		 * // create an empty Annotation just with the given text Annotation
		 * document = new Annotation(text);
		 * 
		 * // run all Annotators on this text customPipeline.annotate(document);
		 * 
		 * // these are all the sentences in this document // a CoreMap is
		 * essentially a Map that uses class objects as keys and // has values
		 * with custom types List<CoreMap> sentences =
		 * document.get(SentencesAnnotation.class);
		 * 
		 * List<String> words = new ArrayList<>(); List<String>
		 * appearedStopwords = new ArrayList<>(); for (CoreMap sentence :
		 * sentences) { // traversing the words in the current sentence // a
		 * CoreLabel is a CoreMap with additional token-specific methods for
		 * (CoreLabel token : sentence.get(TokensAnnotation.class)) { // this is
		 * the text of the token
		 * 
		 * String word = token.get(TextAnnotation.class).toLowerCase();
		 * 
		 * if (stopWords.contains(word) || word.equals(",")) {
		 * appearedStopwords.add(word); } else {
		 * words.add(token.get(LemmaAnnotation.class).toLowerCase()); } // this
		 * is the POS tag of the token String pos =
		 * token.get(PartOfSpeechAnnotation.class); // this is the NER label of
		 * the token String ne = token.get(NamedEntityTagAnnotation.class); }
		 * 
		 * // this is the parse tree of the current sentence // Tree tree =
		 * sentence.get(TreeAnnotation.class); // System.out.println(tree); //
		 * this is the Stanford dependency graph of the current sentence //
		 * SemanticGraph dependencies = sentence //
		 * .get(CollapsedCCProcessedDependenciesAnnotation.class); }
		 * System.out.println("List of words below:"); for (String word : words)
		 * { System.out.println(word); }
		 * System.out.println("List of stop words below:"); for (String word :
		 * appearedStopwords) { System.out.println(word); }
		 * 
		 * // This is the coreference link graph // Each chain stores a set of
		 * mentions that link to each other, // along with a method for getting
		 * the most representative mention // Both sentence and token offsets
		 * start at 1! Map<Integer, CorefChain> graph = document
		 * .get(CorefChainAnnotation.class);
		 */
	}

	public static StanfordNLPProcessing getInstance() {
		if (instance == null) {
			instance = new StanfordNLPProcessing();
		}
		return instance;
	}

	public void findPosTag() {
		MaxentTagger tagger = new MaxentTagger(
				"dictionary/pos-tagger/english-left3words/english-left3words-distsim.tagger");
		// The sample string

		String sample = "love the Nrws";

		// The tagged string

		String tagged = tagger.tagString(sample);

		// Output the result

		System.out.println(tagged);
	}

	public int findMainSentiment_sentence(String text,boolean normalize) {
		Annotation annotation = customPipeline.process(text);
		int mainSentiment = 0; // range from 0-4

		for (CoreMap sentence : annotation
				.get(CoreAnnotations.SentencesAnnotation.class)) {
			Tree tree = sentence
					.get(SentimentCoreAnnotations.AnnotatedTree.class);
			mainSentiment = RNNCoreAnnotations.getPredictedClass(tree);
		}
		if(normalize){// normalize so it range from 0-2
					// 0 means neutral, 1-2 means it has sentiment
			if(mainSentiment ==2)
				mainSentiment = 0;
			if(mainSentiment > 2)
				mainSentiment = mainSentiment-2;
		}
		return mainSentiment;

	}

	public static String normalizePhrase(Tree input) {
		StringBuilder fullPhrase = new StringBuilder();
		List<Tree> leaves = input.getLeaves();
		for (Tree leaf : leaves) {
			fullPhrase.append(leaf.nodeString() + " ");
		}

		String str = fullPhrase.deleteCharAt(fullPhrase.length() - 1)
				.toString();
		// lemmatize and remove stopwords
		// List<String> words = tokenize(fullPhrase.toString());
		// fullPhrase = new StringBuilder();
		// for (String word : words) {
		// if (word.length() > 1) // dont care about single char
		// fullPhrase.append(word + " ");
		// }
		return str;
	}

	private List<CoreMap> extractSentences(String text) {
		text = text.toLowerCase();
		Annotation document = new Annotation(
				Util.ReplaceNonInterestingChars(text));
		// run all Annotators on this text
		customPipeline.annotate(document);
		List<CoreMap> sentences = document
				.get(CoreAnnotations.SentencesAnnotation.class);
		return sentences;

	}

	public Set<String> getDescriptionVocabulary(String descText) {
		Set<String> vocabulary = new HashSet<String>();
		descText = descText.toLowerCase();
		List<CoreMap> sentences = extractSentences(descText);
		int option = 1;

		Set<String> postagForVoc = new HashSet<>();
		if (option == 1) {
			postagForVoc.addAll(POSTAG_OF_NOUN);
			postagForVoc.addAll(POSTAG_OF_VERB);
		}
		for (CoreMap sentence : sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			String stc = Util.StripDot(sentence.toString().toLowerCase());
			if (stc.length() > 3) {
				Tree tree = sentence
						.get(SentimentCoreAnnotations.AnnotatedTree.class);
				String normSentence = normalizePhrase(tree);

				List<CoreMap> normSentences = extractSentences(normSentence);
				if (normSentences.size() > 0) {
					tree = normSentences.get(0)
							.get(SentimentCoreAnnotations.AnnotatedTree.class);

					vocabulary.addAll(
							travelVocabulary(tree, option, postagForVoc));
				}
				// tree.pennPrint();
			}
		}
		return vocabulary;
	}

	private Set<String> travelVocabulary(Tree input, int option,
			Set<String> postagForVoc) {
		Set<String> voc = new HashSet<>();

		if (input == null)
			return voc;
		if (input.isLeaf())
			return voc;
		List<Tree> children = input.getChildrenAsList();
		for (Tree child : children) {
			if (child.isPreTerminal()) {
				if (postagForVoc.contains(child.label().value()))
					voc.add(child.getLeaves().get(0).nodeString());
			} else {
				voc.addAll(travelVocabulary(child, option, postagForVoc));
			}
		}
		return voc;
	}

	public int[] getPhrasalTokens(String text, PrintWriter outputReviews) {
		text = text.toLowerCase();

		int[] sentCount = new int[5];
		List<CoreMap> sentences = extractSentences(text);

		for (CoreMap sentence : sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			String stc = sentence.toString().toLowerCase();
			if (stc.length() > 3) {
				Tree tree = sentence
						.get(SentimentCoreAnnotations.AnnotatedTree.class);
				int[] res = travelForPhrasalVocabulary(tree, outputReviews);
				for (int i = 0; i < res.length; i++) {
					sentCount[i] += res[i];
				}
				// vocabulary.addAll(travelForPhrasalVocabulary(tree));
				// tree.pennPrint();
			}
		}
		return sentCount;
	}

	public List<String> getPhrasalTokens(String text) {
		List<String> voc = new ArrayList<>();
		text = text.toLowerCase();
		List<CoreMap> sentences = extractSentences(text);

		for (CoreMap sentence : sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			String stc = sentence.toString().toLowerCase();
			if (stc.length() > 3) {
				Tree tree = sentence
						.get(SentimentCoreAnnotations.AnnotatedTree.class);
				voc.addAll(travelForPhrasalVocabulary(tree));
				// vocabulary.addAll(travelForPhrasalVocabulary(tree));
				// tree.pennPrint();
			}
		}
		return voc;
	}

	public List<String> splitSentencesAndNormalize(String text) {
		List<String> normalizedSentences = new ArrayList<String>();
		List<CoreMap> sentences = extractSentences(text);
		for (CoreMap sentence : sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			StringBuilder normSentence = new StringBuilder();
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				// this is the text of the token
				// get the stopword annotation
				if (token.get(TextAnnotation.class).length() > 1) {
					String word = token.get(LemmaAnnotation.class)
							.toLowerCase();
					try {
						Integer.parseInt(word);
					} catch (NumberFormatException e) {
						if (/*!stopWords.contains(word) && */!word.equals(","))
							normSentence.append(word + " ");
					}
				}
			}
			normalizedSentences.add(normSentence.toString());
		}
		return normalizedSentences;
	}

	private int[] travelSentimentCombineAndCompare(Tree input,
			PrintWriter outputReviews) {
		int[] sentCount = new int[5];
		int[] sentCombine = new int[5];
		if (input == null)
			return sentCount;
		String postag = input.label().value();
		List<Tree> leaves = input.getLeaves();
		String phrase = normalizePhrase(input);
		if (phrase.length() > 0) {
			// voc.add(phrase);

			int mainSentiment = RNNCoreAnnotations.getPredictedClass(input);
			outputReviews.print("<" + phrase + " = " + mainSentiment + ">");
			sentCount[mainSentiment]++;
			// combine
			for (Tree node : input.getChildrenAsList()) {
				if (!node.isLeaf()) {
					int[] res = travelSentimentCombineAndCompare(node,
							outputReviews);
					for (int i = 0; i < res.length; i++) {
						sentCombine[i] += res[i];
					}
				}
			}
			// compare
			int absMain = Math.abs(mainSentiment - 2);
			for (int i = 0; i < sentCombine.length; i++) {
				if (sentCombine[i] > 0)
					if (absMain < Math.abs(i - 2))
						return sentCombine;
			}
			return sentCount;
		}
		return sentCount;
	}

	private List<String> travelForPhrasalVocabulary(Tree input) {
		List<String> voc = new ArrayList<>();
		List<Tree> leaves = input.getLeaves();
		if (input.isPhrasal() && leaves.size() <= PHRASE_MAX_SIZE/* 20*/) {
			String phrase = normalizePhrase(input);
			if (phrase.length() > 0) {
				voc.add(phrase);
			}
		}
		for (Tree node : input.getChildrenAsList()) {
			if (!node.isLeaf()) {
				voc.addAll(travelForPhrasalVocabulary(node));
			}
		}
		return voc;
	}
	public Tree getParseTree(String text) {

		List<CoreMap> sentences = extractSentences(text);
		for (CoreMap sentence : sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			String stc = sentence.toString().toLowerCase();
			if (stc.length() > 3) {
				return sentence
						.get(SentimentCoreAnnotations.AnnotatedTree.class);
			}
		}
		return null;
	}


	public String expandPhrase(String text, String phrase) {
		
		List<CoreMap> sentences = extractSentences(text);
		String result = null;
		for (CoreMap sentence : sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			String stc = sentence.toString().toLowerCase();
			if (stc.length() > 3) {
				Tree tree = sentence
						.get(SentimentCoreAnnotations.AnnotatedTree.class);
				result = travelForExpandingPhrase(tree, phrase, null);
				// vocabulary.addAll(travelForPhrasalVocabulary(tree));
				// tree.pennPrint();
			}
		}
		if(result == null)
			result = text;
		return result.toString();
	}

	private String travelForExpandingPhrase(Tree input, String phrase,
			String intermediate) {
		String str = normalizePhrase(input);
		String result = intermediate;
		if (str.contains(phrase)) {
			if (input.isPhrasal()) {
				if (intermediate == null
						|| intermediate.length() > str.length()) {
					result = str;
				}

			}
			for (Tree node : input.getChildrenAsList()) {
				if (!node.isLeaf()) {
					result = travelForExpandingPhrase(node, phrase, result);
				}
			}
		}
		return result;
	}

	private int[] travelForPhrasalVocabulary(Tree input,
			PrintWriter outputReviews) {
		// List<String> voc = new ArrayList<>();
		int[] sentCount = new int[5];
		if (input == null)
			return sentCount;
		String postag = input.label().value();

		List<Tree> leaves = input.getLeaves();
		if (input.isPhrasal() && leaves.size() < PHRASE_MAX_SIZE/* 10*/) {
			String phrase = normalizePhrase(input);
			if (phrase.length() > 0) {
				// voc.add(phrase);
				sentCount = travelSentimentCombineAndCompare(input,
						outputReviews);
				// int mainSentiment =
				// RNNCoreAnnotations.getPredictedClass(input);
				// outputReviews.print("<"+phrase+" = "+mainSentiment+">");
				// sentCount[mainSentiment]++;
				outputReviews.print("[" + sentCount[0] + " " + sentCount[1]
						+ " " + sentCount[2] + " " + sentCount[3] + " "
						+ sentCount[4] + "]");
				return sentCount;
			}
		} else if ((POSTAG_OF_VOCABULARY.contains(postag))
				&& input.isPreTerminal()) {
			String phrase = normalizePhrase(input);
			if (phrase.length() > 0) {
				// voc.add(phrase);
				int mainSentiment = RNNCoreAnnotations.getPredictedClass(input);
				// System.out.println(mainSentiment);
				outputReviews.print("<" + phrase + " = " + mainSentiment + ">");
				sentCount[mainSentiment]++;
				return sentCount;
			}
		}
		for (Tree node : input.getChildrenAsList()) {
			if (!node.isLeaf()) {
				int[] res = travelForPhrasalVocabulary(node, outputReviews);
				for (int i = 0; i < res.length; i++) {
					sentCount[i] += res[i];
				}
			}
		}
		return sentCount;
	}

	public List<String> tokenize(String text) {
		// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);

		// run all Annotators on this text
		customPipeline.annotate(document);

		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and
		// has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

		List<String> words = new ArrayList<>();
		// List<String> appearedStopwords = new ArrayList<>();
		for (CoreMap sentence : sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				// this is the text of the token
				// get the stopword annotation

				if (token.get(TextAnnotation.class).length() > 1) {
					String word = token.get(LemmaAnnotation.class)
							.toLowerCase();
					try {
						Integer.parseInt(word);
					} catch (NumberFormatException e) {
						if (/*!stopWords.contains(word) &&*/ !word.equals(","))
							words.add(word);
					}
				}
			}
		}
		/*
		 * System.out.println("List of words below:"); for (String word : words)
		 * { System.out.println(word); }
		 */
		return words;
	}

	public List<String> simplerTokenize(String text) {

		List<String> words = new ArrayList<>();
		StringTokenizer tokenizer2 = new StringTokenizer(text);
		while (tokenizer2.hasMoreTokens()) {
			words.add(tokenizer2.nextToken().toLowerCase());
		}
		return words;
	}

	public List<String> splitTokenize(String text) {

		String[] wordArray = text.toLowerCase().split("[a-z]+");

		return Arrays.asList(wordArray);
	}
}
