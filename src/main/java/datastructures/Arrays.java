package datastructures;

public class Arrays {

	public static void main(String[] args) {

		String[] alphabet = { "a", "b", "c", "d", "e", "f" };
		for (int i = 0; i < 4; i++) {
			System.out.print(alphabet[i] + " ");
		}

		System.out.println("\nUsing the lenght variable");
		for (int n = 0; n < alphabet.length; n++) {
			System.out.print(alphabet[n] + " ");
		}

		System.out.println("\nUsing the For-Each method");
		for (String letter : alphabet) {
			System.out.print(letter + " ");
		}

		System.out.println("\n\nDouble Arrays");
		String[][] users = { { "Jhon", "William", "mail@mailto.com" ,"15679797"}, 
				{ "Sarah", "Jackson", "mail@mailto.com" ,"51646576"},
				{ "Rachel", "Wallace", "mailto@mail.com", "564564654" } };

		// Get the size of array
		int numOfUsers = users.length;
		int numOfFields = users[0].length;
		System.out.println("Number of users: " + numOfUsers);
		System.out.println("Number of fields: " + numOfFields);
		
		// Traverse the double array
		
		for(int i = 0;i< numOfUsers;i++) {
			String firstName = users[i][0];
			String lastName = users[i][1];
			String email = users[i][2];
			String phone = users[i][3];
			System.out.println(firstName +" "+lastName+" "+ email+" "+phone);
			
		}
		
		System.out.println("\n\nUsing FOR-EACH");
		
		for(String[] user : users) {
			System.out.print("[ ");
			for(String field:user) {
				System.out.print(field+ " ");
			}
			System.out.println(" ]");
		}

	}

}
