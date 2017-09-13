package JavaTraining;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

public class Lab1 {

	public static void main(String[] args) {
		System.out.println("Program is starting");
		System.out.println("\n1. Function that takes a value n and return the sum of numbers 1 to n.");
		System.out.println("Result : " + sum(5));

		System.out.println("\n2. Function that computes the factorial number.");
		System.out.println("Result : " + factorialNum(5));

		System.out.println("\n3.1 Function that return minimum value of an array.");
		int[] arrayInt = { 6, 8, 3, 1, 9, 2, 5 };
		System.out.println("Result : " + minValue(arrayInt));
		System.out.println("\n3.2 Function that return maximum value of an array.");
		System.out.println("Result : " + maxValue(arrayInt));
		System.out.println("\n3.3 Function that return average value of an array.");
		System.out.println("Result : " + avgValue(arrayInt));

		System.out.println("*********");
		System.out.println("End of program");

	}

	public static int sum(int value) {
		int count = 1;
		int sum = 0;
		while (count <= value) {

			sum = sum + count;
			count++;
		}
		return sum;
	}

	public static int factorialNum(int n) {
		int factorial = 1;

		if (n == 0) {
			return 1;
		}

		if (n < 0) {
			System.out.println("Not valid number.");
			return 0;
		}

		for (int i = 1; i <= n; i++) {

			factorial = factorial * i;

		}
		return factorial;
	}

	public static int minValue(int[] arrayInt) {
		Arrays.sort(arrayInt);
		return arrayInt[0];
	}

	public static int maxValue(int[] arrayInt) {
		Arrays.sort(arrayInt);
		return arrayInt[arrayInt.length - 1];
	}

	public static float avgValue(int[] arrayInt) {
		int sum = 0;
		for (int i = 0; i <= arrayInt.length; i++) {
			sum = sum + i;
		}

		return sum / arrayInt.length;
	}

}
