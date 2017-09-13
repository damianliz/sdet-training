package JavaTraining;

public class Strings {

	public static void main(String[] args) {
		String bookTitle;
		String wordChoise = "Ring";
		bookTitle = "The Lord of the Rings";

		if (bookTitle.contains(wordChoise)) {
			System.out.println("The book contains the word : " + wordChoise);
		}

		String browser = "Chrome";
		if (browser.equalsIgnoreCase("chrome")) {
			System.out.println("The browser is chrome");
		}

		String firstName = "Tim";
		String lastName = "Short";
		String SSN = "987162168";

		// Print the initials plus last 4 digits of SSN
		System.out.print(firstName.substring(0, 1));
		System.out.print(lastName.substring(0, 1));
		System.out.print(SSN.substring(5));

	}

}
