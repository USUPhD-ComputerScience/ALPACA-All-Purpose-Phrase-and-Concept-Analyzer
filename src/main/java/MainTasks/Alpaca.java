package MainTasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.budhash.cliche.Command;
import com.budhash.cliche.Param;
import com.budhash.cliche.Shell;
import com.budhash.cliche.ShellFactory;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import Analyzers.KeywordAnalyzer;
import Datastores.Dataset;
import Datastores.Document;
import Datastores.FileDataAdapter;
import TextNormalizer.TextNormalizer;

public class Alpaca {
	@Command
	public String help() {
		return "Show you all the help you might need <UNDER CONSTRUCTION>!";
	}

	@Command(description = "type exit to exit Alpaca.")
	public void exit() {
	}

	@Command(description = "preprocessing data", name = "preprocess", abbrev = "prep")
	public void preprocessing(String DataDirectory, int levelOfProcessing) {
		try {
			switch (levelOfProcessing) {
			case PreprocesorMain.LV1_SPELLING_CORRECTION:
			case PreprocesorMain.LV2_ROOTWORD_STEMMING:
			case PreprocesorMain.LV3_OVER_STEMMING:
			case PreprocesorMain.LV4_ROOTWORD_STEMMING_LITE:
				PreprocesorMain.processDBData(DataDirectory, levelOfProcessing);
				break;
			default:
				System.out.println(
						"We don't support this level of preprocessing");
				break;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Command(description = "extract and score keywords", name = "score_keyword", abbrev = "keyw")
	public void analyzeKeyword(String DataDirectory, int levelOfProcessing, boolean isFiveStarReview) {
		Dataset data;
		try {
			switch (levelOfProcessing) {
			case PreprocesorMain.LV1_SPELLING_CORRECTION:
			case PreprocesorMain.LV2_ROOTWORD_STEMMING:
			case PreprocesorMain.LV3_OVER_STEMMING:
			case PreprocesorMain.LV4_ROOTWORD_STEMMING_LITE:
				data = Alpaca.readProcessedData(DataDirectory,
						levelOfProcessing);
				KeywordAnalyzer analyzer = new KeywordAnalyzer();
				analyzer.calculateAndWriteKeywordScore(data,
						levelOfProcessing, 1000, isFiveStarReview);
				break;
			default:
				System.out.println(
						"We don't support this level of preprocessing");
				break;
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Command(description = "setup the config file for text analyzer module", name = "config_file", abbrev = "conf")
	public void readConfigINI(String fileName) {
		TextNormalizer normalizer = TextNormalizer.getInstance();
		try {
			normalizer.readConfigINI(fileName);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.err.println("Cannot find the file specified.");
		}
	}

	public static void main(String[] args) throws IOException {
		ShellFactory
				.createConsoleShell("Alpaca ",
						"Enter '?list' to list all commands", new Alpaca())
				.commandLoop();
	}

	@Command(description = "Add two integers")
	public int addNumbers(
			@Param(name = "augmend", description = "What a fancy word :)") int a,
			@Param(name = "addend") int b) {
		return a + b;
	}

	@Command(name = "cat", abbrev = "", header = "The concatenation is:")
	public String concatenate(String... strings) {
		String result = "";
		for (String s : strings) {
			result += s;
		}
		return result;
	}

	private static Dataset loadDataset(String directory, int level)
			throws Exception {
		Dataset dataset = PreprocesorMain.readRawData(directory, level);
		return dataset;
	}

	public static Dataset readProcessedData(String directory, int level)
			throws Exception {
		String cleansedTextLocationDir = FileDataAdapter.getLevelLocationDir(
				FileDataAdapter.CLEANSED_SUBDIR, directory, level);

		String metaDataFileName = cleansedTextLocationDir + "metadata.csv";

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
					boolean isEnglish = Boolean.parseBoolean(line[4]);
					if (rawtext_fileName == null)
						throw new Exception("Line " + count
								+ ": no raw text data file, aborting");
					// add to dataset
					Document doc = new Document(rawtext_fileName, rating, time,
							isEnglish, author);
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
