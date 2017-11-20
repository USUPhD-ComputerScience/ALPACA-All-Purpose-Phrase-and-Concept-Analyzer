package USU.EDU.ConceptAnalysisBackend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.TimeZone;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import Utils.Util;

public class prepareSURFReviews {
	private static String DIRECTORY = "D:/projects/ALPACA/SURFreplicationDataset/";

	public static class SaxHandler extends DefaultHandler {
		boolean btext = false;
		boolean btitle = false;
		String review_text = "";
		String review_title = "";
		static int counter = 0;
		PrintWriter pwMeta = null;

		public SaxHandler(PrintWriter metapw) throws IOException {
			pwMeta = metapw;
		}

		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {

			// System.out.println("Start Element :" + qName);

			if (qName.equalsIgnoreCase("review_text")) {
				btext = true;
			}
			if (qName.equalsIgnoreCase("review_title")) {
				btitle = true;
			}
		}

		public void endElement(String uri, String localName, String qName)
				throws SAXException {

			// System.out.println("End Element :" + qName);
			if (btext) {
				btext = false;
				try {
					writeContentToFile();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				review_text = "";
				review_title = "";
				counter++;
			}
			if (btitle) {
				btitle = false;
			}
		}

		public void characters(char ch[], int start, int length)
				throws SAXException {

			if (btext) {
				review_text += new String(ch, start, length);
			}
			if (btitle) {
				review_title += new String(ch, start, length);
			}

		}

		public void writeContentToFile() throws FileNotFoundException {
			String directoryRawtext = DIRECTORY + "rawdata/";
			String meta = "\"" + counter + ".txt\",\"\",\"\",\"\"";
			pwMeta.println(meta);

			PrintWriter pwraw = new PrintWriter(
					new File(directoryRawtext + counter + ".txt"));
			if(!review_title.equals(""))
				pwraw.print(review_title+". ");
			pwraw.print(review_text);
			pwraw.close();
		}
	}

	public static void main(String[] args)
			throws IOException, ParserConfigurationException, SAXException {
		// CSVReader csvreader = new CSVReader(
		// new FileReader("D:/projects/ALPACA/securityCVE/allitems.csv"),
		// ',', CSVWriter.DEFAULT_ESCAPE_CHARACTER);
		PrintWriter pwMeta = new PrintWriter(
				new FileWriter(DIRECTORY + "metadata.csv", false));
		pwMeta.println(
				"\"SURF data\",\"This is the review dataset from SURF replication package \",\"false\",\"false\",\"false\",\"no other metadata\"");

		List<String> fileList = Util
				.listFilesForFolder(DIRECTORY + "XMLreviews/");
		for (String fileName : fileList) {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			InputStream xmlInput = new FileInputStream(fileName);

			SAXParser saxParser = factory.newSAXParser();
			SaxHandler handler = new SaxHandler(pwMeta);
			saxParser.parse(xmlInput, handler);
		}
		pwMeta.close();
	}
}
