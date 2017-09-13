package oop;

public class BankAccountApp {

	public static void main(String[] args) {
		// Creating a new bank account >> think instantiate an object

		BankAccount acc1 = new BankAccount();
		acc1.checkBalance();
		acc1.setName("Neo");

		System.out.println(acc1.toString());
		BankAccount acc2 = new BankAccount("Checking Account");
	}


}
