
import java.util.Scanner;// Scanner
import java.io.*;

public class TypeCheck {

	public static Scanner keyboard;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		// declare local variables
		
		
		// read the program input file in from the command line
		String filename;
		if(args.length > 0){
			filename = args[0];
		}else{
			filename = "input.txt";
		}
		System.out.printf("Reading in a file from \"%s\":::\n", filename);
		
		
		try
		{
			FileInputStream inFile = new FileInputStream(filename);
			DataInputStream inData = new DataInputStream(inFile);
			BufferedReader inBuffer = new BufferedReader(new InputStreamReader(inData));
			String strLine;
			
			while((strLine = inBuffer.readLine() ) != null){
				System.out.println(strLine);
			}
			inFile.close();
		}
		catch (IOException e){
			System.err.println("Error: " + e.getMessage());
		}
	
		
		// parse program file for type declarations of variables
		// until no more declarations have been found

		// rest of the file is definitions
		// parse definitions, checking against stored declarations
		
	}

}
