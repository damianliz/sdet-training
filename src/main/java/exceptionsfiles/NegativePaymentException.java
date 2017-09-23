package exceptionsfiles;

public class NegativePaymentException extends Exception {

	// Declare class variable
	double payment;

	// 1. Constructor that takes the value that would throw the exception
	// 2. Assign value of local variable to class variable
	public NegativePaymentException(double payment) {
		this.payment = payment;
	}

	// 3. Override the toString() method to include the specific exception message
	public String toString() {
		return "Error : Cannot take negative payment " + payment;
	}

}
