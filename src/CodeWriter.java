
// Created by Austin Patel on 7/7/16 at 8:36 PM

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

//Translates VM commands into Hack assembly code. 
public class CodeWriter {

	// Contains the different arg1 possibilities for the pop command and maps
	// them to the assembly language symbol that will then be converted by the
	// assembler into the base address of the correct virtual memory segment
	@SuppressWarnings("serial")
	private HashMap<String, String> virtualMemorySegments = new HashMap<String, String>() {
		{
			put("local", "LCL");
			put("argument", "ARG");
			put("this", "THIS");
			put("that", "THAT");
			put("temp", "5");
			put("pointer", "3");
		}
	};

	private BufferedWriter writer; // Writes the Hack assembly code to a file
	private String fileName; // Name of the current VM file being translated
	private int curUniqueLabel; // Number used by the function
								// makeUniqueLabel
	private String functionName; // The name of the current function

	// Opens the output file/stream and gets ready to
	// write into it.
	public CodeWriter(File destination) {
		try {
			writer = new BufferedWriter(new FileWriter(destination.getAbsolutePath()));
		} catch (IOException e) {
			System.out.println("The file to be written to was unable to be accessed.");
			e.printStackTrace();
		}
	}

	// Makes a unique label for VM commands used by the VM translator
	private String makeUniqueTranslatorLabel() {
		curUniqueLabel++;
		return "Unique" + curUniqueLabel;
	}

	// Makes a unique label for the VM commands in the *.vm files
	private String makeUniqueVMLabel(String label) {
		return functionName + "$" + label;
	}

	// Writes assembly code that effects the VM initialization, also called
	// bootstrap code. This code must be placed at the beginning of the output
	// file
	public void writeInit() {
		// Initialize SP to 256 + 5 (number of variables pushed when calling a
		// function -- which doesn't need to happen in initialization)
		write("@261");
		write("D=A");
		write("@SP");
		write("M=D");

		// Call Sys.init
		write("@Sys.init");
		write("0;JMP");
	}

	// Informs the code writer that the translation of a
	// new VM file is started.
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	// Writes the assembly code that is the translation
	// of the given arithmetic command
	public void writeArithmetic(String command) {
		if (command.equals("add"))
			writeTwoValueOperation('+');
		else if (command.equals("sub"))
			writeTwoValueOperation('-');
		else if (command.equals("neg"))
			writeOneValueOperation('-');
		else if (command.equals("eq"))
			writeEquality("JEQ");
		else if (command.equals("gt"))
			writeEquality("JGT");
		else if (command.equals("lt"))
			writeEquality("JLT");
		else if (command.equals("and"))
			writeTwoValueOperation('&');
		else if (command.equals("or"))
			writeTwoValueOperation('|');
		else if (command.equals("not"))
			writeOneValueOperation('!');
	}

	// Writes the assembly code for equality statements
	private void writeEquality(String jumpComparison) {
		String endLabel = makeUniqueTranslatorLabel();
		String trueLabel = makeUniqueTranslatorLabel();

		write("@SP"); // Get the current location on the stack
		write("A=M-1"); // Move to the 1st item on the stack
		write("D=M"); // Store the 1st value from the stack
		write("A=A-1"); // Move to the 2nd item on the stack
		write("D=M-D"); // Find the difference between the two values
		write("@SP"); // Get the current location on the stack
		write("M=M-1"); // Move the stack pointer 1 position back
		write("@" + trueLabel); // Point towards the true part of the eq
		write("D;" + jumpComparison); // Jump to the true part if they are equal
		write("@SP"); // Get the current location on the stack
		write("A=M-1"); // Go back 1 spot on the stack
		write("M=0"); // Set the result to false
		write("@" + endLabel); // Point to the end of the true part
		write("0;JMP"); // Jump to the end of the true part
		write("(" + trueLabel + ")");
		write("@SP"); // Get the current location on the stack
		write("A=M-1"); // Set the current address to the location of the result
		write("M=-1"); // Set the result to true
		write("(" + endLabel + ")");
	}

	// Writes the assembly code for an operation on the top item on the stack
	private void writeOneValueOperation(char operation) {
		write("@SP"); // Get the location of the stack pointer
		write("A=M-1"); // Get the first value in the stack
		write("M=" + operation + "M"); // Perform the given operation on that
										// value
	}

	// Writes the assembly code for operations on the top two items on the stack
	// This does not include equality operations (ex: lt, gt, eq)
	private void writeTwoValueOperation(char operation) {
		write("@SP"); // Get the current location in the stack
		write("A=M-1"); // Move to the 1st item in the stack
		write("D=M"); // Store the 1st value from the stack
		write("A=A-1"); // Move to the 2nd item in the stack
		write("M=M" + operation + "D"); // Add the two values together
		write("@SP"); // Get the current location in the stack
		write("M=M-1"); // Go to the current location in the stack - 1
	}

	// Writes the assembly code that is the translation
	// of the given command, where command is either
	// C_PUSH or C_POP.
	public void writePushPop(Parser.VmCommand commandType, String segment, int index) {
		switch (commandType) {

		case C_PUSH:
			// Get the correct value to be pushed onto the stack in the D
			// Register
			if (segment.equals("constant")) {
				write("@" + index); // Go to the address of the value to be
									// added to the stack
				write("D=A"); // Store the value of the address into the D
								// Register
			} else if (segment.equals("static")) {
				write("@" + fileName + "." + index);
				write("D=M");
			} else {
				write("@" + virtualMemorySegments.get(segment));

				if (segment.equals("temp") || segment.equals("pointer"))
					write("D=A"); // Store the address in the D Register
				else
					write("D=M"); // Store the base address in the D Register

				write("@" + index); // Go to the index address of the ram
				write("A=D+A"); // Add the index of the virtual ram segment to
								// the base to get the correct RAM location
//				write("A=D"); // Change the address to the RAM location
				write("D=M"); // Set the value from the address into the D
								// Register
			}

			// Push the value in the D Register onto the stack
			write("@SP"); // Get the pointer to the current stack location
			write("M=M+1"); // Increment the pointer's location
			write("A=M-1"); // Go to the open location in the stack
			write("M=D"); // Add the push's value to the stack
			write("@SP"); // Go to the stack location pointer
			break;

		case C_POP:

			String tempSpot = "R14";

			// Go to the ram location that contains the base address of the
			// virtual memory segment
			if (segment.equals("static")) {
				write("@" + fileName + "." + index);
				write("D=A");
			} else {
				write("@" + virtualMemorySegments.get(segment));

				if (segment.equals("temp") || segment.equals("pointer"))
					write("D=A"); // Store the address in the D Register
				else
					write("D=M"); // Store the base address in the D Register

				write("@" + index); // Go to the index address of the ram
				write("D=D+A"); // Add the index of the virtual ram segment to
								// the
								// base to get the correct RAM location
			}

			// Pop the value from the stack onto the location on the D Register
			write("@" + tempSpot); // Go to the address of a temp storage spot
			write("M=D"); // Store the RAM destination location in the R5
							// register
			write("@SP"); // Go to the stack pointer
			write("A=M-1"); // Go to the the first item in the stack
			write("D=M"); // Store the value to be "popped" in the D Register
			write("@" + tempSpot); // Go back to the temp storage location
			write("A=M"); // Go to the location stored in the temp storage
							// location
			write("M=D"); // Store the "popped" value in the correct RAM
							// location
			write("@SP"); // Go to the stack pointer
			write("M=M-1"); // Move the stack pointer back one spot

			break;

		default:
			break;
		}
	}

	// Writes the assembly code that effects the label command.
	public void writeLabel(String label) {
		write("(" + makeUniqueVMLabel(label) + ")"); // Create a label
	}

	// Writes assembly code that effects the goto command.
	public void writeGoto(String label) {
		write("@" + makeUniqueVMLabel(label));
		write("0;JMP"); // Jump to the label unconditionally
	}

	// Writes assembly code that effects the if command.
	public void writeIf(String label) {
		write("@SP");
		write("A=M-1");
		write("D=M"); // Store the first value from the stack
		write("@SP");
		write("M=M-1"); // Move the stack pointer back
		write("@" + makeUniqueVMLabel(label));
		write("D;JNE"); // Jump if the first value on the stack is true
	}

	// Writes assembly code that effects the call command.
	public void writeCall(String functionName, int numArgs) {
		// Push return-address
		String returnAddress = makeUniqueTranslatorLabel();
		write("@" + returnAddress);
		write("D=A");
		write("@SP");
		write("A=M");
		write("M=D");
		write("@SP");
		write("M=M+1");

		// push LCL
		write("@LCL");
		write("D=M");
		write("@SP");
		write("A=M");
		write("M=D");
		write("@SP");
		write("M=M+1");

		// push ARG
		write("@ARG");
		write("D=M");
		write("@SP");
		write("A=M");
		write("M=D");
		write("@SP");
		write("M=M+1");

		// push THIS
		write("@THIS");
		write("D=M");
		write("@SP");
		write("A=M");
		write("M=D");
		write("@SP");
		write("M=M+1");

		// push THAT
		write("@THAT");
		write("D=M");
		write("@SP");
		write("A=M");
		write("M=D");
		write("@SP");
		write("M=M+1");

		// ARG = SP-n-5
		write("@SP");
		write("D=M");
		write("@" + numArgs + 5);
		write("D=D-A");
		write("@ARG");
		write("M=D");

		// LCL = SP
		write("@SP");
		write("D=M");
		write("@LCL");
		write("M=D");

		// goto function
		write("@" + functionName);
		write("0;JMP");

		// Declare a label for the return address
		write("(" + returnAddress + ")");
	}

	// Writes assembly code that effects the return command.
	public void writeReturn() {

		String tempSpot = "R15";

		// Store the value of LCL in a temporary register
		// "FRAME = LCL"
//		write("@LCL");
//		write("D=M");
//		write("@" + tempSpot);
//		write("M=D");

		// Put the return address in a temporary register
		write("@5");
		write("D=A");
		write("@LCL");
		write("A=M-D");
		write("D=M");
		write("@R13");
		write("M=D");

		writePushPop(Parser.VmCommand.C_POP, "argument", 0);

		// Restore SP of the caller
		write("@ARG");
		write("D=M+1");
		write("@SP");
		write("M=D");

		// Restore THAT of the caller
		write("@1");
		write("D=A");
		write("@LCL");
		write("A=M-D");
		write("D=M");
		write("@THAT");
		write("M=D");

		// Restore THIS of the caller
		write("@2");
		write("D=A");
		write("@LCL");
		write("A=M-D");
		write("D=M");
		write("@THIS");
		write("M=D");

		// Restore ARG of the caller
		write("@3");
		write("D=A");
		write("@LCL");
		write("A=M-D");
		write("D=M");
		write("@ARG");
		write("M=D");

		// Restore LCL of the caller
		write("@4");
		write("D=A");
		write("@LCL");
		write("A=M-D");
		write("D=M");
		write("@LCL");
		write("M=D");

		// Goto return-address
		write("@R13");
		write("A=M");
		write("0;JMP");
	}

	// Writes assembly code that effects the function command.
	public void writeFunction(String functionName, int numLocals) {
		this.functionName = functionName;

		write("(" + functionName + ")");

		// Initialize local variables to 0
		for (int i = 0; i < numLocals; i++)
			writePushPop(Parser.VmCommand.C_PUSH, "constant", 0);
	}

	// Writes to the Hack output file
	public void write(String content) {
		try {
			writer.newLine();
			writer.write(content);
		} catch (IOException e) {
			System.out.println("Failed to write to the Hack assembly output file.");
			e.printStackTrace();
		}
	}

	// Closes the output file.
	public void close() {
		try {
			writer.close();
		} catch (IOException e) {
			System.out.println("The output file was not able to be closed.");
			e.printStackTrace();
		}
	}

}
