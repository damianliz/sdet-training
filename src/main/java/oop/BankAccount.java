package oop;

public class BankAccount extends BankAccountApp{

	// Define variables
	String accountNumber;
	
	// static >> belongs to the CLASS not the object instance
	// final >> constant often static final
	static final String soutingNumber = "154654";
	String name;
	String ssn;
	String accountType;

	// Constructor definitions: unique methods
	// 1. They are used to define /setup/ initialize properties of an object
	// 2. Constructors are IMPLICITY called upon INSTANTIATION
	// 3. The same name as the class itself
	// 4. Constructors have NO return type
	BankAccount() {
		System.out.println(" NEW ACCOUNT CREATED");
	}

	// Overloading: call same method name with different arguments
	BankAccount(String accountType){
		System.out.print(" NEW ACCOUNT : "+ accountType);
	}

	// Define Method
	void deposit() {

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	void withdraw() {

	}

	void checkBalance() {

	}

	void getStatus() {

	}

	
}
