
// Created by Austin Patel on 7/7/16 at 8:20 PM

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

// This program translates VM files to the Hack assembly language
public class Main {

	// The backbone of the program
	public static void main(String[] args) {

		Scanner scanner = new Scanner(System.in);
				
		String fileLocation;
		
		if (args.length == 0)
		{
			System.out.println("Enter the location of the *.vm file(s) to be converted into the Hack Assembly Language (Folder or File):");
			fileLocation = scanner.nextLine();
		}
		else
			fileLocation = args[0];

		CodeWriter codeWriter;
		ArrayList<File> vmFiles = new ArrayList<File>();
		
		if (fileLocation.contains(".vm")) {
			vmFiles.add(new File(fileLocation));
			
			File writeLocation = new File(fileLocation.replace(".vm", ".asm"));
			codeWriter = new CodeWriter(writeLocation);
		} else {
			// Get the name of the program
			String programName = fileLocation.substring(fileLocation.lastIndexOf('\\') + 1, fileLocation.length());
			
			// Add "\" to the end of the project folder file (helps with figuring out the programName)
			fileLocation = fileLocation.concat("\\");
			
			// Get an ordered list of all the *.vm files to be compiled

			for (File file : new File(fileLocation).listFiles())
				if (file.getAbsolutePath().endsWith(".vm"))
					vmFiles.add(file);
						
			// Initialize the objects for writing the *.asm file
			File writeLocation = new File(fileLocation + programName + ".asm");
			codeWriter = new CodeWriter(writeLocation) {{
				writeInit();
			}};
		}		
		
		// Loop through each file one at a time and compile it into the destination folder
		for (File file : vmFiles) { // Loop until the program has finished compiling
			// Get the new read/write locations for the current file
			File readLocation = file;
			Parser parser = new Parser(readLocation);
			
			codeWriter.setFileName(file.getName().substring(0, file.getName().indexOf(".")));

			while (true) { 
				if (!parser.advance()) // If the program is finished, then quit
					break;

				// Get the command information from the Parser
				String command = parser.getCurrentCommand();
				Parser.VmCommand type = parser.commandType();
				
				// Compile the line
				switch (type) {

				case C_ARITHMETIC:
					codeWriter.writeArithmetic(command);
					break;

				case C_PUSH:
				case C_POP:
					codeWriter.writePushPop(type, parser.arg1(), parser.arg2());
					break;

				case C_LABEL:
					codeWriter.writeLabel(parser.arg1());
					break;

				case C_GOTO:
					codeWriter.writeGoto(parser.arg1());
					break;

				case C_IF:
					codeWriter.writeIf(parser.arg1());
					break;

				case C_FUNCTION:
					codeWriter.writeFunction(parser.arg1(), parser.arg2());
					break;

				case C_RETURN:
					codeWriter.writeReturn();
					break;

				case C_CALL:
					codeWriter.writeCall(parser.arg1(), parser.arg2());
					break;

				default:
					System.out.println("ERROR: The type: " + type + " could not be handled.");
					break;
				}
			}
		}

		codeWriter.close();
		scanner.close();

		System.out.println("VM to assembly language conversion complete.");
	}

}
