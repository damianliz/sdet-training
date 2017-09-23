package exceptionsfiles;

public class CommonExceptions {

	public static void main(String[] args) {
		int a = 5;
		int b = 0;

		// 1. Identify the potential problem area
		// 2. Surround with try-catch block
		try {
			int c = a / b;

		} catch (ArithmeticException e) {
			System.out.println("CANNOT DIVIDE BY ZERO : " + e.getMessage());
		}

		String[] states = { "CA", "TX", "FL", "NY" };

		for (int i = 0; i < states.length; i++) {
			System.out.println("State : " + states[i]);
		}
		System.out.println("Program is closing.");

	}

}
