package USU.EDU.ConceptAnalysisBackend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
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

public class prepareCVEDataset {
	private static String DIRECTORY = "D:/projects/ALPACA/securityCVE/";

	public static class SaxHandler extends DefaultHandler {
		boolean bstatus = false;
		boolean bphase = false;
		boolean bdesc = false;
		boolean brefs = false;
		boolean bvotes = false;
		boolean bcomments = false;
		String sname = null;
		String sdate = null;
		String svote = "";
		String svoteType = "";
		String sref = "";
		String scomment = "";
		String sdesc = "";
		String sphase = "";
		String sstatus = "";
		String sraw = "";
		Item tempItem = null;
		int counter = 0;
		PrintWriter pwMeta = null;

		public SaxHandler() throws FileNotFoundException {
			pwMeta = new PrintWriter(new File(DIRECTORY + "metadata.csv"));
			pwMeta.println(
					"\"CVE data\",\"This is the dataset for security\",\"false\",\"true\",\"false\",\"no other metadata\"");
		}

		private Date parseDate(String text) throws ParseException {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy",
					Locale.US);
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			return dateFormat.parse(text);
		}

		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {

			// System.out.println("Start Element :" + qName);

			if (qName.equalsIgnoreCase("item")) {
				sdate = attributes.getValue("seq").substring(0,4);
				sname = attributes.getValue("name");
				tempItem = new Item();
				tempItem.name = sname;
			}
			if (qName.equalsIgnoreCase("status")) {
				bstatus = true;
			}
			if (qName.equalsIgnoreCase("phase")) {
//				sdate = attributes.getValue("date");
//				if (sdate != null) {
//					try {
//						tempItem.time = parseDate(sdate).getTime();
//					} catch (ParseException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}

				bphase = true;
			}
			if (qName.equalsIgnoreCase("desc")) {
				bdesc = true;
			}
			if (brefs) {
				sref += "<" + qName + " source=\""
						+ attributes.getValue("source") + "\"";
				String url = attributes.getValue("url");
				if (url != null) {
					sref += " url = \"" + url + "\"";
				}
				sref += ">";
			}
			if (qName.equalsIgnoreCase("refs")) {
				brefs = true;
			}

			if (bvotes) {
				svote += "<" + qName + " count=\""
						+ attributes.getValue("count") + "\">";
				svoteType = qName;
			}
			if (qName.equalsIgnoreCase("votes")) {
				bvotes = true;
			}

			if (bcomments) {
				scomment += "<" + qName + " voter=\""
						+ attributes.getValue("voter") + "\">";
			}
			if (qName.equalsIgnoreCase("comments")) {
				bcomments = true;
			}

		}

		public void endElement(String uri, String localName, String qName)
				throws SAXException {

			// System.out.println("End Element :" + qName);
			if (bvotes && qName.equals("votes")) {
				tempItem.votes = svote;
				bvotes = false;
				svote = "";
			}
			if (bvotes && !qName.equals("votes"))
				svote += "</" + svoteType + ">";

			if (bcomments && qName.equals("comments")) {
				tempItem.comments = scomment;
				bcomments = false;
				scomment = "";
			}
			if (qName.equals("comment"))
				scomment += "</comment>";

			if (brefs && qName.equals("refs")) {
				tempItem.ref = sref;
				brefs = false;
				sref = "";
			}
			if (qName.equals("ref"))
				sref += "</ref>";

			if (bdesc) {
				tempItem.desc = sdesc;
				sraw += sdesc + "\n";
				bdesc = false;
				sdesc = "";

			}

			if (bphase) {
				tempItem.phase = sphase;
				bphase = false;
				sphase = "";
			}
			if (bstatus) {

				tempItem.status = sstatus;
				bstatus = false;
				sstatus = "";
			}
			if (qName.equals("item")) {
				tempItem.rawtext = sraw;
				try {
					tempItem.writeContentToFile(DIRECTORY, counter, pwMeta);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				counter++;
				if (counter % 1000 == 0)
					System.out.println(counter);
				sraw = "";
				tempItem = null;
			}
			if (qName.equals("cve")) {
				pwMeta.close();
			}
		}

		public void characters(char ch[], int start, int length)
				throws SAXException {

			if (bstatus) {
				sstatus += new String(ch, start, length);
			}

			if (bphase) {
				sphase += new String(ch, start, length);
			}

			if (bdesc) {
				sdesc += new String(ch, start, length);
			}
			if (brefs & !sref.isEmpty()) {
				sref += new String(ch, start, length);
			}
			if (bvotes && !svoteType.isEmpty()) {
				svote += new String(ch, start, length);
				svoteType = "";
			}
			if (bcomments & !scomment.isEmpty()) {
				scomment += new String(ch, start, length);
				sraw += new String(ch, start, length);
			}

		}

	}

	public static class Item {
		String name = "";
		String status = "";
		String phase = "";
		String desc = "";
		String ref = "";
		String votes = "";
		String comments = "";
		String rawtext = "";
		long time;

		public void writeContentToFile(String directory, int counter,
				PrintWriter pwMeta) throws FileNotFoundException {
			String directoryDesc = directory + "desc/";
			String directoryRef = directory + "ref/";
			String directoryRawtext = directory + "rawdata/";
			String directoryPhase = directory + "phase/";
			String directoryComments = directory + "comments/";
			String directoryVotes = directory + "votes/";
			String meta = "\"" + counter + ".txt\",\"" + name + "\",\"" + time
					+ "\",\"" + status + "\"";
			pwMeta.println(meta);

			PrintWriter pwraw = new PrintWriter(
					new File(directoryRawtext + counter + ".txt"));
			pwraw.print(rawtext);
			pwraw.close();

			PrintWriter pwdesc = new PrintWriter(
					new File(directoryDesc + counter + ".txt"));
			pwdesc.print(desc);
			pwdesc.close();

			if (!ref.isEmpty()) {
				PrintWriter pwref = new PrintWriter(
						new File(directoryRef + counter + ".txt"));
				pwref.print(ref);
				pwref.close();
			}

			if (!phase.isEmpty()) {
				PrintWriter pwphase = new PrintWriter(
						new File(directoryPhase + counter + ".txt"));
				pwphase.print(phase);
				pwphase.close();
			}
			if (!comments.isEmpty()) {
				PrintWriter pwcomments = new PrintWriter(
						new File(directoryComments + counter + ".txt"));
				pwcomments.print(comments);
				pwcomments.close();
			}
			if (!votes.isEmpty()) {
				PrintWriter pwvotes = new PrintWriter(
						new File(directoryVotes + counter + ".txt"));
				pwvotes.print(votes);
				pwvotes.close();
			}
		}
	}

	public static void main(String[] args)
			throws IOException, ParserConfigurationException, SAXException {
		// CSVReader csvreader = new CSVReader(
		// new FileReader("D:/projects/ALPACA/securityCVE/allitems.csv"),
		// ',', CSVWriter.DEFAULT_ESCAPE_CHARACTER);
		SAXParserFactory factory = SAXParserFactory.newInstance();
		InputStream xmlInput = new FileInputStream(
				"D:/projects/ALPACA/securityCVE/allitems.xml");

		SAXParser saxParser = factory.newSAXParser();
		SaxHandler handler = new SaxHandler();
		saxParser.parse(xmlInput, handler);

	}
}
