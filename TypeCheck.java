
import java.util.*;// Scanner, Map, Vector
//import java.util.regex.Pattern;
import java.io.*;

public class TypeCheck {

	public static Scanner keyboard;
	
	public enum DataType{ 
		NONE, BOOL, INT, DOUBLE, STRING, BOOLARR, INTARR, DOUBLEARR, STRINGARR
		}
	
	private static Map<String, DataType> identifierMap = new HashMap<String, DataType>();
	private static Map<String, String> identifierStrMap = new HashMap<String, String>();
	private static Map<String, Integer> arraySizesMap = new HashMap<String, Integer>();
	
	public static void main(String[] args) {
		// read the program input file in from the command line
		boolean debugMode = true;
		
		String filename;
		if(args.length > 0){
			filename = args[0];
		}else{
			filename = "input.txt";
		}
		
		// read in all of the lines from the input "source code" file into a vector of strings
		// do this first for program stability - read first then parse
		Vector<String> inputFileLines = new Vector<String>();
		String strLine;
		try{
			if(debugMode)
				System.out.printf("Reading in a file \"%s\"...\n", filename);

			FileInputStream inFile = new FileInputStream(filename);
			DataInputStream inData = new DataInputStream(inFile);
			BufferedReader inBuffer = new BufferedReader(new InputStreamReader(inData));

			while (  ( strLine = inBuffer.readLine() ) != null  ) {
				if (!strLine.isEmpty()) {// skip lines that are empty
					inputFileLines.add(strLine);
				}
			}
			if(debugMode)
				System.out.printf("File read of \"%s\" successful!\n", filename);

		}catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		}
	
		// make a set of the valid type names to check scanned tokens against
		HashSet<String> validTypes = new HashSet();
		validTypes.add("bool");
		validTypes.add("int");
		validTypes.add("double");
		validTypes.add("string");
		
		String token, strDataType, identifier;
		// parse through each line in the input file
		int counter = 0, arrayIndex;
		StringTokenizer tokenedString;
		while(!inputFileLines.isEmpty()){
			if(debugMode) System.out.println("Line(" + counter + "):" + inputFileLines.firstElement());
			
			strLine = inputFileLines.firstElement();
			tokenedString = new StringTokenizer(strLine);
			arrayIndex = 0;
			
			// get the first token in the string.  
			strDataType = tokenedString.nextToken();
			if(debugMode) System.out.println("\tTOKEN:: \"" + strDataType + "\"");
			
			// check if it's an array
			if(strDataType.contains("[")){
				// this is an array, strip brackets & parse array index
				arrayIndex = findArrayIndex(strDataType);
				strDataType = stripArrayBrackets(strDataType);
			}
			
			// check if a type or not.  if so, parse as declaration, if not parse as definition
			if(validTypes.contains(strDataType)){// strDataType is a valid data type
				// ensure more tokens on line
				if (tokenedString.hasMoreTokens()) {
					// add declaration to map, if array, add size to array-map

					identifier = tokenedString.nextToken();
					if(debugMode) System.out.println("\tDECLARATION:: \"" + identifier + "\""); 
					
					// check the token against regex to verify it's valid
					if(isValidIdentifier(identifier)){
						// place token in map with appropriate token type
						identifierStrMap.put(identifier, strDataType);
						if(debugMode) System.out.println("\t\tAdded to identifier map as \""+ strDataType + "\" type");
						
						if(arrayIndex > 0){
							arraySizesMap.put(identifier, arrayIndex);
							if(debugMode) System.out.println("\t\tAdded to arraySizes map with size [" + arrayIndex + "]");
						}
					}
				}
				
				
			}else{
				// else (line doesn't lead with a type declaration, so it must be a definition

				
				
				
				
			}
			

			// increment counter to next line
			counter++;
			// remove the line from the vector of lines
			inputFileLines.remove(0);
		}
		
		
		
		
		try {
			FileInputStream inFile = new FileInputStream(filename);
			DataInputStream inData = new DataInputStream(inFile);
			BufferedReader inBuffer = new BufferedReader(new InputStreamReader(inData));

			DataType tokenType;
	
			// parse program file for type declarations of variables
			// until no more declarations have been found
			while (  ( strLine = inBuffer.readLine() ) != null  ) {
				if (!strLine.isEmpty()) {// skip lines that are empty
					System.out.println("Consider line: \"" + strLine + "\"");
					tokenedString = new StringTokenizer(strLine);
					// should use string.split here (outmodes StringTOkenizer)

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
							// handle array types:
							if(token.contains("[")){
								// get array index
								arrayIndex = findArrayIndex(token);
								token = stripArrayBrackets(token);
								arraySizesMap.put(token, arrayIndex);
							}
							
							// check the token against regex to verify it's valid
							if(isValidIdentifier(token))// place the validated token in the map
								identifierMap.put(token, tokenType);
							
							// debug printing
							System.out.println("Symbol \" " + token
									+ "\" with type: \"" + tokenType
									+ "\" added to symbol map");
						}
					} else { // if it's not a declaration, it should be a definition
						// parse definitions, checking against stored declarations
						

						// handle array types: 
						if(token.contains("[")){
							arrayIndex = findArrayIndex(token);
							token = stripArrayBrackets(token);
						}
						// lookup the symbol in the map, if it's not there, print an error 1
						if(identifierMap.get(token) == null){
							// ERROR 1: undeclared identifier in expression
							PrintError(1, token);
						}else{
							// parse the rest of the line for valid expression
							System.out.println("\tDefinition::: (" + token + ")");
							// need to check array index against definition size??
							// should the size of the array be stored in the map with name?
							
							//store token in LHS, save for after parsing RHS
							String LHS = token;
							DataType LHStype = identifierMap.get(LHS);
							
							// check for equals,
							token = tokenedString.nextToken();
							if(!token.equals("=") ){// make sure "=" is present
								PrintError(4,"No \"=\" sign present after assignment variable");
							}
							
							// parse RHS of the assignment
							Vector<String> RHS = new Vector<String>();
							while(tokenedString.hasMoreTokens()){
								// build RHS putting tokens into a vector to send to OrderOfOperations parsing functions
								RHS.addElement(tokenedString.nextToken());
							}
							
							// send RHS vector to function to reduce
							DataType RHStype = CheckRHSType(RHS); 
							
							// check LHS DataType against RHS DataType
							if(LHStype == RHStype){
								System.out.println(getSimpleType(LHStype));
							}else{
								PrintError(3, LHS);
							}
							
							// bounds check for arrays
							if(IsArrayType(LHStype)){
								// bounds check
							}
							
						}
					}
				}
			}
			inFile.close();
		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		}

		
	}

	
	private static String getSimpleType(DataType LHS){
		
		String retVal = "none";
		if(LHS == DataType.BOOL || LHS == DataType.BOOLARR){
			retVal = "bool";
		}else if(LHS == DataType.INT || LHS == DataType.INTARR){
			retVal = "int";
		}else if(LHS == DataType.DOUBLE || LHS == DataType.DOUBLEARR){
			retVal = "double";
		}else if(LHS == DataType.STRING || LHS == DataType.STRINGARR){
			retVal = "string";
		}
		return retVal;
	}
	
	private static DataType CheckRHSType(Vector<String> RHS){
		DataType retVal;
		// reduce array indices
		
		// reduce mult/div
		
		// reduce add/sub
				
		// reduce <,>,=,>=,<=
		
		
		String finalType = "int";
		retVal =  getDataType(finalType);
		return retVal;
	}
	
	private static boolean IsArrayType(DataType thisType){
		boolean retVal = false;
		if(thisType.ordinal()>4) 
			retVal = true;
		return retVal;
	}
	
	private static boolean isValidIdentifier(String token){
		boolean retVal = false;
		if(token.matches("[a-zA-Z]+"))
			retVal = true;
		return retVal;
	}
	
	private static String stripArrayBrackets(String token){
		// strip token of brackets
		token = token.substring(0, token.indexOf("["));
		return token;
	}
	
	private static void PrintError(int errorNum, String message){
		switch (errorNum){
		case 1: 
			System.out.println("ERROR 1: Undeclared identifier in expression (\"" + message + "\")");
			break;
		case 2: 
			System.out.println("ERROR 2: Type Mispatch in expression (\"" + message + "\")");
			break;
		case 3: 
			System.out.println("ERROR 3: Type Mispatch in assignment (\"" + message + "\")");
			break;
		default: 
			System.out.println("ERROR 4: General formatting error (\"" + message + "\")");
		}
	}
	
	private static int findArrayIndex(String token){
		int LBindex = token.indexOf("[");
		int RBindex = token.indexOf("]");
		int retVal = 0;
		try{
			// the String to int conversion happens here
			retVal = Integer.parseInt(token.substring(LBindex+1, RBindex));
			// print out the value after the conversion
			System.out.println("\tarray index = " + retVal);

		} catch (NumberFormatException nfe)
		{
			System.out.println("NumberFormatException: " + nfe.getMessage());
		}
		return retVal;
	}

	
	private static DataType getDataType(String token){
		DataType retVal = DataType.NONE; 
		if(token.equals("int")){
			retVal = DataType.INT;
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
