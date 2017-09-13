package oop;

class Person {
	String name;
	String email;
	String phone;

	void walk() {
		System.out.println(name + " is walking");
	}

	void eMail() {
		System.out.println(email + " eMail");
	}

	void sleep() {
		System.out.println(name + " is sleeping");
	}
}

public class Demo {

	public static void main(String[] args) {

		// Instatiating an Object
		Person person1 = new Person();

		// Define some properties
		person1.name = "Joe";
		person1.sleep();

		Person person2 = new Person();
		person2.name = "Sarah";

		// Abstraction
		person2.walk();

	}

}
