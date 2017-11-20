package MainTasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import Datastores.Dataset;
import Datastores.Document;
import Datastores.FileDataAdapter;
import TextNormalizer.TextNormalizer;
import Utils.Util;
import Vocabulary.Vocabulary;

public class PreprocesorMain {

	public static final int LV1_SPELLING_CORRECTION = 1;
	public static final int LV2_ROOTWORD_STEMMING = 2;
	public static final int LV3_OVER_STEMMING = 3;
	public static final int LV4_ROOTWORD_STEMMING_LITE = 4;

	public static void main(String[] args) throws Throwable {
		// TODO Auto-generated method stub
		// processData(1);
		TextNormalizer normalizer = TextNormalizer.getInstance();
		normalizer.readConfigINI(
				"D:\\EclipseWorkspace\\TextNormalizer\\config.INI");

//		processDBData("D:/projects/ALPACA/NSF/",
//				PreprocesorMain.LV2_ROOTWORD_STEMMING);
	}

	public static Dataset processDBData(String directory, int level,
			String additionalTextFile) throws Exception {
		Dataset data = readRawData(directory, level);

		String outputDir = FileDataAdapter.getLevelLocationDir("", directory,
				level);
		File fDirectory = new File(outputDir);
		if (!fDirectory.exists()) {
			fDirectory.mkdirs();
			// If you require it to make the entire directory path including
			// parents,
			// use directory.mkdirs(); here instead.
		}
		long start = System.currentTimeMillis();
		int count = 0;
		int englishCount = 0;
		// read documents for this dataset
		System.out.println(">> Querying raw documents...");
		Vocabulary voc = data.getVocabulary();
		double percentageCompleted = 0, docCompleted = 0;
		int totalDoc = data.getDocumentSet().size();
		//Util.printProgress(percentageCompleted);
		for (Document doc : data.getDocumentSet()) {
			doc.setLevel(level);
			// preprocessing part
			boolean isEnglish = doc.preprocess(level, directory, voc);
			count++;
			if (isEnglish) {
				englishCount++;
				PrintWriter csvwrt = new PrintWriter(new FileWriter(
						outputDir + "//" + doc.getRawTextFileName()));
				csvwrt.println(doc.toString(false, voc));
				csvwrt.println(doc.toPOSString(voc));
				csvwrt.close();
			}
			docCompleted++;
			double newPercentage = Util.round(100*docCompleted/totalDoc, 2);
			if(newPercentage > percentageCompleted){
				percentageCompleted=newPercentage;
				Util.printProgress(percentageCompleted);
			}

		}
		System.out.println();
		System.out.println(">> processed " + count + " documents ("
				+ englishCount + "/" + count + " is English)");
		System.out.println("Writing data to database..");
		FileDataAdapter.getInstance().writeCleansedText(data, level);
		voc.writeToDB();
		writeToTrainingFile(data, additionalTextFile);
		//trainWithWord2Vec(data);
		System.out.println(" Done! Preprocessing took "
				+ (double) (System.currentTimeMillis() - start) / 1000 / 60
				+ "minutes");

		return data;
	}

	public static void writeToTrainingFile(Dataset dataset,
			String additionalFile) throws ClassNotFoundException,
			UnsupportedEncodingException, SQLException, IOException {
		String outputDir = FileDataAdapter.getLevelLocationDir("word2vec",
				dataset.getDirectory(), dataset.getLevel());
		File fDirectory = new File(outputDir);
		if (!fDirectory.exists()) {
			fDirectory.mkdirs();
			// If you require it to make the entire directory path including
			// parents,
			// use directory.mkdirs(); here instead.
		}
		PrintWriter pw = new PrintWriter(
				new FileWriter(new File(outputDir + "trainingText.txt")));
		System.out.println("Writing data to training file at: " + outputDir
				+ "trainingText.txt");
		Vocabulary voc = dataset.getVocabulary();
		for (Document doc : dataset.getDocumentSet()) {
			pw.println(doc.toString(false, voc));
		}
		if (additionalFile != null) {
			Scanner scn = new Scanner(new File(additionalFile));
			while (scn.hasNext())
				pw.println(scn.nextLine().toLowerCase());
			scn.close();
		}
		pw.close();
	}

	public static void trainWithWord2Vec(Dataset dataset) throws FileNotFoundException {
		System.out.println("start training word2vec..");
		System.out.println("loading data..");
		String outputDir = FileDataAdapter.getLevelLocationDir("word2vec",
				dataset.getDirectory(), dataset.getLevel());
		// Strip white space before and after for each line
		SentenceIterator iter = new BasicLineIterator(outputDir + "trainingText.txt");
		// Split on white spaces in the line to get words
		TokenizerFactory t = new DefaultTokenizerFactory();
		/*
		CommonPreprocessor will apply the following regex to each token: [\d\.:,"'\(\)\[\]|/?!;]+
		So, effectively all numbers, punctuation symbols and some special symbols are stripped off.
		Additionally it forces lower case for all tokens.
		*/
		t.setTokenPreProcessor(new CommonPreprocessor());
		Word2Vec vec = new Word2Vec.Builder().minWordFrequency(5).iterations(40)
				.layerSize(200).seed(42).windowSize(8).iterate(iter)
				.tokenizerFactory(t).build();
		vec.fit();
		System.out.println("Writing word vectors to text file....");
		WordVectorSerializer.writeWord2VecModel(vec, outputDir + "vectors.txt");
	}

	public static Dataset readRawData(String directory, int level)
			throws Exception {
		String metaDataFileName = directory + "metadata.csv";
		Dataset dataset = null;
		File fcheckExist = new File(metaDataFileName);
		if (!fcheckExist.exists()) {
			throw new FileNotFoundException(
					"This file can't be found: " + metaDataFileName);
		}
		CSVReader reader = null;
		int count = 0;
		try {
			reader = new CSVReader(new FileReader(metaDataFileName), ',',
					CSVWriter.DEFAULT_ESCAPE_CHARACTER);
			String[] line = reader.readNext(); // read first line to get dataset
												// info
			if (line != null) {

				String name = line[0];
				String description = line[1];
				boolean has_rating = Boolean.parseBoolean(line[2]);
				boolean has_time = Boolean.parseBoolean(line[3]);
				boolean has_author = Boolean.parseBoolean(line[4]);
				String otherMetadata = line[5];
				// add to database, get id back
				dataset = new Dataset(name, description, has_time, has_rating,
						has_author, otherMetadata, directory, level);
				while ((line = reader.readNext()) != null) {
					String rawtext_fileName = line[0];
					int rating = -1;
					if (has_rating)
						rating = Integer.parseInt(line[1]);
					long time = -1;
					if (has_time)
						time = Long.parseLong(line[2]);
					String author = null;
					if (has_author)
						author = line[3];
					if (rawtext_fileName == null)
						throw new Exception("Line " + count
								+ ": no raw text data file, aborting");
					// add to dataset
					Document doc = new Document(rawtext_fileName, rating, time,
							false, author);
					dataset.addDocument(doc);
					count++;
					if (count % 100 == 0)
						System.out.println("read in " + count + " documents");
					if (count % 51000 == 0)
						System.out.println("read in " + count + " documents");
				}
				System.out.println("read in " + count + " documents");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw e;
		} finally {
			if (reader != null)
				reader.close();
		}
		return dataset;
	}

}
