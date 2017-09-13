package common.integration;

public class MVCPatternDemo {

	public static void main(String[] args) {
		Student model = retrieveStudent();
		StudentView view = new StudentView();
		StudentController controller = new StudentController(model, view);

		controller.updateView();

		controller.setStudentName("Manuel");
		controller.updateView();

	}

	private static Student retrieveStudent() {
		Student student = new Student();
		student.setName("Robert ");
		student.setRollNo("20");
		return student;
	}
}