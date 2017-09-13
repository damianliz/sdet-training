package labs;

public class BankAccountApp {

	public static void main(String[] args) {
		BankAccount acc1 = new BankAccount("645891", 1000);
		BankAccount acc2 = new BankAccount("322457", 2000);
		BankAccount acc3 = new BankAccount("789451", 2500);

		acc1.setName("Jim");
		System.out.println(acc1.getName());
		acc1.makeDeposit(600);
		acc1.makeDeposit(200);
		acc1.payBill(150);
		acc1.accrue();
		System.out.println(acc1.toString());

	}
}

class BankAccount implements IInterest {
	// Properties of bank account
	private static int id = 1000; // Internal ID
	private String accountNumer; // ID + random + 2-digit number + first 2 of SSN
	private static final String routingNumber = "23465454";
	private String name;
	private String ssn;
	private double balance;

	// Constructor
	public BankAccount(String SSN, double initDeposit) {
		balance = initDeposit;
		System.out.println("New Account created.");
		this.ssn = SSN;
		id++;
		setAccountNumber();
	}

	private void setAccountNumber() {
		int random = (int) (Math.random() * 100);

		accountNumer = id + "" + random + ssn.substring(0, 2);
		System.out.println("Acc No : " + accountNumer);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void payBill(double amount) {
		balance = balance - amount;
		System.out.println("Paying Bill :" + amount);
		showBalance();
	}

	public void makeDeposit(double amount) {
		balance = balance + amount;
		System.out.println("Making Deposit : " + amount);
		showBalance();
	}

	public void showBalance() {
		System.out.println("Balance : " + balance);
	}

	public void accrueInterest() {

	}

	@Override
	public void accrue() {
		balance = balance * (1 + rate / 100);
		showBalance();
	}

	@Override
	public String toString() {
		return "[ Name : " + name + " ]\n[Account Number : " + accountNumer + " ]\n" + "[Routing Number : "
				+ routingNumber + " ]\n" + "[Balance :" + balance + " ]";
	}
}
