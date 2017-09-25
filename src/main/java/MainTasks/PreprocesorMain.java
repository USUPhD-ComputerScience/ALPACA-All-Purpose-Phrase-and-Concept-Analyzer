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
import Datastores.DocumentDatasetDB;
import TextNormalizer.TextNormalizer;
import Utils.Util;
import Vocabulary.Vocabulary;

public class PreprocesorMain {

	public static void main(String[] args) throws Throwable {
		// TODO Auto-generated method stub
		// processData(1);
		TextNormalizer normalizer = TextNormalizer.getInstance();
		normalizer.readConfigINI(
				"D:\\EclipseWorkspace\\TextNormalizer\\config.INI");
		int datasetID = readRawDataToDB(
				"D:\\projects\\ALPACA\\NSF\\metadata.csv");
		processDBData(datasetID, DocumentDatasetDB.LV2_ROOTWORD_STEMMING,
				"D:\\projects\\ALPACA\\NSF\\", "D:\\projects\\ALPACA\\NSF\\");
	}

	public static void processDBData(int datasetID, int level,
			String outputDirectory, String inputDirectory) throws Exception {

		long start = System.currentTimeMillis();
		int count = 0;
		int englishCount = 0;
		DocumentDatasetDB db = DocumentDatasetDB.getInstance();
		// read documents for this dataset
		System.out.println(">> Querying raw documents...");
		Dataset dataset = db.queryDatasetInfo(datasetID);
		db.queryRawTextForSingleDataset(dataset); // too much memory used
		for (Document doc : dataset.getDocumentSet()) {
			doc.setLevel(level);
			// preprocessing part
			boolean isEnglish = doc.preprocess(level, inputDirectory);
			db.updateCleansedText(doc);

			count++;
			if (isEnglish) {
				englishCount++;
				PrintWriter csvwrt = new PrintWriter(
						new FileWriter(outputDirectory + "preprocessed_LV"+level+"//"
								+ doc.getRawTextFileName()));
				csvwrt.println(doc.toString(false));
				csvwrt.println(doc.toPOSString());
				csvwrt.close();
				if (count % 100 == 0)
					System.out.println(">> processed " + count + " documents ("
							+ englishCount + "/" + englishCount + " is English)");
			}

		}

		System.out.println(">> processed " + count + " documents ("
				+ englishCount + "/" + count + " is English)");
		// db.preprocessRawTextForSingleDataset(dataset, level, csvwrt,
		// withPOS);
		Vocabulary.getInstance().updateCountToDB(level);
		System.out.println(" Done! Preprocessing took "
				+ (double) (System.currentTimeMillis() - start) / 1000 / 60
				+ "minutes");
	}

	public static int readRawDataToDB(String metaDataFileName)
			throws IOException, ClassNotFoundException, SQLException {
		File fcheckExist = new File(metaDataFileName);
		if (!fcheckExist.exists()) {
			throw new FileNotFoundException(
					"This file can't be found: " + metaDataFileName);
		}
		DocumentDatasetDB db = DocumentDatasetDB.getInstance();
		CSVReader reader = null;
		int count = 0;
		int datasetID = -1;
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
				datasetID = db.addNewDataset(name, description, has_rating,
						has_time, has_author, otherMetadata);

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
					// add to database
					db.insertRawData(rawtext_fileName, rating, time, author,
							datasetID);
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
		return datasetID;
	}

}
