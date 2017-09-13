package labs;

public class StudentDatabase {

	public static void main(String[] args) {

		Student student1 = new Student("Mario", "0123456789");
		student1.setPhone("28292623");
		student1.setState("Michigan");
		student1.setCity("Milan");
		student1.enroll("Math");
		student1.showCourses();
		student1.enroll("Chemistry");
		student1.showCourses();
		System.out.println(student1.toString());
		System.out.println("\n *** Process terminated ***");
	}

}

class Student {

	private static int ID = 1000;
	private String name;
	private String SSN;
	private String eMail;
	private String userID;
	private String phone;
	private String city;
	private String state;
	private String course;
	private double balance = 0;

	public Student(String name, String SSN) {
		this.name = name;
		this.SSN = SSN;
		ID++;
		eMail = name + ID + "@mailEmaple.com";
		int random = (int) (Math.random() * 10000);
		while (random <= 1000) {
			random = (int) (Math.random() * 10000);
		}
		userID = ID + "" + random + SSN.substring(6);
		System.out.println("   USER ID : " + userID);

	}

	public void showCourses() {
		System.out.println("[ Course : " + course + "]");

	}

	@Override
	public String toString() {
		return "\n -> Printing student info. \n[ Name  : " + name + " ]\n[ Email : " + eMail + " ]\n[ Phone : " + phone
				+ " ]\n[ City  : " + city + " ]\n[ State : " + state + " ]";
	}

	public void checkBalance() {
		System.out.println("Balance : " + balance);
	}

	public void pay(double amount) {
		balance = balance - amount;
		System.out.println("Amount paid : " + amount);
		checkBalance();
	}

	public void enroll(String courseName) {
		course = courseName;

	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getPhone() {
		return phone;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCity(String city) {
		return city;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getState() {
		return state;
	}
}
