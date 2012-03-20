/*******************************************************************************
 * FILENAME:    TypeCheck.java
 * DESCRIPTION: Type Checking Parser
 * AUTHOR:      James Matthew Welch [JMW]
 * SCHOOL:      Arizona State University
 * CLASS:       CSE340: Principles of Programming Languages
 * INSTRUCTOR:  Dr. Toni Farley
 * SECTION:     27199
 * TERM:        Spring 2012
 ******************************************************************************/
/* :::Program Description:::
 * This project will implement a type-checking algorithm based on a provided 
 * language description. The Type Checker will read in a program stored in a 
 * text file, and output the type-checking results. It will use the ideas from 
 * Hindley-Milner type checking to infer types of expressions, and determine 
 * the type correctness of the program. The procedures will process expressions
 * using the correct order of operations, and keep track of intermediate types
 * during processing. This program is written in Java using jre7. */

import java.util.*;// Scanner, Map, Vector
import java.io.*;

public class TypeCheck {

	public static Scanner keyboard;
	
/*	public enum DataType{ 
		NONE, BOOL, INT, DOUBLE, STRING, BOOLARR, INTARR, DOUBLEARR, STRINGARR
		}
	
	private static Map<String, DataType> identifierMap = new HashMap<String, DataType>();
*/
	private static Map<String, String> identifierStrMap = new HashMap<String, String>();
	private static Map<String, Integer> arraySizesMap = new HashMap<String, Integer>();
	private static boolean debugMode = false;
	private static HashSet<String> validTypes = new HashSet();
		
	public static void main(String[] args) {
		// read the program input file in from the command line

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
		validTypes.add("bool");
		validTypes.add("int");
		validTypes.add("double");
		validTypes.add("string");
		
		
		String token, strDataType, identifier;
		// parse through each line in the input file
		int lineCounter = 0, arrayIndex;
		StringTokenizer tokenedString;
		while(!inputFileLines.isEmpty()){
			if(debugMode) System.out.println("Line(" + lineCounter + "): \"" + inputFileLines.firstElement() + "\"");
			
			strLine = inputFileLines.firstElement();
			tokenedString = new StringTokenizer(strLine);
			arrayIndex = 0;
			
			// get the first token in the string.  
			strDataType = tokenedString.nextToken();
			
			// check if it's an array
			if(strDataType.contains("[")){
				// this is an array, strip brackets & parse array index
				arrayIndex = findArrayIndex(strDataType);
				strDataType = stripArrayBrackets(strDataType);
			}
			
			// check if a type or not.  if so, parse as declaration, if not parse as definition
			if(validTypes.contains(strDataType)){// strDataType is a valid data type
				if(debugMode) System.out.println("\tTYPE:\"" + strDataType + "\"");
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
						
						//add to array map if array
						if(arrayIndex > 0){
							arraySizesMap.put(identifier, arrayIndex);
							if(debugMode) System.out.println("\t\tAdded to arraySizes map with size [" + arrayIndex + "]");
						}
					}
				}
			}else{// line doesn't lead with a type declaration, so it must be a definition/expression 
				
				// reassign first token in definition as LHS of expression
				String LHSToken = strDataType;
				if(debugMode) System.out.println("\tEXPRESSION:: \"" + LHSToken + "\""); 
				
				// retrieve the type string from the map
				String LHSType = identifierStrMap.get(LHSToken);
				
				// check for equals,
				token = tokenedString.nextToken();
				if(!token.equals("=") ){// make sure "=" is present
					PrintError(4,"No \"=\" sign present after assignment variable");
				}
				
				/////parse RHS of the assignment/////
				// 	convert tokened string to vector
				Vector<String> RHS = convertTokenedStringToVector(tokenedString); 
				
				// reduce the expression on the RHS to a single type
				String RHSType = getRHSType(RHS);
								
				// check LHS DataType against RHS DataType
				if(LHSType.equals(RHSType)){
					System.out.println(LHSType);
				}else if(RHSType.equals("Error1") || RHSType.equals("Error2")){
					// do nothing since error was caught in getRHSType()
				}else{
					PrintError(3, LHSToken);
				}
			}
			// increment counter to next line
			lineCounter++;
			// remove the line from the vector of lines
			inputFileLines.remove(0);
		}
		// end of program
	}

	private static String getRHSType(Vector<String> RHS){
		String retVal = "double";
		// reduce arrays to types 
		RHS = replaceArrayRefs(RHS);
		if(debugMode) System.out.println("RHS::ArrayRefs replaced: " + RHS.toString());

		// reduce remaining identifiers to types
		RHS = replaceIdentifiers(RHS);
		if(debugMode) System.out.println("RHS::Identifiers replaced: " + RHS.toString());

		// reduce mult/div
		RHS = reduceMultDiv(RHS);
		if(debugMode) System.out.println("RHS::Mult/Div reduced: " + RHS.toString());
		
		// reduce add/sub
		RHS = reduceAddSub(RHS);
		if(debugMode) System.out.println("RHS::Add+Sub reduced: " + RHS.toString());

		// reduce <,>,=,>=,<=
		RHS = reduceEquality(RHS);
		if(debugMode) System.out.println("RHS::Equality reduced: " + RHS.toString());


		if(debugMode) System.out.println("\tTOKEN:\"" + RHS.firstElement() + "\"");

		if(RHS.size() == 1){
			retVal = RHS.get(0);
		}

		return retVal;
	}

	// step 1, replace array references with their types
	private static Vector<String> replaceArrayRefs(Vector<String> RHS){
		String token, tokenOrig;
		for(int i = 0; i < RHS.size(); ++i){
			token = RHS.elementAt(i);
			tokenOrig = token;
			
			// check if it's an array
			if(token.contains("[")){
				// this is an array, strip brackets & parse array index
//				int arrayIndex = findArrayIndex(token);
				token = stripArrayBrackets(token);
			}
			
			// TODO: should check array sizes at some point too
			if(isValidIdentifier(token)){
				if(identifierStrMap.containsKey( token ) ){
					RHS.remove(i);
					String dataType = identifierStrMap.get(token);
					if(debugMode) System.out.println("\tTOKEN:\"" + tokenOrig + "\" replaced with \""+ dataType +"\"");
					RHS.insertElementAt(dataType, i);
				}else{
					PrintError(1, tokenOrig);
					RHS.clear();
					RHS.add("Error1");
				}
			}
		}
		return RHS;
	}
	
	// step 2, replace identifiers with their types
	private static Vector<String> replaceIdentifiers(Vector<String> RHS){
		String token;
		for(int i = 0; i < RHS.size(); ++i){
			token = RHS.elementAt(i);
			if(!validTypes.contains(token)){// ensure not already been replaced as arrayRef
				if(isValidIdentifier(token)){
					if(identifierStrMap.containsKey( token ) ){
						RHS.remove(i);
						String dataType = identifierStrMap.get(token);
						if(debugMode) System.out.println("\tTOKEN:\"" + token + "\" replaced with \""+ dataType +"\"");
						RHS.insertElementAt(dataType, i);
					}else{
						PrintError(1, token);
						RHS.clear();
						RHS.add("Error1");
					}
				}
			}
		}
		return RHS;
	}

	// step 3: replace multiplicatoin.division with their resulting types
	private static Vector<String> reduceMultDiv(Vector<String> RHS){
		String token;
		for(int i = 0; i < RHS.size(); ++i){
			token = RHS.elementAt(i);
			if(isMultSymbol(token)){
				String Lfactor = RHS.elementAt(i-1);
				String Rfactor = RHS.elementAt(i+1);
				
				if(Lfactor.equals(Rfactor)){
					token = Lfactor;
					// replace all three with type
				}else if( (Lfactor.equals("double") && Rfactor.equals("int")) || 
						(Lfactor.equals("int") && Rfactor.equals("double")) )
				{	// double-int math is always widening
					// TODO narrowing on assignment must be checked
					token = "double";
				}else{
					// error on expression type (2), clear and return error
					PrintError(2, Lfactor + ", " + Rfactor);
					RHS.clear();
					RHS.add("Error2");
					return RHS;
				}
				RHS.setElementAt(token, i-1);
				RHS.remove(i+1);
				RHS.remove(i);
			}
		}
		return RHS;
	}
	
	// step 4: replace addition.subtration with their resulting types
	private static Vector<String> reduceAddSub(Vector<String> RHS){
		String token;
		for(int i = 0; i < RHS.size(); ++i){
			token = RHS.elementAt(i);
			if(isAddSymbol(token)){
				String augend = RHS.elementAt(i-1);
				String addend = RHS.elementAt(i+1);
				
				if(augend.equals(addend)){
					token = augend;
					// replace all three with type
				}else if(   (augend.equals("double") && addend.equals("int") )||
						(augend.equals("int")&& addend.equals("double") ) ){
					// double-?? math, always widening
					// TODO narrowing on assignment must be checked
					token = "double";
				}else{
					// error on expression type (2), clear and return error
					PrintError(2, augend + ", " + addend);
					RHS.clear();
					RHS.add("Error2");
					return RHS;
				}
				RHS.setElementAt(token, i-1);
				RHS.remove(i+1);
				RHS.remove(i);
			}
		}
		return RHS;
	}
	
	// step 5: replace equality symbols with their types
	private static Vector<String> reduceEquality(Vector<String> RHS){
		String token;
		for(int i = 0; i < RHS.size(); ++i){
			token = RHS.elementAt(i);
			if(isEqualitySymbol(token)){
				String LHSType = RHS.elementAt(i-1);
				String RHSType = RHS.elementAt(i+1);
				
				if(LHSType.equals(RHSType)){
					token = "bool";
					// replace all three with type
				}else if(LHSType.equals("double") || RHSType.equals("double")){
					// double-?? compare, always widening
					token = "bool";
					// TODO narrowing on assignment must be checked
				}else{
					// error condition, clear and return error
					PrintError(2, LHSType + ", " + RHSType);
					RHS.clear();
					RHS.add("Error2");
					return RHS;
				}
				RHS.setElementAt(token, i-1);
				RHS.remove(i+1);
				RHS.remove(i);
			}
		}
		return RHS;
	}
	
	private static boolean isEqualitySymbol(String token){
		boolean retVal = false;
		if(token.matches("==|>=|>|<=|<"))
			retVal = true;
		return retVal;
	}
	
	private static boolean isAddSymbol(String token){
		boolean retVal = false;
		if(token.equals("+") || token.equals("-"))
			retVal = true;
		return retVal;
	}

	private static boolean isMultSymbol(String token){
		boolean retVal = false;
		if(token.equals("*") || token.equals("/"))
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
		if(debugMode){message = ("(\"" + message + "\")");}
		else message = "";
		
		switch (errorNum){
		case 1: 
			System.out.println("ERROR 1: Undeclared identifier in expression" + message);
			break;
		case 2: 
			System.out.println("ERROR 2: Type Mispatch in expression" + message);
			break;
		case 3: 
			System.out.println("ERROR 3: Type Mispatch in assignment" + message);
			break;
		default: 
			System.out.println("ERROR 4: General formatting error" + message);
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
			if(debugMode) System.out.println("\tarray index = " + retVal);

		} catch (NumberFormatException nfe)
		{
			System.out.println("NumberFormatException: " + nfe.getMessage());
		}
		return retVal;
	}
	
	private static Vector<String> convertTokenedStringToVector(StringTokenizer tokenedString){
		Vector<String> RHS = new Vector<String>();
		while(tokenedString.hasMoreTokens()){
			/* build RHS putting tokens into a vector to send to 
			 * OrderOfOperations parsing functions */
			RHS.add(tokenedString.nextToken());
		}
		return RHS;
	}
	
/* TODO:  I should delete this, but I'm a pack rat for the moment.  DELETE ME LATER		
 *
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
		
	private static DataType CheckRHSType(Vector<String> RHS){
		DataType retVal;
		// reduce array indices
		
		// reduce mult/div
		
		// reduce add/sub
				
		// reduce <,>,=,>=,<=
		
		
		DataType finalType = getDataType("int");
		retVal = finalType;
		return retVal;
	}
	
	private static boolean IsArrayType(DataType thisType){
		boolean retVal = false;
		if(thisType.ordinal()>4) 
			retVal = true;
		return retVal;
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

	// snippet from main()
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
//							DataType RHStype = CheckRHSType(RHS); 
							DataType RHStype = DataType.INT;
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
		*/
}
