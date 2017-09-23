package files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

public class PhoneNumberApp {

	public static void main(String[] args) {
		// this will read a text file and will retrieve phone number

		String filename = "/home/pitt/Documents/Dev/PhonesNumber";
		File file = new File(filename);
		String phoneNum = null;
		String[] phoneNums = new String[7];

		try {

			BufferedReader br = new BufferedReader(new FileReader(file));

			for (int i = 0; i < phoneNums.length; i++) {

				phoneNums[i] = br.readLine();
			}
			br.close();
		} catch (FileNotFoundException e) {
			System.out.println("ERROR : File not found.");
		} catch (IOException e) {
			System.out.println("ERROR : Could not read file");
		}

		// Valid phone number
		// 10 digits long
		// Area code cannot start in 0 or 9
		// There can be 911 in the phone
		for (int i = 0; i < phoneNums.length; i++) {

			phoneNum = phoneNums[i];
			try {
				if (phoneNum.length() != 10) {
					throw new TenDigitsException(phoneNum);
				}

				if (phoneNum.substring(0, 1).equals("0") || phoneNum.substring(0, 1).equals("9")) {
					throw new AreaCodeException(phoneNum);
				}

				for (int n = 0; n < phoneNum.length(); n++) {
					if (phoneNum.substring(n, n + 1).equals("9")) {
						if (phoneNum.substring(n + 1, n + 3).equals("11")) {
							throw new EmergencyException(phoneNum);
						}
					}
				}
				System.out.println(phoneNum);

			} catch (TenDigitsException e) {
				System.out.println("Error : Phone number is not 10 digits");
				System.out.println(e.toString());
			} catch (AreaCodeException e) {
				System.out.println("ERROR : Phone number has ivalid area code");
				System.out.println(e.toString());
			} catch (EmergencyException e) {
				System.out.println("ERROR : Invalid 911 sequence found.");
				System.out.println(e.toString());
			}
		}

	}

}

class TenDigitsException extends Exception {
	String number;

	public TenDigitsException(String number) {
		this.number = number;

	}

	public String toString() {
		return ("TenDigitsException: " + number);
	}
}

class AreaCodeException extends Exception {
	String number;

	public AreaCodeException(String number) {
		this.number = number;

	}

	public String toString() {
		return ("AreaCodeException: " + number);
	}
}

class EmergencyException extends Exception {
	String number;

	public EmergencyException(String number) {
		this.number = number;

	}

	public String toString() {
		return ("EmergencyException: " + number);
	}
}
