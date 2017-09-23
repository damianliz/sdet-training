package datastructures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Lists {

	public static void main(String[] args) {
		// 1. Create a collection

		ArrayList<String> cities = new ArrayList<>();

		// 2. Add some elements
		cities.add("Cleveland");
		cities.add("Toronto");
		cities.add("Chicago");
		cities.add("Miami");

		// 3. Iterate the collection
		for (String citi : cities) {
			System.out.println(citi);
		}

		// 4. Get the size
		int size = cities.size();
		System.out.println("There are [ " + size + " ] elements in the collection");

		// 5. Retrieve specific element
		System.out.println(cities.get(2));

		// 6. Remove
		cities.remove(0);
		size = cities.size();
		System.out.println("Now there are [ " + size + " ] elements in the collection");
		for (String citi : cities) {
			System.out.println(citi);
		}
		
		int[] arr1 = {8,9,3,6,4,1};
		int[] arr2 = {18,5,3,2,7};
		
		Arrays.sort(arr1);
		HashSet<Integer> hash = new HashSet<>();
		
		for(int i = 0; i<arr1.length; i++) {
			System.out.println("Sort "+arr1[i]);

		}
	
		
	}

}
