package exceptionsfiles;

import java.util.Scanner;

public class PaymentsApp {

	// Take a payment from the user

	public static void main(String[] args) {

		double payment = 0;
		boolean positivePAyment = true;

		// 1. Ask the user for input

		do {
			System.out.print("Enter the payment amount : ");

			// 2. Get the amount and test the value
			Scanner in = new Scanner(System.in);

			// 3. Handle exceptions appropriately
			try {
				payment = in.nextDouble();

				if (payment < 0) {
					// throw error
					throw new NegativePaymentException(payment);
				} else {
					positivePAyment = true;
				}
			} catch (NegativePaymentException e) {
				System.out.println(e.toString());
				System.out.println("Please try again...");
				positivePAyment = false;

			}
		} while (!positivePAyment);

		// 4. Print confirmation
		System.out.println("Thank you for your Payment : $" + payment);

	}

}
