package MainTasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
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

		processDBData("D:/projects/ALPACA/NSF/",
				PreprocesorMain.LV2_ROOTWORD_STEMMING);
	}

	public static void processDBData(String directory, int level)
			throws Exception {
		Dataset data = readRawData(directory,
				PreprocesorMain.LV2_ROOTWORD_STEMMING);
		long start = System.currentTimeMillis();
		int count = 0;
		int englishCount = 0;
		// read documents for this dataset
		System.out.println(">> Querying raw documents...");
		Vocabulary voc = data.getVocabulary();
		for (Document doc : data.getDocumentSet()) {
			doc.setLevel(level);
			// preprocessing part
			boolean isEnglish = doc.preprocess(level, directory, voc);
			count++;
			if (isEnglish) {
				englishCount++;
				PrintWriter csvwrt = new PrintWriter(
						new FileWriter(directory + "preprocessed_LV" + level
								+ "//" + doc.getRawTextFileName()));
				csvwrt.println(doc.toString(false, voc));
				csvwrt.println(doc.toPOSString(voc));
				csvwrt.close();
				if (count % 100 == 0)
					System.out.println(">> processed " + count + " documents ("
							+ englishCount + "/" + englishCount
							+ " is English)");
			}

		}

		System.out.println(">> processed " + count + " documents ("
				+ englishCount + "/" + count + " is English)");
		System.out.println("Writing data to database..");
		FileDataAdapter.getInstance().writeCleansedText(data, level);
		voc.writeToDB();
		System.out.println(" Done! Preprocessing took "
				+ (double) (System.currentTimeMillis() - start) / 1000 / 60
				+ "minutes");
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
