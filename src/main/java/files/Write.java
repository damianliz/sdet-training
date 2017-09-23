package files;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Write {

	public static void main(String[] args) {

		// 1. Define the path that we want to write
		String fileName = "/home/pitt/Documents/Dev/FileToWrite";
		String message = "I'm writing data that will be placed to a file";

		// 2. Create the file in java
		File file = new File(fileName);

		// 3. Open the file
		FileWriter fw;
		try {
			fw = new FileWriter(file);

			// 4. Write on file
			fw.write(message);

			// 5. Close the resource
			fw.close();
		} catch (IOException e) {
			System.out.println("ERROR : Could not read file - " + fileName);
			e.printStackTrace();
		} finally {
			System.out.println("Closing the file writer");
		}

	}

}
