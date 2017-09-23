package labs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Passwords {

	private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d+");
	private static final Pattern LETTER_PATTERN = Pattern.compile("\\D+");

	public static void main(String[] args) {

		String fileName = "/home/pitt/Documents/Dev/Lab3";
		File file = new File(fileName);
		String keyWord = null;
		String[] passwords = new String[6];

		try {
			BufferedReader br = new BufferedReader(new FileReader(file));

			for (int i = 0; i < passwords.length; i++) {
				passwords[i] = br.readLine();
			}
			br.close();
		} catch (FileNotFoundException e) {
			System.out.println("ERROR : File not found.");
		}

		catch (IOException e) {
			System.out.println("ERROR : Could not read file.");
		}

		// 1. Verify that the password contains a number
		// 2. Verify that the password contains a letter
		// 3. Verify that the password contains a special character [! # @]
		for (int i = 0; i < passwords.length; i++) {
			keyWord = passwords[i];
			Matcher dm = DIGIT_PATTERN.matcher(keyWord);
			Matcher lm = LETTER_PATTERN.matcher(keyWord);
			System.out.println("Password : " + keyWord);
			try {
				if (!dm.find()) {
					throw new NumberException(keyWord);
				}
				if (!lm.find()) {
					throw new LetterException(keyWord);
				}
				if (!keyWord.contains("!") && !keyWord.contains("#") && !keyWord.contains("@")) {
					throw new SpecialCharacterException(keyWord);
				}
			} catch (NumberException e) {
				System.out.println("ERROR : Password must contain at least one Number.");
				System.out.println(e.toString());
			} catch (LetterException e) {
				System.out.println("ERROR : Password must contain at least one Letter.");
				System.out.println(e.toString());
			} catch (SpecialCharacterException e) {
				System.out.println("ERROR : Password must contain at least one Special Character (! # @)");
				System.out.println(e.toString());
			}
		}
	}

}

class NumberException extends Exception {

	String pass;

	public NumberException(String pass) {
		this.pass = pass;
	}

	public String toString() {
		return ("NumberException : [ " + pass + " ]");
	}
}

class LetterException extends Exception {
	String pass;

	public LetterException(String pass) {
		this.pass = pass;
	}

	public String toString() {
		return ("LetterException : [ " + pass + " ]");
	}

}

class SpecialCharacterException extends Exception {
	String pass;

	public SpecialCharacterException(String pass) {
		this.pass = pass;
	}

	public String toString() {
		return ("SpecialCharacterException : [ " + pass + " ]");
	}
}
