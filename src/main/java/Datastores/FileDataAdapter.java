package Datastores;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import MainTasks.PreprocesorMain;
import Vocabulary.DBWord;
import Vocabulary.Vocabulary;

public class FileDataAdapter {
	// write keywords data to file in byte array
	// main file:
	// 1 byte for flag, 1 byte for text length, 28 bytes for text (14
	// chars max UTF-16LE), 4 bytes for Count,1 bytes for POS
	// Big word syntax: for words longer than 14 chars, the 35 bytes is
	// converted as:
	// 0x00000001<int 4 bytes><int 4 bytes><21 random bytes><int 4
	// count><1 POS byte> which
	// has the location of the long word in optional file and
	// its length.
	// optional file:just bytestream of all long words

	// how to read:
	// main file:
	// 1. calculate the reading pointer by multiplying id with 35. i.e. id = 0
	// -> pointer at 0, id= 1 -> pointer at 38
	// 2. read first 2 bytes to determine if it is a * (big word) or - (normal
	// word)
	// 3. for normal word, read next 28 bytes coded in UTF-16 -> cut the space
	// to get real word
	// 4. for big word, read next 4 bytes as int to get location, read next 4
	// bytes as in to get length -> read optional file
	// 5. after reading text, read next 8 bytes for POS in UTF-16 -> cut the
	// space to get real POS

	// ======================
	// write hash table object to file by serializing
	// hash table consists of hash and its word locations on main file.
	// at beginning the system will read this entire hash table to RAM
	// search flow: word -> location
	// use hash bimap from Guava

	// =======================
	// write cleansed text to file with corresponding file name to rawfile
	// the cleansed text are int[][] array, written to file by serializing

	// -========
	// all writing operation must be override, not expansion. And must be
	// completed.
	// if the processing function has done its business, write a flag file to
	// true (initialize it with false at first)
	// any attemp on accessing a data folder must has checkFlag() to check if
	// the data folder was completed
	private static final int MAX_WORD_LENGTH = 14;
	private static final int MAX_STORING_SIZE = 35;
	private static final byte NORMAL_FLAG = 0x00000000;
	private static final byte BIGW_FLAG = 0x00000001;
	private static final String KEYWORD_SUBDIR = "keywordDB/";
	public static final String CLEANSED_SUBDIR = "cleansedText/";
	private byte[] spaceBytes;
	private static FileDataAdapter instance = null;

	public static FileDataAdapter getInstance()
			throws UnsupportedEncodingException {
		if (instance == null)
			instance = new FileDataAdapter();
		return instance;
	}

	private FileDataAdapter() throws UnsupportedEncodingException {
		// TODO Auto-generated constructor stub
		spaceBytes = " ".getBytes("UTF-16LE");
	}

	/*
	 * write cleansed text to file with corresponding file name to rawfile the 
	 * cleansed text are int[][] array, written to file by serializing it.
	 * */
	public void writeCleansedText(Dataset dataset, int level)
			throws IOException {
		String cleansedTextLocationDir = getLevelLocationDir(CLEANSED_SUBDIR,
				dataset.getDirectory(), level);
		File fDirectory = new File(cleansedTextLocationDir);
		if (!fDirectory.exists()) {
			fDirectory.mkdirs();
			// If you require it to make the entire directory path including
			// parents,
			// use directory.mkdirs(); here instead.
		}
		setCompletionFlag(cleansedTextLocationDir, false);
		PrintWriter pwt = new PrintWriter(
				new File(cleansedTextLocationDir + "metadata.csv"));
		pwt.println("\"" + dataset.getName() + "\",\""
				+ dataset.getDescription() + "\",\"" + dataset.hasRating()
				+ "\",\"" + dataset.hasTime() + "\",\"" + dataset.hasAuthor()
				+ "\",\"" + dataset.getOtherMetadata() + "\"");
		for (Document doc : dataset.getDocumentSet()) {
			FileOutputStream fos = new FileOutputStream(
					cleansedTextLocationDir + doc.getRawTextFileName());
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(doc.getSentences());
			oos.close();
			pwt.println("\"" + doc.getRawTextFileName() + "\",\""
					+ doc.getRating() + "\",\"" + doc.getTime() + "\",\""
					+ doc.getAuthorFileName() + "\",\"" + doc.isEnglish()
					+ "\"");
		}
		pwt.close();
		setCompletionFlag(cleansedTextLocationDir, true);
	}

	public void readCleansedText(Document doc, Dataset dataset, int level)
			throws IOException, ClassNotFoundException {
		String cleansedTextLocationDir = getLevelLocationDir(CLEANSED_SUBDIR,
				dataset.getDirectory(), level);
		if (!isCompleted(cleansedTextLocationDir))
			throw new IOException(
					"Data is corrupted or not completely processed.");
		FileInputStream fis = new FileInputStream(
				cleansedTextLocationDir + doc.getRawTextFileName());
		ObjectInputStream iis = new ObjectInputStream(fis);
		int[][] cleansedText = (int[][]) iis.readObject();
		doc.setSentences(cleansedText);
		iis.close();
	}

	/*
	 * Directory is the general folder of this dataset/project
	 * */
	public void writeVoc(List<DBWord> wordSet, String directoryName, int level)
			throws IOException {
		String keywordLocationDir = getLevelLocationDir(KEYWORD_SUBDIR,
				directoryName, level);
		int bigWFileCurrentLocation = 0;
		File fDirectory = new File(keywordLocationDir);
		if (!fDirectory.exists()) {
			fDirectory.mkdirs();
			// If you require it to make the entire directory path including
			// parents,
			// use directory.mkdirs(); here instead.
		}
		// set flag file to false first, this flag will remain false until all
		// operation is finished, preventing the reading of unfinised processed
		// data
		setCompletionFlag(keywordLocationDir, false);
		// start writing keywords into two files
		FileOutputStream mainFile = new FileOutputStream(
				keywordLocationDir + "mainVoc.dat");
		FileOutputStream additionalFile = new FileOutputStream(
				keywordLocationDir + "longVoc.dat");
		try {
			for (DBWord word : wordSet) {
				// get the bytes and write them in files, also update location
				// value
				// of the big word file if needed
				byte[] storingBytes = getStoringBytes(word,
						bigWFileCurrentLocation);
				mainFile.write(storingBytes);
				if (storingBytes[0] == BIGW_FLAG) {
					// write additionally (I love these word plays)
					byte[] wordBytes = word.getText().getBytes("UTF-16LE");
					additionalFile.write(wordBytes);
					bigWFileCurrentLocation += wordBytes.length;
				}
			}
		} finally {
			mainFile.close();
			additionalFile.close();
		}
		// set flag file to true
		setCompletionFlag(keywordLocationDir, true);
	}

	public List<DBWord> getVoc(String directoryName, int level)
			throws IOException {
		// setup the directory
		String keywordLocationDir = getLevelLocationDir(KEYWORD_SUBDIR,
				directoryName, level);
		List<DBWord> voc = null;
		if (isCompleted(keywordLocationDir)) {
			voc = new ArrayList<>();
			File vocFile = new File(keywordLocationDir + "mainVoc.dat");
			int totalVocSize = (int) (vocFile.length() / MAX_STORING_SIZE);
			for (int id = 0; id < totalVocSize; id++) {
				DBWord word = null;
				word = readWord(keywordLocationDir, id);
				voc.add(word);
			}
		} else
			throw new IOException(
					"Data is corrupted or not completely processed.");
		return voc;
	}

	private DBWord readWord(String keywordLocationDir, int id)
			throws FileNotFoundException, IOException,
			UnsupportedEncodingException {
		DBWord word;
		// get location on file
		long wordLocation = id * MAX_STORING_SIZE;
		RandomAccessFile raf1 = new RandomAccessFile(
				keywordLocationDir + "mainVoc.dat", "r");
		raf1.seek(wordLocation);
		byte[] wordInfoBytes = new byte[MAX_STORING_SIZE];
		raf1.readFully(wordInfoBytes);
		if (wordInfoBytes[0] == BIGW_FLAG) {
			// read as big word
			// byte[] textBytes = new byte[textLength];
			// mergeByteArrays(textBytes, wordInfoBytes, 2, textLength);
			// String text = new String(textBytes);

			byte[] longTextLocationBytes = new byte[4];
			mergeByteArrays(longTextLocationBytes, wordInfoBytes, 0, 1, 4);
			long longTextLocation = ByteBuffer.wrap(longTextLocationBytes)
					.getInt();
			byte[] longTextSizeBytes = new byte[4];
			mergeByteArrays(longTextSizeBytes, wordInfoBytes, 0, 5, 4);
			int longTextSize = ByteBuffer.wrap(longTextSizeBytes).getInt();
			// read from the additional file
			RandomAccessFile raf2 = new RandomAccessFile(
					keywordLocationDir + "longVoc.dat", "r");
			raf2.seek(longTextLocation);
			byte[] longTextBytes = new byte[longTextSize];
			try {
				raf2.readFully(longTextBytes);
			} catch (EOFException e) {
				System.out.println();
			}
			String text = new String(longTextBytes, "UTF-16LE");
			raf2.close();

			byte[] countBytes = new byte[4];
			mergeByteArrays(countBytes, wordInfoBytes, 0, 30, 4);
			int count = ByteBuffer.wrap(countBytes).getInt();
			word = new DBWord(id, text, wordInfoBytes[MAX_STORING_SIZE - 1],
					count);
		} else {
			// read as small word
			int textLength = wordInfoBytes[1];
			byte[] textBytes = new byte[textLength];
			mergeByteArrays(textBytes, wordInfoBytes, 0, 2, textLength);
			String text = new String(textBytes, "UTF-16LE");
			byte[] countBytes = new byte[4];
			mergeByteArrays(countBytes, wordInfoBytes, 0, 30, 4);
			int count = ByteBuffer.wrap(countBytes).getInt();
			word = new DBWord(id, text, wordInfoBytes[MAX_STORING_SIZE - 1],
					count);
		}
		raf1.close();
		return word;
	}

	public DBWord getWord(int id, String directoryName, int level)
			throws IOException {
		// setup the directory
		String keywordLocationDir = getLevelLocationDir(KEYWORD_SUBDIR,
				directoryName, level);
		DBWord word = null;
		if (isCompleted(keywordLocationDir)) {
			word = readWord(keywordLocationDir, id);
		} else
			throw new IOException(
					"Data is corrupted or not completely processed.");
		return word;
	}

	public static String getLevelLocationDir(String subDir,
			String directoryName, int level) {
		String keywordLocationDir = directoryName + subDir;
		switch (level) {
		case PreprocesorMain.LV1_SPELLING_CORRECTION:
			keywordLocationDir += "lv1/";
			break;
		case PreprocesorMain.LV2_ROOTWORD_STEMMING:
			keywordLocationDir += "lv2/";
			break;
		case PreprocesorMain.LV3_OVER_STEMMING:
			keywordLocationDir += "lv3/";
			break;
		case PreprocesorMain.LV4_ROOTWORD_STEMMING_LITE:
			keywordLocationDir += "lv4/";
			break;
		}
		return keywordLocationDir;
	}

	/*
	 * put word info inside a byte array
	 * */
	private byte[] getStoringBytes(DBWord word, int bigWFileCurrentLocation)
			throws UnsupportedEncodingException {
		String originalTxt = word.getText();
		byte[] originalTextBytes = originalTxt.getBytes("UTF-16LE");
		byte[] storingBytes = new byte[MAX_STORING_SIZE];
		byte[] LVxIntbytes = new byte[4];
		if (originalTxt.length() > MAX_WORD_LENGTH) {
			// store as long word in additional file
			storingBytes[0] = BIGW_FLAG;
			byte[] firstIntbytes = new byte[4];
			byte[] secondIntbytes = new byte[4];
			// BigEndian
			ByteBuffer.wrap(firstIntbytes).putInt(bigWFileCurrentLocation);
			ByteBuffer.wrap(secondIntbytes)
					.putInt(originalTextBytes.length);
			mergeByteArrays(storingBytes, firstIntbytes, 1, 0,
					firstIntbytes.length);
			mergeByteArrays(storingBytes, secondIntbytes, 5, 0,
					secondIntbytes.length);
		} else {
			// store normally
			storingBytes[0] = NORMAL_FLAG;
			storingBytes[1] = (byte) originalTextBytes.length;
			mergeByteArrays(storingBytes, originalTextBytes, 2, 0,
					originalTextBytes.length);
		}
		ByteBuffer.wrap(LVxIntbytes).putInt(word.getCount());
		mergeByteArrays(storingBytes, LVxIntbytes, 30, 0, LVxIntbytes.length);
		storingBytes[MAX_STORING_SIZE - 1] = word.getPOS();

		return storingBytes;
	}

	public static void mergeByteArrays(byte[] destinationArray,
			byte[] originalArray, int startLocationDes, int startLocationOri,
			int length) {
		for (int i = 0; i < length; i++) {
			destinationArray[startLocationDes + i] = originalArray[i
					+ startLocationOri];
		}
	}

	/*
	 * read flag file of this directory to see if it is completed preprocessed 
	 * data
	 */
	private boolean isCompleted(String directoryName) {
		Scanner scn;
		try {
			scn = new Scanner(new File(directoryName + "iscompleted.flag"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			return false;
		}
		String isCompleted = "false";
		if (scn.hasNext())
			isCompleted = scn.nextLine();
		scn.close();
		if (isCompleted.equals("true"))
			return true;
		return false;
	}

	/*
	 *  set flag file to false first, this flag will remain false until all 
	 *  operation is finished, preventing the reading of unfinised processed data
	 */
	private void setCompletionFlag(String directoryName, boolean flag)
			throws FileNotFoundException {
		// making the flag file
		PrintWriter flagFile = new PrintWriter(
				new File(directoryName + "iscompleted.flag"));
		if (flag)
			flagFile.print("true");
		else {
			flagFile.print("false");
		}
		flagFile.close();
	}
}
