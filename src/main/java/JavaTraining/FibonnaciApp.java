package JavaTraining;

public class FibonnaciApp {

	public static void main(String[] args) {
		// Fibonnaci number is defined by the sum of the 2 previous fibonnaci numbers
		// fibonacci(0) = 0
		// fibonnaci(1) = 1
		// fibonacci(2) = fib(1) + fib(0) = 1 + 0 = 1
		// fibonnaci(3) = fib(2) + fib(1) = 1 + 1 = 2
		// fibonnaci(4) = fib(3) + fib(2) = 2 + 1 = 3
		// fibonnaci(5) = fib(4) + fib(3) = 3 + 2 = 5
		
		System.out.println(fib(8));

	}

	public static int fib(int n) {

		if (n == 0) {
			return 0;
		} else if (n == 1) {
			return 1;
		}
		return ( (fib(n - 1)) + (fib(n - 2)) );
	}

}
