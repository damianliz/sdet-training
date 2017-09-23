package files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Read {

	public static void main(String[] args) {

		// 1. Define the file (path) that we want to read
		String fileName = "/home/pitt/Downloads/MX_COSTCO_LFL_COMPETITORS.xlsx";
		String text = null;

		// 2. Create the file in java
		File file = new File(fileName);

		try {

			// 3. Open the file
			// BufferedReader br = new BufferedReader(new FileReader(file));

			FileInputStream fis = new FileInputStream(file);
			XSSFWorkbook myWorkBook = new XSSFWorkbook(fis);
			XSSFSheet sheet = myWorkBook.getSheetAt(0);

			Iterator<Row> itr = sheet.iterator();

			while (itr.hasNext()) {

				Row row = itr.next();

				Iterator<Cell> cellIterator = row.cellIterator();
				Cell cell = cellIterator.next();

				String value = cell.getStringCellValue();
				System.out.println("Cell value : " + value);
			}

			// 4. Read the file
			// text = br.readLine();

			// 5. Close the resources
			// br.close();

		} catch (FileNotFoundException e) {
			System.out.println("ERROR : File not found : " + fileName);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("ERROR : Could not read the file : " + fileName);
			e.printStackTrace();
		} finally {
			System.out.println("Finished reading the file");
		}
		System.out.println(text);

	}

}
