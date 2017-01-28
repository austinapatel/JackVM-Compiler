
// Created by Austin Patel on 7/7/16 at 8:35 PM

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

// Handles the parsing of a single .vm file, and encapsulates
//access to the input code. It reads VM commands, parses
//them, and provides convenient access to their components.
//In addition, it removes all white space and comments.
public class Parser {

	// Holds all the possible VM command types
	public static enum VmCommand {
		C_ARITHMETIC, C_PUSH, C_POP, C_LABEL, C_GOTO, C_IF, C_FUNCTION, C_RETURN, C_CALL
	}

	// Relates a VM command string to a VmCommand type
	@SuppressWarnings("serial")
	private HashMap<String, VmCommand> vmConversion = new HashMap<String, VmCommand>() {
		{
			put("push", VmCommand.C_PUSH);
			put("pop", VmCommand.C_POP);
			put("label", VmCommand.C_LABEL);
			put("goto", VmCommand.C_GOTO);
			put("if-goto", VmCommand.C_IF);
			put("function", VmCommand.C_FUNCTION);
			put("return", VmCommand.C_RETURN);
			put("call", VmCommand.C_CALL);
			put("add", VmCommand.C_ARITHMETIC);
			put("sub", VmCommand.C_ARITHMETIC);
			put("neg", VmCommand.C_ARITHMETIC);
			put("eq", VmCommand.C_ARITHMETIC);
			put("gt", VmCommand.C_ARITHMETIC);
			put("lt", VmCommand.C_ARITHMETIC);
			put("and", VmCommand.C_ARITHMETIC);
			put("or", VmCommand.C_ARITHMETIC);
			put("not", VmCommand.C_ARITHMETIC);
		}
	};

	private BufferedReader fileReader; // Reads the file
	private String currentLine; // Command at the current line
	private int lineNum; // Index of the current command
	private int numLines; // Number of commands in the file

	// Opens the input file/stream and gets ready to parse it.
	public Parser(File file) {
		try {

			// Open the file to get ready to read it
			fileReader = new BufferedReader(new FileReader(file));

			// Get the number of commands in the file
			while ((currentLine = fileReader.readLine()) != null) {
				String curLineEdit = currentLine.trim();

				if (curLineEdit.equals(""))
					continue;

				if (!curLineEdit.substring(0, 2).equals("//"))
					numLines++;
			}

			// Re-open the file so it starts from the beginning
			fileReader = new BufferedReader(new FileReader(file));

		} catch (IOException e) {
			System.out.println("Unable to read the file.");
			e.printStackTrace();
		}
	}

	// Gets the current command
	public String getCurrentCommand() {
		return currentLine;
	}

	// Determines if there are more commands
	private boolean hasMoreCommands() {
		return lineNum < numLines;
	}

	// Reads the next command from the input
	// and makes it the current command. Should
	// be called only if hasMoreCommands() is true.
	// Initially there is no current command.
	public boolean advance() {

		// Don't continue reading the file if it is over
		if (!hasMoreCommands())
			return false;

		boolean validLine = false;
		while (!validLine) {
			try {
				currentLine = fileReader.readLine().trim();

				validLine = !(currentLine.startsWith("//") || currentLine.equals(""));

				// Remove comments at the ends of the line
				if (currentLine.contains("//"))
					currentLine = currentLine.substring(0, currentLine.indexOf("//")).trim();

			} catch (IOException e) {
				System.out.println("Failed to read the next line");
				e.printStackTrace();
			}
		}

		lineNum++;

		return true;
	}

	// Returns the type of the current VM command.
	// C_ARITHMETIC is returned for all the arithmetic
	// commands.
	public VmCommand commandType() {
		return vmConversion.get(currentLine.split(" ")[0]);
	}

	// Returns the first argument of the current command.
	// In the case of C_ARITHMETIC, the command itself
	// (add, sub, etc.) is returned. Should not be called if
	// the current command is C_RETURN.
	public String arg1() {
		// Return the command itself if the current command
		// is C_ARITHMETIC
		if (commandType() == VmCommand.C_ARITHMETIC)
			return currentLine;

		return currentLine.split(" ")[1];
	}

	// Returns the second argument of the current
	// command. Should be called only if the current
	// command is C_PUSH, C_POP, C_FUNCTION, or
	// C_CALL.
	public int arg2() {
		return Integer.parseInt(currentLine.split(" ")[2]);
	}

}
