Description:
This project will implement a type-checking algorithm based on a provided language description.  The Type Checker will read in a program stored in a text file, and output the type-checking results. It will use the ideas from Hindley-Milner type checking to infer types of expressions, and determine the type correctness of the program. The procedures will process expressions using the correct order of operations, and keep track of intermediate types during processing.
This program is written in Java using jre7.  

Instructions:
To compile this program: javac TypeCheck.java
To run this program with the file "input.txt" (default file): java TypeCheck
To run this program with an alternate file, "program.txt": java TypeCheck program.txt