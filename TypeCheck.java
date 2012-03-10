
import java.util.*;// Scanner, Map
import java.util.regex.Pattern;
import java.io.*;

public class TypeCheck {

	public static Scanner keyboard;
	/**
	 * @param args
	 */
	
	public enum DataType{ 
		NONE, INT, DOUBLE, STRING, BOOL, INTARR, DOUBLEARR, STRINGARR, BOOLARR
		}
	
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
		
		
		try {
			FileInputStream inFile = new FileInputStream(filename);
			DataInputStream inData = new DataInputStream(inFile);
			BufferedReader inBuffer = new BufferedReader(new InputStreamReader(
					inData));
			String strLine, token;
			DataType tokenType;
			Map<String, DataType> symbolMap = new HashMap<String, DataType>();

			// parse program file for type declarations of variables
			// until no more declarations have been found
			while ((strLine = inBuffer.readLine()) != null) {
				if (!strLine.isEmpty()) {// skip lines that are empty
					System.out.println("Consider line: \"" + strLine + "\"");
					StringTokenizer tokenedString = new StringTokenizer(strLine);

					// first token in line must be a type

					// get first token, assume it's a type.
					token = tokenedString.nextToken();
					tokenType = getDataType(token);
					if (tokenType != DataType.NONE) { 
						// valid type, parse line and put it in the map
						if (tokenedString.hasMoreTokens()) {
							
							// ensure more tokens on line
							token = tokenedString.nextToken();

							// place token in map with appropriate token type
							symbolMap.put(token, tokenType);

							System.out.println("Symbol \" " + token
									+ "\" with type: \"" + tokenType
									+ "\" added to symbol map");
						}
					} else { 
						// if it's not a declaration, it should be a definition

						// strip brackets from array types: 
						if(token.contains("[")){
							token = token.substring(0, token.indexOf("["));
						}
						
						// lookup the symbol in the map, if it's not there, print an error 1
						if(symbolMap.get(token) == null){
							// ERROR 1: undeclared identifier in expression
							System.out.println("ERROR 1: Undeclared identifier in expression (\"" + token + "\")");
						}else{
							// parse the rest of the line for valid expression
							System.out.println("\tDefinition::: (" + token + ")");
						}
					}
				}
			}
			inFile.close();
		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		}

		// rest of the file is definitions
		// parse definitions, checking against stored declarations
		
	}
	
	public static DataType getDataType(String token){
		DataType retVal = DataType.NONE; 
		
		if(token.equals("int")){
			retVal = DataType.INT;
		}else if(token.equals("int") ){
			retVal = DataType.BOOL;
		}else if(token.equals("bool") ){
			retVal = DataType.BOOL;
		}else if(token.equals("string") ){
			retVal = DataType.STRING;
		}else if(token.equals("double") ){
			retVal = DataType.DOUBLE;
		}else if(token.matches("int\\[\\d+\\]") ){
			retVal = DataType.INTARR;
		}else if(token.matches("bool\\[\\d+\\]")){
			retVal = DataType.BOOLARR;
		}else if(token.matches("double\\[\\d+\\]")){
			retVal = DataType.DOUBLEARR;
		}else if(token.matches("string\\[\\d+\\]")){
			retVal = DataType.STRINGARR;
		}

		return retVal;
	}

}
