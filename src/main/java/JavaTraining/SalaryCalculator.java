package JavaTraining;

public class SalaryCalculator {
	public static void main(String[] args) {
		// Let's create a variable to define our carrer

		// Declare a variable
		String carrer;
		System.out.println("Program is starting");

		// Define variable
		carrer = "Software Developer";
		System.out.println("My Carrer: " + carrer);

		// Declare and Define
		int hoursPerWeek = 40;
		int weeksPerYear = 50;
		double rate = 42.50;

		double salary = hoursPerWeek * weeksPerYear * rate;
		System.out.println(
				"My salary as a " + carrer + " at the rate of $" + rate + " per hour is $" + salary + " per Year.");

		// Computer our annual salary
		// rate * hoursPerWeek * weeksPerYear
	}

}
