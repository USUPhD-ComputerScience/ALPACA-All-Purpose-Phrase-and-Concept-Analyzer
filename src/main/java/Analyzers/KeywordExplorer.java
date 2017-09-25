package Analyzers;



import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import NLP.NatureLanguageProcessor;
import NLP.WordVec;
import Vocabulary.Vocabulary;

import java.util.Map.Entry;


public class KeywordExplorer {

  public static WordVec word2vec;

	// static Map<String, Double> pairSimiliarity = new HashMap<>();
  public static Set<String> expand(List<String> wordList, WordVec word2vec) throws Throwable {
    KeywordExplorer.word2vec = word2vec;
    System.out.println("Expanding words");
    Set<String> selection = new HashSet<>();
    selection.addAll(wordList);
    autoExploreKeyWords(selection, 0.7);
    System.out.println(selection.size());
    System.out.println("Done!");
    for(String w : wordList){
      if(selection.contains(w))
        selection.remove(w);
    }
    return selection;
  }

  public static double cosineSimilarity(String word1, String word2) {
    //String pair = word1 + "_" + word2;
		// Double sim = pairSimiliarity.get(pair);
    // if (sim == null) {
    Double sim = word2vec.cosineSimilarityForWords(word1, word2, true);
		// pairSimiliarity.put(pair, sim);
    // }
    return sim;
  }

  public static void autoExploreKeyWords(Set<String> selection, double threshold) throws Throwable {
    System.out.println(">>Start expanding!");
    int count = 0;
    while (true) {
      count++;
      List<String> results = findTopSimilarWords(selection, 10);
      System.out.println(results.toString());
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
    System.out.println(">> done with " + count + " iterations.");

  }

  private static double avgSimilarity(Collection<String> selection) {
    double totalSim = 0;
    int count = 0;
    for (String word1 : selection) {
      for (String word2 : selection) {
        if (word1 != word2) {
          totalSim += cosineSimilarity(word1, word2);
          count++;
        }
      }
    }
    return totalSim / count;
  }

  private static List<String> findTopSimilarWords(Set<String> selection,
          int top) {
    String[] words = new String[top];
    double[] cosineDistance = new double[top];
    NatureLanguageProcessor ntlins = NatureLanguageProcessor.getInstance();
    for (Entry<String, float[]> entry : word2vec.getWordVector().entrySet()) {
      String word2 = entry.getKey();
      if (selection.contains(word2)) {
        continue;
      }
      if (ntlins.getStopWordSet()
              .contains(word2)) {
        continue;
      }
      double result = cosineSimilarityForWords(selection, word2);
      for (int i = 0; i < top; i++) {
        
          //System.out.println(" for (int i = 0; i < top; i++) {" + result + " - " + cosineDistance[i]);
        if (result > cosineDistance[i]) {
          double lastDistance = cosineDistance[i];
          String lastWord = words[i];
          cosineDistance[i] = result;
          words[i] = word2.intern();
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
          //System.out.println(" currentWord = lastWord;->" + words[i]);
          //System.out.println(" currentWord = lastWord;->" + currentWord);
          }
          break;
        } else {
          continue;
        }
      }
    }
    List<String> results = new ArrayList<>();
    for (int i = 0; i < top; i++) {
      results.add(words[i]);
    }
    return results;
  }

  public static double cosineSimilarityForWords(Set<String> words,
          String word2) {
    double score = 1.0;
    for (String word1 : words) {
      score *= (1.0 - cosineSimilarity(word1, word2));
    }
    return 1.0 - score;
  }

}
