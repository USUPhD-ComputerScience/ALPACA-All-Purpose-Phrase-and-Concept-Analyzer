package USU.EDU.ConceptAnalysisBackend;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

public class PrepareGuzmanReviews {
	public static String destinationDir = "D:/projects/ALPACA/GuzmanREVIEWS/";

	private static void readAndWriteTrGuzmanData()
			throws FileNotFoundException {
		Scanner scn = new Scanner(
				new File("D:/projects/featuremining/truthsetEmitzaGuzmen.tsv"));
		File directory = new File(destinationDir);
		if (!directory.exists()) {
			directory.mkdirs();
			// If you require it to make the entire directory path including
			// parents,
			// use directory.mkdirs(); here instead.
		}
		String directoryReview = destinationDir + "rawData/";
		directory = new File(directoryReview);
		if (!directory.exists()) {
			directory.mkdirs();
			// If you require it to make the entire directory path including
			// parents,
			// use directory.mkdirs(); here instead.
		}

		PrintWriter pwMeta = new PrintWriter(
				new File(destinationDir + "metadata.txt"));
		pwMeta.println("\"Emitza Gutzman\",\"This is the dataset for DECA\",\"true\",\"false\",\"false\",\"no other metadata\"");
		int counter = 0;
		while (scn.hasNextLine()) {
			String line = scn.nextLine();
			String[] values = line.split("\t");
			int rating = Integer.parseInt(values[4]);
			String meta = "\"" + counter + ".txt\",\""
					+ rating + "\",\"no timestamp data\"";
			pwMeta.println(meta);
			PrintWriter pwRev = new PrintWriter(
					new File(directoryReview + counter + ".txt"));
			pwRev.println(values[5]);
			pwRev.close();
			counter++;
		}
		pwMeta.close();
		scn.close();
	}
	public static void main(String[] args) throws FileNotFoundException {
		readAndWriteTrGuzmanData();
	}
}
