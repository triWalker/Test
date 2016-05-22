package semanticActions;

import java.io.FileNotFoundException;
import java.lang.*;

import errors.*;
import java.util.*;

import lex.Token;
import lex.Tokenizer;
import lex.TokenType;
import symbolTable.SymbolTable;
import symbolTable.*;
import errors.SymbolTableError;


public class SemanticActions {

	private Stack<Object> semanticStack, parameters, nextParam ;
	private boolean insert ;
	private boolean isArray ;
	private boolean global ;
	private int globalMemory ;
	private int localMemory ;

	private SymbolTable globalTable ;
	private SymbolTable localTable ;


	private SymbolTable constantTable ;

	private int tableSize = 97;
	private boolean isParam;
	private SymbolTableEntry nullEntry = null;
	private Tokenizer tokenizer;

	private FunctionEntry currentFunction = null;

	private boolean readOrWrite = false;//helps determine whether current procedure is read or write, which will be handled differently

	private Quadruples quads;

	private int globalStore, localStore;

	private int tempCount = 1; //Variable will keep track of how many temps have been creatd to avoid duplicate variable error

	public SemanticActions(Tokenizer tokenizer) throws SymbolTableError {
		semanticStack = new Stack<Object>();
		parameters = new Stack<>(); // Will contain parameters and a count for a procedure or function
		nextParam = new Stack<>(); // Will be used to verify parameters are valid for a procedure or function to use
		insert = false;
		isArray = false;
		isParam = false;
		global = true;
		globalMemory = 0 ;
		localMemory = 0;
		globalTable = new SymbolTable(tableSize);
		localTable = new SymbolTable(tableSize);
		constantTable = new SymbolTable(tableSize);
		installBuiltins(globalTable);
		this.tokenizer = tokenizer;
		quads = new Quadruples();
	}

	public void execute(int actionNumber, Token token) throws SemanticError, SymbolTableError, FileNotFoundException {

		debug("calling action : " + actionNumber + " with token " + token.getType() + " with value " + token.getValue());

		// Commonly used variables
		String name, Etype, operatorValue, op;
		SymbolTableEntry entry, id, id1, id2, offset, temp, procOrFuncEntry, paramTester;
		ProcedureEntry procEntry;
		FunctionEntry funcEntry;
		VariableEntry funcResult;
		TokenType type;
		int result, beginLoop,count,upperBound, lowerBound, mSize;
		boolean stop;
		ArrayList<Integer> Etrue, Efalse, skipElse;
		ArrayList<SymbolTableEntry> params, orderedParams;
		int paramCount = 0;

		switch (actionNumber) {
			case 1:
				//Will be inserting new entries soon
				insert = true;
				break;

			case 2:
				//Finished inserting entries
				insert = false;
				break;

			case 3:
				stop = false; //Will be true when there are no more elements to enter

				type = (TokenType) semanticStack.pop();
				dumpstack(true);

				if(isArray)
				{
					// Inserting new array entries
					upperBound = (int) semanticStack.pop();
					lowerBound = (int) semanticStack.pop();
					mSize = (upperBound - lowerBound) + 1;

					//Get the first id
					name = (String) semanticStack.pop();

					//Loop to create an entry for each id currently on the stack
					while (!stop) {
						if (global)
						{
							//Global Case

							//Check if variable name is one of the reserved/keywords
							if (name.toUpperCase().equals("MAIN") || name.toUpperCase().equals("READ") || name.toUpperCase().equals("WRITE") ||
									name.toUpperCase().equals("INPUT") || name.toUpperCase().equals("OUTPUT")) {
								throw SemanticError.ReservedName(name, tokenizer.getLineNumber());
							}
							//Check if variable is already in global table
							else if (globalTable.lookup(name) != null)
							{
								//variable is already in global table
								throw SemanticError.MultipleDeclaration(name, tokenizer.getLineNumber());
							}
							else
							{
								//Inserting into global table
								int address = globalMemory;
								globalMemory = globalMemory + mSize;
								globalTable.insert(new ArrayEntry(name, address, type, upperBound, lowerBound));
							}
						}
						else
						{
							//Local Case

							//Check if variable is already in local table
							if (localTable.lookup(name) != null)
							{
								//variable is already in local table
								throw SemanticError.MultipleDeclaration(name, tokenizer.getLineNumber());
							}
							else {
								//Inserting into local table
								int address = localMemory;
								localMemory = localMemory + mSize;
								localTable.insert(new ArrayEntry(name, address, type, upperBound, lowerBound));
							}
						}

						//Check if stack is empty
						if (!semanticStack.empty()) {
							//Procedure or Function entry may be on stack. It should not be reinserted
							Object element = semanticStack.pop();
							if(element instanceof SymbolTableEntry)
							{
								//All new entries have been inserted at this point. Stop the loop. Push element back
								stop = true;
								semanticStack.push(element);
							}
							else {
								//Another entry must be inserted
								name = (String) element;
							}
						}
						else
						{
							//Stack is empty, loop will stop
							stop = true;
						}
					}
				}
				else
				{
					//New entries will not be array entries. They will be variable entries

					//Get the first id
					name = (String) semanticStack.pop();

					//Loop to create an entry for each id currently on the stack
					while (!stop) {
						if (global)
						{
							//Global Case

							//Check if variable name is one of the reserved/keywords
							if (name.toUpperCase().equals("MAIN") || name.toUpperCase().equals("READ") || name.toUpperCase().equals("WRITE") ||
									name.toUpperCase().equals("INPUT") || name.toUpperCase().equals("OUTPUT")) {
								throw SemanticError.ReservedName(name, tokenizer.getLineNumber());
							}
							//Check if variable is already in the global table
							else if (globalTable.lookup(name) != null) {
								//variable is already in table
								throw SemanticError.MultipleDeclaration(name, tokenizer.getLineNumber());
							}
							else {
								//Inserting into global table
								int address = globalMemory;
								globalMemory++;
								globalTable.insert(new VariableEntry(name, address, type));
							}
						}
						else {
							//Local Case

							//Check if variable is already in the local table
							if (localTable.lookup(name) != null) {
								//variable is already in table
								throw SemanticError.MultipleDeclaration(name, tokenizer.getLineNumber());
							} else {
								//Entering into local table
								int address = localMemory;
								localMemory = localMemory + 1;
								localTable.insert(new VariableEntry(name, address, type));
							}
						}

						//Check if stack is empty
						if (!semanticStack.empty()) {
							//Procedure or Function entry may be on stack. These should not be reinserted
							Object element = semanticStack.pop();
							if(element instanceof SymbolTableEntry) {
								//All new entries have been inserted at this point. Stop the loop. Push element back
								stop = true;
								semanticStack.push(element);
							}
							else {
								//Another entry must be inserted
								name = (String) element;
							}
						}
						else
						{
							//Stack is empty, loop will stop
							stop = true;
						}
					}
				}

				//Next entry may not be an array
				isArray = false;
				break;

			case 4:
				//Push Token Type
				semanticStack.push(token.getType());
				dumpstack(true);
				break;

			case 5:
				dumpstack(true);

				//Starting procedure
				insert = false;
				id = (SymbolTableEntry) semanticStack.pop();
				gen("PROCBEGIN", id);
				localStore = quads.getNextQuad();
				gen("alloc", "_");
				break;

			case 6:
				// A new array entry will be inserted soon
				isArray = true;
				break;

			case 7:
				//Pushing a constant value

				if (token.getType().equals(TokenType.INTCONSTANT)) {
					// Constant is an integer

					// Gets integer value of the string held by the token
					semanticStack.push(Integer.valueOf(token.getValue()));
				} else {
					// Constant is a real, but not an integer

					// Gets double(real) value out of the string held by the token
					semanticStack.push(Double.valueOf(token.getValue()));
				}
				break;

			case 9:
				count = 0; // Will be used to ensure only the first two ids are entered as IODevice entries
				stop = false; // Will be used to stop loop

				//Get first id
				name = (String) semanticStack.pop();

				//Loop to create new entries for each id on stack
				while (!stop) {

					// Only first two ids will become IO devices. The rest will become procedures
					if (count < 2)
					{
						//Create new IODevice and set it to reserved, then insert into the table
						IODeviceEntry io = new IODeviceEntry(name);
						io.setReserved(true);
						globalTable.insert(io);
						count++;
					}
					else {// Finished inserting IODevice entries

						//Creating reserved procedure entry. Set it to reserved, then insert into the table
						procEntry = new ProcedureEntry(name, 0);
						procEntry.setReserved(true);
						globalTable.insert(procEntry);
					}

					//Check if stack is empty
					if (!semanticStack.isEmpty()) {
						//Another entry must be inserted
						name = (String) semanticStack.pop();
					}
					else {
						//Stack is empty. Loop will stop
						stop = true;
					}
				}

				//Finished inserting
				insert = false;

				//Generate tvi code
				//gen("CODE");
				gen("call", globalTable.lookup("main"), 0);
				gen("exit");

				break;

			case 11:
				// Finished with local table
				global = true;

				// "Delete" current local table
				localTable = new SymbolTable(tableSize);

				//Current function is no longer executing
				currentFunction = null;

				//Fill in empty target space
				backPatch(localStore, localMemory);

				//Generate tvi code
				gen("free", localMemory);
				gen("PROCEND");
				break;

			case 13:
				//Push name of identifier, which will become a new entry into the table
				semanticStack.push(token.getValue());
				dumpstack(true);
				break;

			case 15:
				//Create a new function entry, and insert into the global table
				name = token.getValue();
				funcEntry = new FunctionEntry(name);
				funcResult = create(name, TokenType.INTEGER);
				funcResult.setAsFuncResult(true);
				funcEntry.setResult(funcResult);
				globalTable.insert(funcEntry);

				// New entries will be inserted into a local table
				global = false;
				localMemory = 0;

				//Push new function entry onto the stack
				semanticStack.push(funcEntry);
				dumpstack(true);
				break;

			case 16:
				//Set the function's result as type recently popped off of the stack
				type = (TokenType) semanticStack.pop();
				dumpstack(true);
				funcEntry = (FunctionEntry) semanticStack.pop();
				funcEntry.setResultType(type);

				//Set Current Function
				currentFunction = funcEntry;

				//Push function onto the stack
				semanticStack.push(funcEntry);
				break;

			case 17:
				//Create new procedure entry, and insert into the global table
				name = token.getValue();
				procEntry = new ProcedureEntry(name, 0);
				globalTable.insert(procEntry);

				//Push new procedure onto the stack
				semanticStack.push(procEntry);

				//New entries will be inserted into a local table
				global = false;
				localMemory = 0;
				break;

			case 19:
				// Start a parameter count from 0. Count will increase as parameters  are declared
				paramCount = 0;

				//Push parameter count onto the stack
				semanticStack.push(paramCount);
				break;

			case 20:
				//Set entry's number of parameters

				//Get parameter count and function/procedure off of the stack
				paramCount = (int) semanticStack.pop();
				procOrFuncEntry = (SymbolTableEntry) semanticStack.pop();

				//Check if entry is a function or procedure
				if(procOrFuncEntry.isProcedure())
				{
					//Procedure Entry
					procEntry = (ProcedureEntry) procOrFuncEntry;
					procEntry.setNumberOfParameters(paramCount);
					semanticStack.push(procEntry);
				}
				else
				{
					//Recently popped element must have been a function
					funcEntry = (FunctionEntry) procOrFuncEntry;
					funcEntry.setNumberOfParameters(paramCount);
					semanticStack.push(funcEntry);
				}

				break;

			case 21:
				//Create next two variables since the official parameter count and the function/procedure with the list
				//of parameter info are at the bottom of the stack, under the parameters to be declared

				//Start a count that will increase for each newly declared variable
				count = 0;
				// New list to hold parameters, until all have been inserted into the local table
				// Then, its elements will be passed in reverse to the function or procedure entry's parameter info
				params = new ArrayList<>();

				// Will be used to  end the loop
				stop = false;

				//Get type of new entries
				type = (TokenType) semanticStack.pop();

				// Loop until all parameters have been declared, and only the official parameter count and
				// procedure/function remain on the stack
				while(!stop) {
					//Check if new entries are arrays
					if (isArray) {
						//Array Entry

						//Get Array Information
						upperBound = (int) semanticStack.pop();
						lowerBound = (int) semanticStack.pop();
						name = (String) semanticStack.pop();

						//Check if parameter name is already taken
						if(localTable.lookup(name) != null)
							throw SemanticError.MultipleDeclaration(name, tokenizer.getLineNumber());
						else {
							//Inserting new array

							//Set new entry as a parameter and insert it into the local table
							ArrayEntry arrEntry = new ArrayEntry(name, localMemory, type, upperBound, lowerBound);
							arrEntry.setAsParam(true);
							params.add(arrEntry);
							localTable.insert(arrEntry);
						}
					} else {
						//Variable Entry

						//Get Variable Name
						name = (String) semanticStack.pop();

						//Check if parameter name is already taken
						if(localTable.lookup(name) != null)
							throw SemanticError.MultipleDeclaration(name, tokenizer.getLineNumber());
						else {
							//Inserting new variable

							//Set new entry as a parameter and insert it into the local table
							VariableEntry varEntry = new VariableEntry(name, localMemory, type);
							varEntry.setAsParam(true);
							params.add(varEntry);
							localTable.insert(varEntry);
						}
					}

					//Increment the memory and count per newly inserted entry
					localMemory++;
					count++;

					//Check if all parameters have been declared. If so, parameter count will be on top of the stack
					Object element = semanticStack.pop();
					if (element instanceof Integer) {
						//All parameters have been popped and current element is the paramCount integer,
						// followed by procedure/function entry
						paramCount = (int) element;
						dumpstack(true);
						entry = (SymbolTableEntry) semanticStack.pop();

						//Check if entry is a function or procedure
						if(entry.isProcedure()) {
							//Procedure Entry

							procEntry = (ProcedureEntry) entry;
							// Add parameters to procedure's parameter info, in reverse. Ex. j,i will be current order.
							// Official order should be i,j. This should help later on when param info is pushed onto
							// stack backwards. That way, i will be on top of the stack, and match with first parameter
							// to be validated later.
							orderedParams = new ArrayList<>();
							for(int i = params.size() - 1; i>= 0; i--)
							{
								SymbolTableEntry parm = params.get(i);
								orderedParams.add(parm);
							}
							procEntry.addParamInfo(orderedParams);

							semanticStack.push(procEntry);
						}
						else {
							//Function Entry

							funcEntry = (FunctionEntry) entry;
							//  Add parameters to function's parameter info, in reverse. Ex. j,i will be current order.
							// Official order should be i,j. This should help later on when param info is pushed onto
							// stack backwards. That way, i will be on top of the stack, and match with first parameter
							// to be checked later.
							orderedParams = new ArrayList<>();
							for (int i = params.size() - 1; i >= 0; i--) {
								SymbolTableEntry parm = params.get(i);
								orderedParams.add(parm);
							}
							funcEntry.addParamInfo(orderedParams);
							System.out.println("Current parameters  in case 21: " + funcEntry.parameterInfo);

							semanticStack.push(funcEntry);
						}
						//Add the count of new parameters to the total number of parameters
						paramCount = paramCount + count;
						//End the loop
						stop = true;
					}
					//Element was not the parameter count. So, there are more ids to pop. Push element back
					else
						semanticStack.push(element);
				}

				//Push official parameter count onto stack
				semanticStack.push(paramCount);
				//New insertions may not be arrays
				isArray = false;
				break;

			case 22:
				dumpstack(true);
				//Get Expression Type
				Etype = (String) semanticStack.pop();

				// Check if Etype is relational. If not, an error will occur
				if(!Etype.equals("RELATIONAL"))
				{
					throw SemanticError.ETypeMismatch(tokenizer.getLineNumber());
				}
				else
				{
					//Fill in target addresses, using values from Etrue list
					Efalse = (ArrayList) semanticStack.pop();
					Etrue = (ArrayList) semanticStack.pop();
					backPatch(Etrue, quads.getNextQuad());

					//Push lists back onto the stack
					semanticStack.push(Etrue);
					semanticStack.push(Efalse);
				}
				break;

			case 24:
				//A loop is being defined. Save its starting position
				beginLoop = quads.getNextQuad();

				//Push it onto the stack
				semanticStack.push(beginLoop);
				break;

			case 25:
				//Get Expression Type
				Etype = (String) semanticStack.pop();

				//Check if Etype is relational. If not, an error will occur
				if(!Etype.equals("RELATIONAL"))
				{
					throw SemanticError.ETypeMismatch(tokenizer.getLineNumber());
				}
				else
				{
					//Fill in target addresses, using values from Etrue list
					Efalse = (ArrayList) semanticStack.pop();
					Etrue = (ArrayList) semanticStack.pop();
					backPatch(Etrue, quads.getNextQuad());

					//Push lists back. They will be used again later
					semanticStack.push(Etrue);
					semanticStack.push(Efalse);
				}
				break;

			case 26:
				dumpstack(true);
				// Get lists and integer off of the stack
				Efalse = (ArrayList) semanticStack.pop();
				Etrue =  (ArrayList) semanticStack.pop();
				beginLoop = (int) semanticStack.pop();

				//Generate tvi code
				gen("goto", beginLoop);

				//Fill in target addresses, using values in Efalse list
				backPatch(Efalse, quads.getNextQuad());

				//Push Efalse and Etrue back onto the stack. May be used again later
				semanticStack.push(Etrue);
				semanticStack.push(Efalse);
				break;

			case 27:
				//Create a new list for skipElse
				skipElse = makeList(quads.getNextQuad());

				//Generate tvi code
				gen("goto", "_");

				//Get Efalse list
				Efalse = (ArrayList) semanticStack.pop();

				//Fill in target addresses, using values in Efalse list
				backPatch(Efalse, quads.getNextQuad());

				//Push both lists onto the stack
				semanticStack.push(Efalse);
				semanticStack.push(skipElse);
				break;

			case 28:
				//Set of expression lists should be popped off of the stack
				skipElse = (ArrayList) semanticStack.pop();
				Efalse = (ArrayList) semanticStack.pop();
				Etrue = (ArrayList) semanticStack.pop();

				//Fill in target addresses, using values in skipElse
				backPatch(skipElse, quads.getNextQuad());

				break;

			case 29:
				//Set of expression lists should be popped off of the stack
				Efalse = (ArrayList) semanticStack.pop();
				Etrue = (ArrayList) semanticStack.pop();

				//Fill in target addresses, using values in Efalse list
				backPatch(Efalse, quads.getNextQuad());

				break;

			case 30:
				//Get id's name
				name = token.getValue();

				//Check local table first
				if (localTable.lookup(token.getValue()) == null) {
					//Check global Table
					if (globalTable.lookup(token.getValue()) == null)
						throw SemanticError.UndeclaredVariable(name, tokenizer.getLineNumber());
					else {
						//Found in globalTable
						// Push entry and Etype
						semanticStack.push(globalTable.lookup(name));
						semanticStack.push("ARITHMETIC");
						dumpstack(true);

						//Check if the read or write procedure is being called
						if(name.toUpperCase().equals("READ") || name.toUpperCase().equals("WRITE")) {
							//Set boolean to true to help determine which course of action to take later
							readOrWrite = true;
						}
					}
				}
				else {
					//Found in local table
					//Push entry and Etype
					semanticStack.push(localTable.lookup(name));
					semanticStack.push("ARITHMETIC");

					//Do not need to check for read/write here. They won't be found in a local table
				}
				break;

			case 31:
				dumpstack(true);
				//Get Expression Type
				Etype = (String) semanticStack.pop();
				//Get second operand, which will be popped of the stack before the first operand
				id2 = (SymbolTableEntry) semanticStack.pop();
				//Get the value of offset, which may be null
				offset = (SymbolTableEntry) semanticStack.pop();
				//Get first operand
				id1 = (SymbolTableEntry) semanticStack.pop();
				//Get result of the two operands' types
				result = typeCheck(id1, id2);

				// Check if expression is arithmetic
				if (!Etype.equals("ARITHMETIC"))
				{
					throw SemanticError.ETypeMismatch(tokenizer.getLineNumber());
				} else if (result == 3) {
					//Cannot assign a real to an integer value
					throw SemanticError.TypeMismatch(tokenizer.getLineNumber());
				} else if (result == 2) {

					//Create temporary variable
					temp = create("TEMP" + tempCount, TokenType.REAL);
					tempCount++;

					//Convert second operand to a "real" number
					gen("ltof", id2, temp);

					//Check if offset is null
					if (offset == null) {
						//Offset is null
						gen("move", temp, id1);
					} else {
						//Offset is not null. Store value at offset position in first operand, which must be an array
						gen("stor", temp, offset, id1);
					}
				}
				//Can safely assign value at this point

				//Check if offset is null
				else if (offset == null) {
					//Offset is null, so assign value to variable
					gen("move", id2, id1);
				}
				//Offset is not null
				else {
					//Check if offset is "real"
					if(offset.getType().equals(TokenType.REAL))
						//Subscript/Offset values must be integers
						throw SemanticError.InvalidSubscript(tokenizer.getLineNumber());
					else
					//Offset is an integer
						gen("stor", id2, offset, id1);
				}

				dumpstack(true);
				break;

			case 32:
				Etype = (String) semanticStack.pop();

				// Check if Etype is Arithmetic
				if(!Etype.equals("ARITHMETIC"))
					throw SemanticError.ETypeMismatch(tokenizer.getLineNumber());

				// Pop entry off the stack
				id = (SymbolTableEntry) semanticStack.pop();

				//Make sure the recently popped entry is an array entry
				if(!id.isArray())
				{
					throw SemanticError.NotArray(tokenizer.getLineNumber());
				}

				// Push the entry(id) to be used again later
				semanticStack.push(id);
				break;

			case 33:
				Etype = (String) semanticStack.pop();

				//Make sure Etype is arithmetic
				if(!Etype.equals("ARITHMETIC"))
				{ throw SemanticError.ETypeMismatch(tokenizer.getLineNumber());}
				else {
					//Pop entry off of the stack
					id = (SymbolTableEntry) semanticStack.pop();

					//Make sure the recently popped entry is a constant entry with integer as its type
					if (!id.getType().equals(TokenType.INTEGER)) {
						throw SemanticError.TypeMismatch(tokenizer.getLineNumber());
					}

					//Create temporary variable
					temp = create("TEMP" + tempCount, TokenType.INTEGER);
					tempCount++;

					dumpstack(true);
					//Entry on top of the stack will be an array. Need to use its lower bound value
					ArrayEntry array = (ArrayEntry) semanticStack.pop();

					//Generate tvi code
					gen("sub", id, array.getLBound(), temp);

					//Push array and temporary variable onto the stack
					semanticStack.push(array);
					semanticStack.push(temp);
				}
				break;

			case 34:
				dumpstack(true);

				Etype = (String) semanticStack.pop();
				id = (SymbolTableEntry) semanticStack.pop();

				//Check if popped entry is a function
				if(id.isFunction()) {
					//Push Etype and entry back onto the stack and execute action 52
					semanticStack.push(id);
					semanticStack.push(Etype);
					execute(52, token);
				}
				else {
					//Push entry and null(Value of Offset) onto the stack
					semanticStack.push(id);
					semanticStack.push(nullEntry);
				}

				break;

			case 35:
				//New stack will save parameters after each has been validated. Will have total count of them on top.
				parameters = new Stack();
				parameters.push(0);

				//New stack will be used to validate each parameter being passed to procedure
				nextParam = new Stack();

				//Push elements from parameter info list onto next parameter stack in reverse order.
				// So, the first element popped off of next parameter stack will match with the first parameter
				// used in procedure call
				procEntry = (ProcedureEntry) semanticStack.pop();
				for(int i = procEntry.parameterInfo.size() - 1; i >= 0; i--)
				{
					SymbolTableEntry info = procEntry.parameterInfo.get(i);
					nextParam.push(info);
					System.out.println("Contents of nextParameter stack are " + nextParam.toString());
				}
				semanticStack.push(procEntry);
				System.out.println("Contents of parameter stack are " + parameters.toString());
				break;

			case 36:
				Etype = (String) semanticStack.pop();

				//Procedure will be on stack
				procEntry = (ProcedureEntry) semanticStack.pop();

				//Procedure should need 0 parameters. If not, an error will occur
				if(procEntry.getNumberOfParameters() != 0)
					throw SemanticError.WrongNumParameters(procEntry.getName(), tokenizer.getLineNumber());
				else
					//Generate tvi code
					gen("call", procEntry, 0);
				break;

			case 37:
				dumpstack(true);
				Etype = (String) semanticStack.pop();

				//Get next parameter for validation
				id = (SymbolTableEntry) semanticStack.pop();
				System.out.println("Contents of parameter stack are " + parameters.toString());

				//Count of parameters will be on top, followed by valid parameters
				paramCount = (int) parameters.pop();

				//Check if Etype is Arithmetic. If not, an error will occur
				if(!Etype.equals("ARITHMETIC")) {
					throw SemanticError.ETypeMismatch(tokenizer.getLineNumber());
				}
				//Check parameter entry type. An error will occur if parameter is not one of the four below.
				else if(!(id.isVariable() || id.isConstant() || id.isFunctionResult() || id.isArray()))
				{
					System.out.println("ID FAILED ONE OF THE TESTS in case 37");
					throw SemanticError.BadParmType(tokenizer.getLineNumber());
				}

				if(readOrWrite)
				{
					//Handling "read" or "write" procedure

					//Push valid parameter and  count onto parameters stack
					System.out.println("In reading or writing case");
					//will be the procedure read or write
					//Dont need to pop procedure, just add parameters to stack
					//procEntry = (ProcedureEntry) semanticStack.pop();
					System.out.println("Pushing parameter: " + id);
					parameters.push(id);
					parameters.push(paramCount);
				}
				else {
					// Check if program is handling a procedure or a function, not "read" or "write"
					procOrFuncEntry = (SymbolTableEntry) semanticStack.pop();

					//Checking that parameters are valid for the called function or procedure

					//Check if call is a function or a procedure
					if (procOrFuncEntry.isFunction()) {
						//Handling a Function
						System.out.println("Contents of nextParameter stack are " + nextParam.toString());

						// Get parameter off of next parameter stack. It will be used to test for parameter's validity
						paramTester = (SymbolTableEntry) nextParam.pop();

						System.out.println("Case 37 is working with a function");
						funcEntry = (FunctionEntry) procOrFuncEntry;

						//Increment parameter count
						paramCount++;

						//If too many parameters have been passed, an error will occur
						if (paramCount > funcEntry.getNumberOfParameters()) {
							throw SemanticError.WrongNumberParms(funcEntry.getName(), tokenizer.getLineNumber());
						}
						//If the recently passed parameter doesn't match the expected type, an error will occur
						else if (!id.getType().equals(paramTester.getType())) {
							throw SemanticError.BadParmType(tokenizer.getLineNumber());
						}
						//Check if parameter is an array
						else if (paramTester.isArray() & id.isArray()) {
							ArrayEntry idCheck = (ArrayEntry) id;
							ArrayEntry arrayCheck = (ArrayEntry) paramTester;
							//If array details are not as expected, an error will occur
							if (idCheck.getUBound() != arrayCheck.getUBound() || (idCheck.getLBound() != arrayCheck.getLBound())) {
								throw SemanticError.BadArrayBounds(tokenizer.getLineNumber());
							}
							else
							{
								//Add valid array parameter to parameterStack. Count stays on top
								parameters.push(id);
								parameters.push(paramCount);
							}
						}
						//Check if parameters is expected to not be an array
						else if (!paramTester.isArray() & !id.isArray())
						{
							// Parameter is valid and not an array
							parameters.push(id);
							parameters.push(paramCount);
						}
						//An error will occur at this point. The parameter does not have the expected parameter details
						else
							throw SemanticError.InvalidParameter(paramTester, id, tokenizer.getLineNumber());

						//Push function entry onto the stack
						semanticStack.push(funcEntry);
					}
					//Must be handling a procedure
					else
					{
						System.out.println("Contents of nextParameter stack are " + nextParam.toString());
						// Get parameter off of next parameter stack. It will be used to test for parameter's validity
						paramTester = (SymbolTableEntry) nextParam.pop();

						System.out.println("Case 37 is working with a procedure");
						procEntry = (ProcedureEntry) procOrFuncEntry;

						//Make sure procedure is not "read" or "write"
						if (!(procEntry.getName().equals("WRITE") || procEntry.getName().equals("READ"))) {

							//Increment parameter count
							paramCount++;
							//If too many parameters have been passed, an error will occur
							if (paramCount > procEntry.getNumberOfParameters()) {
								throw SemanticError.WrongNumberParms(procEntry.getName(), tokenizer.getLineNumber());
							}
							//If the recently passed parameter doesn't match the expected type, an error will occur
							else if (!id.getType().equals(paramTester.getType())) {
								throw SemanticError.BadParmType(tokenizer.getLineNumber());
							}
							//Check if parameter is an array
							else if (paramTester.isArray() & id.isArray()) {
								ArrayEntry idCheck = (ArrayEntry) id;
								ArrayEntry arrayCheck = (ArrayEntry) paramTester;
								//If array details are not as expected, an error will occur
								if (idCheck.getUBound() != arrayCheck.getUBound() || (idCheck.getLBound() != arrayCheck.getLBound())) {
									throw SemanticError.BadArrayBounds(tokenizer.getLineNumber());
								} else
								{
									//Add valid array parameter to parameterStack. Count stays on top
									parameters.push(id);
									parameters.push(paramCount);
								}
							}
							//Check if parameters is expected to not be an array
							else if (!paramTester.isArray() & !id.isArray())
							{
								// Parameter is valid and not an array
								parameters.push(id);
								parameters.push(paramCount);
							}
							//An error will occur at this point. The parameter does not have the expected parameter details
							else
								throw SemanticError.BadParmType(tokenizer.getLineNumber());

							//Push procedure entry
							semanticStack.push(procEntry);
						}
					}
				}

				break;


			case 38:
				Etype = (String) semanticStack.pop();

				//Check if Etype is Arithmetic. If not, an error will occur
				if(!Etype.equals("ARITHMETIC"))
				{
					throw SemanticError.ETypeMismatch(tokenizer.getLineNumber());
				}

				//Push operator
				operatorValue = token.getValue();
				if (operatorValue.equals("1")) {
					//"Equality" Operator
					semanticStack.push("=");
				} else if (operatorValue.equals("2")) {
					//"Not Equal" Operator
					semanticStack.push("<>");
				}
				else if (operatorValue.equals("3")) {
					//Less Than Operator
					semanticStack.push("<");
				}
				else if (operatorValue.equals("4")) {
					//Greater Than Operator
					semanticStack.push(">");
				}
				else if (operatorValue.equals("5")) {
					//Less Than Or Equal Operator
					semanticStack.push("<=");
				}
				else if (operatorValue.equals("6")) {
					//Greater Than Or Equal Operator
					semanticStack.push(">=");
				}
				else //Could not identify relop
					semanticStack.push("RELOP op not pushed properly");
				break;

			case 39:
				dumpstack(true);
				//Get expression type
				Etype = (String) semanticStack.pop();
				//Get second operand, which will be popped of the stack before the first operand
				id2 = (SymbolTableEntry) semanticStack.pop();
				//Get operator
				op = (String) semanticStack.pop();
				//Get first operand
				id1 = (SymbolTableEntry) semanticStack.pop();
				//Get result of the two operands' types
				result = typeCheck(id1, id2);

				//Check if Etype is Arithmetic. If not, an error will occur
				if (!Etype.equals("ARITHMETIC")) // Check if ARITHMETIC is on the stack
				{
					throw SemanticError.ETypeMismatch(tokenizer.getLineNumber());
				} else if (result == 2) {

					//Create temporary variable
					temp = create("TEMP" + tempCount, TokenType.REAL);
					tempCount++;

					gen("ltof", id2, temp);

					//gen call depends on operator
					if (op.equals(">")) {
						//Greater Than Case
						gen("bgt", id1, temp, "_");
					}
					if (op.equals(">=")) {
						//Greater Than Or Equal Case
						gen("bge", id1, temp, "_");
					} else if (op.equals("<")) {
						//Less Than Case
						gen("blt", id1, temp, "_");
					} else if (op.equals("<=")) {
						//Less Than Or Equal Case
						gen("ble", id1, temp, "_");
					} else if (op.equals("<>")) {
						//Not Equal Case
						gen("bne", id1, temp, "_");
					} else if (op.equals("=")) {
						//Equality Case
						gen("beq", id1, temp, "_");
					} else
						System.out.println("Operator Error");
				} else if (result == 3) {
					temp = create("TEMP" + tempCount, TokenType.REAL);
					tempCount++;

					gen("ltof", id1, temp);

					//gen call depends on the operator
					if (op.equals(">")) {
						//Greater Than Case
						gen("bgt", temp, id2, "_");
					}
					else if (op.equals(">=")) {
						//Greater Than Or Equal Case
						gen("bge", temp, id2, "_");
					} else if (op.equals("<")) {
						//Less Than Case
						gen("blt", temp, id2, "_");
					} else if (op.equals("<=")) {
						//Less Than Or Equal Case
						gen("ble", temp, id2, "_");
					} else if (op.equals("<>")) {
						//Not Equal Case
						gen("bne", temp, id2, "_");
					} else if (op.equals("=")) {
						//Equality Case
						gen("beq", temp, id2, "_");
					} else
						System.out.println("Operator Error");
				} else {
					//gen call depends on operator
					if (op.equals(">")) {
						//Greater Than Case
						gen("bgt", id1, id2, "_");
					}
					else if (op.equals(">=")) {
						//Greater Than Or Equal Case
						gen("bge", id1, id2, "_");
					} else if (op.equals("<")) {
						//Less Than Case
						gen("blt", id1, id2, "_");
					} else if (op.equals("<=")) {
						//Less Than Or Equal Case
						gen("ble", id1, id2, "_");
					} else if (op.equals("<>")) {
						// Not Equal Case
						gen("bne", id1, id2, "_");
					} else if (op.equals("=")) {
						//Equality Case
						gen("beq", id1, id2, "_");
					} else
						System.out.println("Operator Error");
				}

				//Generate tvi code
				gen("goto", "_");

				// Create new Etrue and Efalse lists associated with the result of the above relational operators
				Etrue = makeList(quads.getNextQuad() - 2);
				Efalse = makeList(quads.getNextQuad() - 1);

				//Push lists and Etype onto the stack
				semanticStack.push(Etrue);
				semanticStack.push(Efalse);
				semanticStack.push("RELATIONAL");
				dumpstack(true);
				break;

			case 40:
				//Type will be Unary Plus or Minus
				semanticStack.push(token.getType());
				break;

			case 41:
				dumpstack(true);
				Etype = (String) semanticStack.pop();
				id = (SymbolTableEntry) semanticStack.pop();
				TokenType sign = (TokenType) semanticStack.pop();

				//Check if sign is "-"
				if(sign.equals(TokenType.UNARYMINUS))
				{
					temp = create("TEMP" + tempCount, id.getType());
					tempCount++;

					//Check if id is an integer or real to determine which unary minus to use
					if(id.getType().equals(TokenType.INTEGER))
						gen("uminus", id, temp);
					else
						gen("fuminus", id, temp);

					semanticStack.push(temp);
				}
				else
				//Sign must be "+". Just push id back onto the stack
					semanticStack.push(id);

				semanticStack.push("ARITHMETIC");
				break;

			case 42:
				//Token will be an ADDOP

				Etype = (String) semanticStack.pop();

				//Check if Etype is relational
				if (Etype.equals("RELATIONAL"))
				{
					//Get operator value from token
					operatorValue = token.getValue();
					if(operatorValue.equals("OR")) {
						//Satisfy the conditions of "OR" operator
						Efalse = (ArrayList) semanticStack.pop();
						backPatch(Efalse, quads.getNextQuad());
						semanticStack.push(Efalse);
						semanticStack.push(operatorValue);
					}
					else //Did not find "OR"
						throw SemanticError.ExpectedConjunction(operatorValue, tokenizer.getLineNumber());
				}
				else if (!Etype.equals("ARITHMETIC")) {
					//Etype is neither relational nor arithmetic
					throw SemanticError.ETypeMismatch(tokenizer.getLineNumber());
				}
				else {
					//Get operator value and push it onto the stack
					operatorValue = token.getValue();
					if (operatorValue.equals("1")) {
						//Addition Operator
						semanticStack.push("+");
					} else
						//Subtraction Operator
						semanticStack.push("-");
				}
				break;

			case 43:
				dumpstack(true);
				Etype = (String) semanticStack.pop();

				//Check if Etype is relational
				if(Etype.equals("RELATIONAL"))
				{

					//Setup the Etrue and Efalse lists to satisfy the "or" condition
					ArrayList falseList2 = (ArrayList) semanticStack.pop();
					ArrayList trueList2 = (ArrayList) semanticStack.pop();

					//"OR" operator should be on stack, between two sets of expression lists
					operatorValue = (String) semanticStack.pop();

					ArrayList falseList1 = (ArrayList) semanticStack.pop();
					ArrayList trueList1 = (ArrayList) semanticStack.pop();

					Etrue = merge(trueList1, trueList2);
					Efalse = falseList2;

					//Push expression lists onto the stack
					semanticStack.push(Etrue);
					semanticStack.push(Efalse);
					//Push Relational after relational operation
					semanticStack.push("RELATIONAL");

				}
				else{
					//Etype must be Arithmetic at this point
					if (!Etype.equals("ARITHMETIC")) {
						throw SemanticError.ETypeMismatch(tokenizer.getLineNumber());
					}
					else {
						id2 = (SymbolTableEntry) semanticStack.pop();
						op = (String) semanticStack.pop();
						id1 = (SymbolTableEntry) semanticStack.pop();
						result = typeCheck(id1, id2);
						if (result == 0) {
							//Both ids(entries) have integer as their token type

							temp = create("TEMP" + tempCount, TokenType.INTEGER);
							tempCount++;

							//Gen call depend on the operator

							if (op.equals("+")) {
								//Integer Addition Case
								gen("add", id1, id2, temp);
								semanticStack.push(temp);
							} else if (op.equals("DIV")) {
								//Integer Division Case
								gen("div", id1, id2, temp);
								semanticStack.push(temp);
							} else if (op.equals("-")) {
								//Integer Subtraction Case
								gen("sub", id1, id2, temp);
								semanticStack.push(temp);
							} else {
								//Integer Multiplication Case
								gen("mul", id1, id2, temp);
								semanticStack.push(temp);
							}
						} else if (result == 1) {
							// ADD type check result specifics so this case can be understood at a glance
							//  Ex. 2 id is an integer and the first is real or something like that.
							temp = create("TEMP" + tempCount, TokenType.REAL);
							tempCount++;

							//gen call depends on operator
							if (op.equals("+")) {
								//"Real" Addition Case
								gen("fadd", id1, id2, temp);
								semanticStack.push(temp);
							} else if (op.equals("/")) {
								//"Real" Division Case
								gen("fdiv", id1, id2, temp);
								semanticStack.push(temp);
							} else if (op.equals("-")) {
								//"Real" Subtraction Case
								gen("fsub", id1, id2, temp);
								semanticStack.push(temp);
							} else {
								//"Real" Multiplication Case
								gen("fmul", id1, id2, temp);
								semanticStack.push(temp);
							}
						}
							else if (result == 2) {
							SymbolTableEntry temp1 = create("TEMP" + tempCount, TokenType.REAL);
							tempCount++;

							gen("ltof", id2, temp1);

							SymbolTableEntry temp2 = create("TEMP" + tempCount, TokenType.REAL);
							tempCount++;

							//gen call depends on operator

							if (op.equals("+")) {
								//"Real" Addition Case
								gen("fadd", id1, temp1, temp2);
								semanticStack.push(temp2);
							} else if (op.equals("-")) {
								//"Real" Subtraction Case
								gen("fsub", id1, temp1, temp2);
								semanticStack.push(temp2);
							} else if (op.equals("*")) {
								//"Real" Multiplication Case
								gen("fmul", id1, temp1, temp2);
								semanticStack.push(temp2);
							} else {
								//"Real" Division Case
								gen("fdiv", id1, temp1, temp2);
								semanticStack.push(temp2);
							}
						} else if (result == 3) {
							SymbolTableEntry temp1 = create("TEMP" + tempCount, TokenType.REAL);
							tempCount++;

							gen("ltof", id1, temp1);

							SymbolTableEntry temp2 = create("TEMP" + tempCount, TokenType.REAL);
							tempCount++;

							//gen call depends on operator

							if (op.equals("+")) {
								//"Real" Addition Case
								gen("fadd", temp1, id2, temp2);
								semanticStack.push(temp2);
							} else if (op.equals("-")) {
								//"Real" Subtraction Case
								gen("fsub", temp1, id2, temp2);
								semanticStack.push(temp2);
							} else if (op.equals("*")) {
								//"Real" Multiplication Case
								gen("fmul", temp1, id2, temp2);
								semanticStack.push(temp2);
							} else {
								//"Real" Division Case
								gen("fdiv", temp1, id2, temp2);
								semanticStack.push(temp2);
							}
						}
					}
					//Push Arithmetic, at the end of arithmetic operations
					semanticStack.push("ARITHMETIC");
				}

				break;

			case 44:
				Etype = (String) semanticStack.pop();

				//Check if Etype is relational
				if(Etype.equals("RELATIONAL"))
				{
					//Get operator value from token.
					operatorValue = token.getValue();
					if(operatorValue.equals("AND"))
					{
						//Satisfy the conditions of "AND" operator
						Efalse = (ArrayList) semanticStack.pop();
						Etrue = (ArrayList) semanticStack.pop();
						backPatch(Etrue, quads.getNextQuad());

						//Push expression lists onto the stack
						semanticStack.push(Etrue);
						semanticStack.push(Efalse);
						//Push "AND" onto the stack
						semanticStack.push(operatorValue);
					}
					else //Did not find "AND"
						throw SemanticError.ExpectedConjunction(operatorValue, tokenizer.getLineNumber());
				}
				//Etype must be arithmetic, otherwise an error will occur
				else if (!Etype.equals("ARITHMETIC")) {
					throw SemanticError.ETypeMismatch(tokenizer.getLineNumber());
				}
				else {
					//Get operator value and push it onto stack
					operatorValue = token.getValue();
					if (operatorValue.equals("1")) {
						//Multiplication Operator
						semanticStack.push("*");
					} else if (operatorValue.equals("2")) {
						//"Real" Division Operator
						semanticStack.push("/");
					} else if (operatorValue.equals("DIV")) {
						//Integer Division Operator
						semanticStack.push("DIV");
					} else
						//Modulo Operator
						semanticStack.push("MOD");
				}

				dumpstack(true);
				break;

			case 45:
				dumpstack(true);
				Etype = (String) semanticStack.pop();

				//Check if Etype is relational
				if(Etype.equals("RELATIONAL"))
				{
					//Setup the Etrue and Efalse lists to satisfy the "AND" condition

					ArrayList falseList2 = (ArrayList) semanticStack.pop();
					ArrayList trueList2 = (ArrayList) semanticStack.pop();

					//"AND" operator should be on stack, between two sets of expression lists
					operatorValue = (String) semanticStack.pop();
					System.out.println(operatorValue);

					ArrayList falseList1 = (ArrayList) semanticStack.pop();
					ArrayList trueList1 = (ArrayList) semanticStack.pop();

					Etrue = trueList2;
					Efalse = merge(falseList1, falseList2);

					//Push expression lists onto the stack
					semanticStack.push(Etrue);
					semanticStack.push(Efalse);
					//Push Relational after relational operation
					semanticStack.push("RELATIONAL");

				}
				else {
					//Etype should be arithmetic. If not, an error will occur
					if (!Etype.equals("ARITHMETIC")) {
						throw SemanticError.ETypeMismatch(tokenizer.getLineNumber());
					}
					else {

						id2 = (SymbolTableEntry) semanticStack.pop();
						op = (String) semanticStack.pop();
						id1 = (SymbolTableEntry) semanticStack.pop();
						result = typeCheck(id1, id2);
						if (result == 0) {
							// Both ids(entries) have integer types
							if (op.equals("MOD")){
								//Modulo case
								SymbolTableEntry temp1 = create("TEMP" + tempCount, TokenType.INTEGER);
								tempCount++;

								gen("move", id1, temp1);

								SymbolTableEntry temp2 = create("TEMP" + tempCount, TokenType.INTEGER);
								tempCount++;

								gen("move", temp1, temp2);

								gen("sub", temp2, id2, temp1);

								gen("bge", temp1, id2, quads.getNextQuad());

								semanticStack.push(temp1);
							}
							else if (op.equals("/")) {
								//Both entries are integers, but program wants a "real" number to be returned

								SymbolTableEntry temp1 = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								gen("ltof", id1, temp1);

								SymbolTableEntry temp2 = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								gen("ltof", id2, temp2);

								SymbolTableEntry temp3 = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								//"Real" Division
								gen("fdiv", temp1, temp2, temp3);
								semanticStack.push(temp3);
							}
							//gen calls will depend on operator
							else if (op.equals("+")) {
								temp = create("TEMP" + tempCount, TokenType.INTEGER);
								tempCount++;

								//Addition Case
								gen("add", id1, id2, temp);
								semanticStack.push(temp);
							} else if (op.equals("-")) {
								temp = create("TEMP" + tempCount, TokenType.INTEGER);
								tempCount++;

								//Subtraction Case
								gen("sub", id1, id2, temp);
								semanticStack.push(temp);
							} else if (op.equals("*")) {
								temp = create("TEMP" + tempCount, TokenType.INTEGER);
								tempCount++;

								//Multiplication Case
								gen("mul", id1, id2, temp);
								semanticStack.push(temp);
							} else {
								temp = create("TEMP" + tempCount, TokenType.INTEGER);
								tempCount++;

								//Integer Division Case
								gen("div", id1, id2, temp);
								semanticStack.push(temp);
							}
						} else if (result == 1) {
							if (op.equals("MOD")){
								//Ids are not both integer types. MOD requires integer operands
								throw SemanticError.BadMODoperands(tokenizer.getLineNumber());
							}
							else if (op.equals("DIV")) {
								SymbolTableEntry temp1 = create("TEMP" + tempCount, TokenType.INTEGER);
								tempCount++;

								gen("ftol", id1, temp1);

								SymbolTableEntry temp2 = create("TEMP" + tempCount, TokenType.INTEGER);
								tempCount++;

								gen("ftol", id2, temp2);

								SymbolTableEntry temp3 = create("TEMP" + tempCount, TokenType.INTEGER);
								tempCount++;

								//Integer Division Case
								gen("div", temp1, temp2, temp3);
								semanticStack.push(temp3);
							}
							//gen calls depend on operator
							else if (op.equals("+")) {
								temp = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								//"Real" Addition Case
								gen("fadd", id1, id2, temp);
								semanticStack.push(temp);
							} else if (op.equals("-")) {
								temp = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								//"Real" Subtraction Case
								gen("fsub", id1, id2, temp);
								semanticStack.push(temp);
							} else if (op.equals("*")) {
								temp = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								//"Real" Multiplication Case
								gen("fmul", id1, id2, temp);
								semanticStack.push(temp);
							} else {
								temp = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								//"Real" Division Case
								gen("fdiv", id1, id2, temp);
								semanticStack.push(temp);
							}
						} else if (result == 2) {
							if (op.equals("MOD")){
								//Ids are not both integer types. MOD requires integer operands
								throw SemanticError.BadMODoperands(tokenizer.getLineNumber());
							}
							else if (op.equals("DIV")) {
								SymbolTableEntry temp1 = create("TEMP" + tempCount, TokenType.INTEGER);
								tempCount++;

								gen("ftol", id1, temp1);

								SymbolTableEntry temp2 = create("TEMP" + tempCount, TokenType.INTEGER);
								tempCount++;

								//Integer Division Case
								gen("div", temp1, id2, temp2);
								semanticStack.push(temp2);
							}
							//gen call depends on operator
							else if (op.equals("+")) {
								SymbolTableEntry temp1 = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								gen("ltof", id2, temp1);

								SymbolTableEntry temp2 = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								//"Real" Addition Case
								gen("fadd", id1, temp1, temp2);
								semanticStack.push(temp2);
							} else if (op.equals("-")) {
								SymbolTableEntry temp1 = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								gen("ltof", id2, temp1);

								SymbolTableEntry temp2 = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								//"Real" Subtraction Case
								gen("fsub", id1, temp1, temp2);
								semanticStack.push(temp2);
							} else if (op.equals("*")) {
								SymbolTableEntry temp1 = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								gen("ltof", id2, temp1);

								SymbolTableEntry temp2 = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								//"Real" Multiplication Case
								gen("fmul", id1, temp1, temp2);
								semanticStack.push(temp2);
							} else {
								SymbolTableEntry temp1 = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								gen("ltof", id2, temp1);

								SymbolTableEntry temp2 = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								//"Real" Division Case
								gen("fdiv", id1, temp1, temp2);
								semanticStack.push(temp2);
							}
						} else if (result == 3) {
							if (op.equals("MOD")){
								//Ids are not both integer types. MOD requires integer operands
								throw SemanticError.BadMODoperands(tokenizer.getLineNumber());
							}
							else if (op.equals("DIV")) {
								SymbolTableEntry temp1 = create("TEMP" + tempCount, TokenType.INTEGER);
								tempCount++;

								gen("ftol", id2, temp1);

								SymbolTableEntry temp2 = create("TEMP" + tempCount, TokenType.INTEGER);
								tempCount++;

								//Integer Division Case
								gen("div", id1, temp1, temp2);
								semanticStack.push(temp2);
							} else if (op.equals("+")) {
								SymbolTableEntry temp1 = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								gen("ltof", id1, temp1);

								SymbolTableEntry temp2 = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								//"Real" Addition Case
								gen("fadd", temp1, id2, temp2);
								semanticStack.push(temp2);
							} else if (op.equals("-")) {
								SymbolTableEntry temp1 = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								gen("ltof", id1, temp1);

								SymbolTableEntry temp2 = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								//"Real" Subtraction Case
								gen("fsub", temp1, id2, temp2);
								semanticStack.push(temp2);
							} else if (op.equals("*")) {
								SymbolTableEntry temp1 = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								gen("ltof", id1, temp1);

								SymbolTableEntry temp2 = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								//"Real" Multiplication Case
								gen("fmul", temp1, id2, temp2);
								semanticStack.push(temp2);
							} else {
								SymbolTableEntry temp1 = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								gen("ltof", id1, temp1);

								SymbolTableEntry temp2 = create("TEMP" + tempCount, TokenType.REAL);
								tempCount++;

								//"Real" Division Case
								gen("fdiv", temp1, id2, temp2);
								semanticStack.push(temp2);
							}
						}
					}
					//Push Arithmetic after arithmetic operation
					semanticStack.push("ARITHMETIC");
				}

				dumpstack(true);
				break;

			case 46:
				System.out.println("Contents of parameter stack are " + parameters.toString());

				//Get token's information
				name = token.getValue();
				type = token.getType();

				System.out.println("" + type);

				//Check if token is identifier, should be in one of the tables if it has been declared previously
				if(type.equals(TokenType.IDENTIFIER))
				{
					System.out.println("Type equals identifier");
					//Check local table first for entry
					if (localTable.lookup(name) == null) {
						//Check global Table for entry
						System.out.println("Checking global");
						if (globalTable.lookup(name) == null)
							//Entry associated with id cannot be found. It must be undeclared
							throw SemanticError.UndeclaredVariable(name, tokenizer.getLineNumber());
						else {
							//Found in globalTable
							// Pushing entry associated with id, which will be used later
							semanticStack.push(globalTable.lookup(name));
							dumpstack(true);
						}
					}
					else {
						//Found in local table
						// Pushing entry associated with id, which will be used later
						semanticStack.push(localTable.lookup(name));
						dumpstack(true);
					}
				}
				// /token must be a constant
				else if(type.equals(TokenType.INTCONSTANT))
				{ // Dealing with an integer

					System.out.println("Type equals integer");
					// If constant entry doesn't exist for the constant, create one and insert it into constant table
					if (lookupConstant(token) == null)
					{
						entry = new ConstantEntry(name, TokenType.INTEGER);
						constantTable.insert(entry);
						System.out.println("Pushing int onto stack");
						semanticStack.push(entry);
					}
					else //Constant is already in constant table
						semanticStack.push(lookupConstant(token));
				}
				else if(type.equals(TokenType.REALCONSTANT))
				{ // Dealing with a "real" number
					System.out.println("Type equals real");
					// If constant entry doesn't exist for the constant, create one and insert it into constant table
					if (lookupConstant(token) == null)
					{
						entry = new ConstantEntry(name, TokenType.REAL);
						constantTable.insert(entry);
						semanticStack.push(entry);
					}
					else //Constant is already in constant table
						semanticStack.push(lookupConstant(token));
				}

				semanticStack.push("ARITHMETIC");
				dumpstack(true);
				System.out.println("Contents of parameter stack are " + parameters.toString());
				break;

			case 47:
				Etype = (String) semanticStack.pop();

				//Check if Etype is relational. If not, an error will occur
				if(!Etype.equals("RELATIONAL"))
				{
					throw SemanticError.ETypeMismatch(tokenizer.getLineNumber());
				}
				else
				{
					// This is the "NOT" case. Switch the values of the Etrue and Efalse lists.
					// Etrue should contain the info in Efalse, and
					// Efalse should contain the info in Etrue

					Efalse = (ArrayList) semanticStack.pop();
					Etrue = (ArrayList) semanticStack.pop();

					ArrayList trueList = new ArrayList();
					ArrayList falseList = new ArrayList();

					trueList.addAll(Etrue);
					falseList.addAll(Efalse);

					Etrue = falseList;
					Efalse = trueList;

					semanticStack.push(Etrue);
					semanticStack.push(Efalse);
					semanticStack.push("RELATIONAL");
				}

				break;

			case 48:

				offset = (SymbolTableEntry) semanticStack.pop();
				id1 = (SymbolTableEntry) semanticStack.pop();

				//Check if offset is not equal to null
				if(offset != null)
				{
					//offset is not null. It is either a Variable Entry or Constant Entry
					temp = create("TEMP" + tempCount,id1.getType());
					tempCount++;

					gen("load", id1, offset, temp);

					semanticStack.push(temp);
				}
				else
				{
					//Offset is equal to null
					//Push id back
					semanticStack.push(id1);
				}


				semanticStack.push("ARITHMETIC");
				dumpstack(true);
				System.out.println("Contents of parameter stack are " + parameters.toString());
				break;

			case 49:
				Etype = (String) semanticStack.pop();
				id = (SymbolTableEntry) semanticStack.pop();

				//Check if Etype is Arithmetic. If not, an error will occur
				if(!Etype.equals("ARITHMETIC"))
				{
					throw SemanticError.ETypeMismatch(tokenizer.getLineNumber());
				}
				//Check if id is a function. If not, an error will occur
				else if(!id.isFunction())
				{
					throw SemanticError.IllegalFunctionName(id.getName(), tokenizer.getLineNumber());
				}
				else
				{
					//New stack will save parameters after each has been validated. Will have count of them on top.
					parameters = new Stack();
					parameters.push(0);
					System.out.println("Contents of parameter stack are " + parameters.toString());
					//New stack will be used to validate each parameter being passed to function
					nextParam = new Stack();

					//Push elements from parameter info list onto next parameter stack in reverse order.
					// So, the first element popped off of next parameter stack will match with the first parameter
					// used in function call
					funcEntry = (FunctionEntry) id;
					for (int i = funcEntry.parameterInfo.size() - 1; i >= 0; i--)
					{
						SymbolTableEntry info = funcEntry.parameterInfo.get(i);
						nextParam.push(info);
						System.out.println("Contents of nextParameter stack are " + nextParam.toString());
					}
					semanticStack.push(funcEntry);
				}

				break;

			case 50:
				//Etype = (String) semanticStack.pop();
				funcEntry = (FunctionEntry) semanticStack.pop();
				System.out.println("Contents of parameter stack are " + parameters.toString());
				paramCount = (int) parameters.pop();

				//Check if parameters passed equals the number required for the function.
				if(paramCount != funcEntry.getNumberOfParameters())
				{
					throw SemanticError.WrongNumberParms(funcEntry.getName(), tokenizer.getLineNumber());
				}

				//Parameters on stack are in reverse, with the last parameter on top.
				// Pass parameters in order, from earliest to latest
				params = new ArrayList();
				while(!parameters.empty())
				{
					SymbolTableEntry element = (SymbolTableEntry) parameters.pop();
					params.add(element);
				}

				for(int i = params.size() - 1; i >= 0; i--)
				{
					gen("param",params.get(i));
					localMemory++;
				}

				gen("call", funcEntry, paramCount);
				temp = create("TEMP" + tempCount, funcEntry.getResultType());
				tempCount++;

				gen("move", funcEntry.getResult(), temp);

				semanticStack.push(temp);
				semanticStack.push("ARITHMETIC");
				break;

			case 51:
				//Get name of current procedure
				dumpstack(true);
				procEntry = (ProcedureEntry) semanticStack.pop();
				name = procEntry.getName();
				System.out.println("Contents of parameter stack are " + parameters.toString());
				//Get count of validated parameters
				paramCount = (int) parameters.pop();

				//Check if "write" procedure was called
				if(name.equals("WRITE"))
				{
					//Write Procedure

					//Parameters on stack are in reverse, with the last parameter on top.
					// Print the parameters in order, from earliest to latest
					params = new ArrayList<>();
					while(!parameters.empty())
					{
						SymbolTableEntry element = (SymbolTableEntry) parameters.pop();
						params.add(element);
					}

					for(int i = params.size() - 1; i >= 0; i--)
					{
						id = params.get(i);
						gen("print", id.getName() + " =");
						if(id.getType().equals(TokenType.REAL))
							gen("foutp", id);
						else
							gen("outp", id);

						gen("newl");
					}
					//Finished writing. Set boolean to false
					readOrWrite = false;
				}
				//Check if "read" procedure was called
				else if(name.equals("READ"))
				{
					//Read Procedure

					//Parameters on stack are in reverse, with the last parameter on top.
					// Read in each parameter in order, from earliest to latest
					params = new ArrayList<>();
					while(!parameters.empty())
					{
						SymbolTableEntry element = (SymbolTableEntry) parameters.pop();
						params.add(element);
					}

					for(int i = params.size() - 1; i >= 0; i--)
					{
						id = params.get(i);
						if(id.getType().equals(TokenType.REAL))
							gen("finp", id);
						else
							gen("inp", id);
					}
					//Finished reading, set boolean to false
					readOrWrite = false;
				}
				// Procedure is neither "read" nor "write"
				else
				{
					//Check if parameter count are equal. If not, an error will occur
					if(paramCount != procEntry.getNumberOfParameters())
					{
						throw SemanticError.WrongNumberParms(name, tokenizer.getLineNumber());
					}

					//Parameters on stack are in reverse, with the last parameter on top.
					// Pass parameters in order, from earliest to latest
					params = new ArrayList();
					while(!parameters.empty())
					{
						SymbolTableEntry element = (SymbolTableEntry) parameters.pop();
						params.add(element);
					}

					for(int i = params.size() - 1; i >= 0; i--)
					{
						gen("param",params.get(i));
						localMemory++;
					}
				}

				gen("call", procEntry, paramCount);

				break;

			case 52:
				Etype = (String) semanticStack.pop();
				id = (SymbolTableEntry) semanticStack.pop();

				//Check if id is a function. If not, an error will occur
				if(!id.isFunction())
				{
					throw SemanticError.IllegalFunctionName(id.getName(), tokenizer.getLineNumber());
				}

				funcEntry = (FunctionEntry) id;
				System.out.println("52, number of parameters for function is " + funcEntry.getNumberOfParameters());
				//Function should need 0 parameters. Otherwise, an error will occur
				if(funcEntry.getNumberOfParameters() > 0)
					throw SemanticError.WrongNumParameters(funcEntry.getName(), tokenizer.getLineNumber());

				gen("call", funcEntry, 0);
				temp = create("TEMP" + tempCount, funcEntry.getResultType()); // Or have getType that returns result type
				tempCount++;

				gen("move", funcEntry.getResult(), temp);

				semanticStack.push(temp);
				semanticStack.push("ARITHMETIC");
				break;

			case 53:
				Etype = (String) semanticStack.pop();
				id = (SymbolTableEntry) semanticStack.pop();

				//Check if recently popped entry is a function
				if(id.isFunction()) {
					//If entry is a function, it must be the current function. Otherwise, an error will occur.
					if (!id.equals(currentFunction)) {
						throw SemanticError.IllegalFunctionName(id.getName(), tokenizer.getLineNumber());
					} else {
						//Get and push function result
						funcEntry = (FunctionEntry) id;
						funcResult = funcEntry.getResult();
						semanticStack.push(funcResult);
						semanticStack.push("ARITHMETIC");
					}
				}
				else {
					//Entry is not a function. Push it back
					semanticStack.push(id);
					semanticStack.push("ARITHMETIC");
				}
				break;

			case 54:
				Etype = (String) semanticStack.pop();
				id = (SymbolTableEntry) semanticStack.pop();

				//Check if entry(id) is a procedure. If not, an error will occur
				if(!id.isProcedure())
					throw SemanticError.IllegalProcedureCall(id.getName(), tokenizer.getLineNumber());
				else
					//ID was a procedure. Push it back, to be used again later
					semanticStack.push(id);
				break;

			case 55:
				backPatch(globalStore,globalMemory);
				gen("free", globalMemory);
				gen("PROCEND");

				dumpstack(true);
				System.out.println("Parameters Stack:" + parameters.toString());
				System.out.println("Next Parameters Stack:" + nextParam.toString());
				//printCode(quads);
				writeCodeToFile(quads);
				break;

			case 56:
				gen("PROCBEGIN", globalTable.lookup("main"));
				globalStore = quads.getNextQuad();
				gen("alloc", "_");
				break;

			case 57:
				entry =lookupConstant(token);
				if(entry == null)
				{
					ConstantEntry constant = new ConstantEntry(token.getValue(), TokenType.INTEGER);
					constantTable.insert(constant);
					token.setEntry(constant);
				}
				else
					token.setEntry((ConstantEntry) entry);
				break;

			case 58:
				entry = lookupConstant(token);
				if(entry == null)
				{
					ConstantEntry constant = new ConstantEntry(token.getValue(), TokenType.REAL);
					constantTable.insert(constant);
					token.setEntry(constant);
				}
				else
					token.setEntry((ConstantEntry) entry);
				break;

			default:
				debug("Action " + actionNumber + " not yet implemented.");
				break;

			// TODO Eventually (i.e. final project) this should throw an exception.
		}
	}

	public SymbolTableEntry lookup(String name)
	{
		return globalTable.lookup(name);
	}

	public ConstantEntry lookupConstant(Token token)
	{
		return (ConstantEntry) constantTable.lookup(token.getValue());
	}

	private void installBuiltins(SymbolTable table) throws SymbolTableError {
		SymbolTable.installBuiltins(table);
	}


	private void debug(String message) {
		// TODO Uncomment the following line to enable debug output.
		System.out.println(message);
	}

	public void dumpstack(boolean onOrOff) {
		if (onOrOff) {
			System.out.println("Contents of stack are " + semanticStack.toString());
		}
	}

	public ArrayList makeList(int i)
	{//Return list containing a single integer, which will be the address of a memory location
		ArrayList list = new ArrayList();
		list.add(i);
		return list;
	}

	public ArrayList merge(ArrayList list1, ArrayList list2)
	{
		//Return a collection of all elements from two lists.
		ArrayList merger = new ArrayList();
		merger.addAll(list1);
		merger.addAll(list2);
		return merger;
	}

	public void backPatch(int p, int i)
	{
		//Sets the location of a previously unknown address
		quads.setField(p, 1, "" + i);
	}

	public void backPatch(ArrayList<Integer> expList, int i)
	{
		//Fills in the missing address in every location represented in the list
		for(int index = 0; index < expList.size(); index++)
		{
			int pos = expList.get(index);
			quads.setField(pos, 3, "" + i);
		}
	}

	public void gen(String tviCode)
	{
		//handles gen(CODE), gen(exit), gen(PROCBEGIN), gen(PROCEND)
		String[] codeArray = new String[4];
		codeArray[0] = tviCode;
		quads.addQuad(codeArray);
	}

	public void gen(String tviCode1, String target)
	{
		//handles gen(alloc,_)
		String[] codeArray = new String[4];
		codeArray[0] = tviCode1;
		codeArray[1] = target;
		quads.addQuad(codeArray);
	}

	public void gen(String tviCode1, int num)
	{
		//handles gen(free, globalMemory)
		String[] codeArray = new String[4];
		codeArray[0] = tviCode1;
		codeArray[1] = "" + num;
		quads.addQuad(codeArray);
	}


	public void gen(String tviCode, SymbolTableEntry op) throws SymbolTableError {

		if(op.isProcedure() || op.isFunction())
		{
			String[] codeArray = new String[4];
			codeArray[0] = tviCode;
			codeArray[1] = op.getName();
			quads.addQuad(codeArray);
		}
		else //Op is not a function or procedure
		{
			String prefix = "";
			//Determine prefix for address

			//Check if tviCode is "param", prefixes will be different
			if (tviCode.equals("param")) {
				if (op.isParameter())
					prefix = "^%"; // Refers to parameter
				else if (!global)
					prefix = "%"; // Refers to local variable
				else
					prefix = "_"; // Refers to global variable
			} else { //Not param code
				if (op.isParameter())
					prefix = "%"; // Refers to parameter
				else if (!global)
					prefix = "@%"; // Refers to local variable
				else
					prefix = "@_"; // Refers to global variable
			}

			// Entry could either be an array or a variable
			// If entry is a constant, its value will be moved to a variable entry
			if (op.isArray()) {
				ArrayEntry entry = (ArrayEntry) op;
				String[] codeArray = new String[4];
				codeArray[0] = tviCode;
				codeArray[1] = prefix + entry.getAddress();
				quads.addQuad(codeArray);
			} else {
				VariableEntry entry = new VariableEntry("Entry");
				if (op.isVariable()) {
					entry = (VariableEntry) op;
				} else if (op.isConstant()) {
					entry = create("TEMP" + tempCount, op.getType());
					tempCount++;

					TokenType type1 = op.getType();
					if (type1.equals(TokenType.INTEGER)) {
						int value1 = Integer.valueOf(op.getName());
						gen("move", value1, entry);
					} else //Type must be REAL
					{
						double value1 = Double.valueOf(op.getName());
						gen("move", value1, entry);
					}
				} else {
					System.out.println("OP isn't a variable/constant/array");
				}

				String[] codeArray = new String[4];
				codeArray[0] = tviCode;
				codeArray[1] = prefix + entry.getAddress();
				quads.addQuad(codeArray);
				}
			}
		}

	public void gen(String tviCode, SymbolTableEntry op, int num) throws SymbolTableError {

		//If entry is a function or procedue
		if(op.isFunction() || op.isProcedure())
		{
			//handles gen("call", procedure/function name, parameter count)
			String[] codeArray = new String[4];
			codeArray[0] = tviCode;
			codeArray[1] = op.getName();
			codeArray[2] = "" + num;
			quads.addQuad(codeArray);
		}
		else {
			//Determine prefix for address
			String prefix;
			if(op.isParameter())
				prefix = "^%"; // Refers to parameter
			else if(!global)
				prefix = "%"; // Refers to local variable
			else
				prefix = "_"; // Refers to global variable

			VariableEntry entry = new VariableEntry("Entry");

			if (op.isVariable()) {
				entry = (VariableEntry) op;
			} else if (op.isConstant()) {
				entry = create("TEMP" + tempCount, op.getType());
				tempCount++;

				TokenType type = op.getType();
				if (type.equals(TokenType.INTEGER)) {
					int value = Integer.valueOf(op.getName());
					gen("move", value, entry);
				} else //Type must be REAL
				{
					double value = Double.valueOf(op.getName());
					gen("move", value, entry);
				}
			} else {
				System.out.println("OP isn't a variable nor a constant");
			}

			String[] codeArray = new String[4];
			codeArray[0] = tviCode;
			codeArray[1] = prefix + entry.getAddress();
			codeArray[2] = "" + num;
			quads.addQuad(codeArray);
		}
	}

	public void gen(String tviCode, SymbolTableEntry op1, SymbolTableEntry op2, int storeHere) throws SymbolTableError {

		VariableEntry entry1 = new VariableEntry("Entry1");
		VariableEntry entry2 = new VariableEntry("Entry2");

		//Determine prefix for address of op1
		String prefixOp1;
		if(op1.isParameter())
			prefixOp1 = "^%"; // Refers to parameter
		else if(!global)
			prefixOp1 = "%"; // Refers to local variable
		else
			prefixOp1 = "_"; // Refers to global variable
		if(op1.isVariable())
		{
			entry1 = (VariableEntry) op1;
		}
		else if(op1.isConstant()) {
			entry1 = create("TEMP" + tempCount, op1.getType());
			tempCount++;

			TokenType type1 = op1.getType();
			if (type1.equals(TokenType.INTEGER)) {
				int value1 = Integer.valueOf(op1.getName());
				gen("move", value1, entry1);
			} else //Type must be REAL
			{
				double value1 = Double.valueOf(op1.getName());
				gen("move", value1, entry1);
			}
		}
		else
		{
			System.out.println("OP1 isn't a variable nor a constant");
		}

		//Determine prefix for op2
		String prefixOp2;
		if(op2.isParameter())
			prefixOp2 = "^%"; // Refers to parameter
		else if(!global)
			prefixOp2 = "%"; // Refers to local variable
		else
			prefixOp2 = "_"; // Refers to global variable
		if(op2.isVariable())
		{
			entry2 = (VariableEntry) op2;
		}
		else if(op2.isConstant()) {
			entry2 = create("TEMP" + tempCount, op2.getType());
			tempCount++;

			TokenType type2 = op2.getType();
			if (type2.equals(TokenType.INTEGER)) {
				int value2 = Integer.valueOf(op2.getName());
				gen("move", value2, entry2);
			} else //Type must be REALCONSTANT
			{
				double value2 = Double.valueOf(op2.getName());
				gen("move", value2, entry2);
			}
		}
		else
		{
			System.out.println("OP2 isn't a variable nor a constant");
		}

		String[] codeArray = new String[4];
		codeArray[0] = tviCode;
		codeArray[1] = prefixOp1 + entry1.getAddress();
		codeArray[2] = prefixOp2 + entry2.getAddress();
		codeArray[3] = "" + storeHere;
		quads.addQuad(codeArray);
	}

	public void gen(String tviCode, SymbolTableEntry op1, SymbolTableEntry op2, String target) throws SymbolTableError {

		VariableEntry entry1 = new VariableEntry("Entry1");
		VariableEntry entry2 = new VariableEntry("Entry2");

		//Determine prefix for address of op1
		String prefixOp1;
		if(op1.isParameter())
			prefixOp1 = "^%"; // Refers to parameter
		else if(!global)
			prefixOp1 = "%"; // Refers to local variable
		else
			prefixOp1 = "_"; // Refers to global variable
		if(op1.isVariable())
		{
			entry1 = (VariableEntry) op1;
		}
		else if(op1.isConstant()) {
			entry1 = create("TEMP" + tempCount, op1.getType());
			tempCount++;

			TokenType type1 = op1.getType();
			if (type1.equals(TokenType.INTEGER)) {
				int value1 = Integer.valueOf(op1.getName());
				gen("move", value1, entry1);
			} else //Type must be REAL
			{
				double value1 = Double.valueOf(op1.getName());
				gen("move", value1, entry1);
			}
		}
		else
		{
			System.out.println("OP1 isn't a variable nor a constant");
		}

		//Determine prefix for address of op2
		String prefixOp2;
		if(op2.isParameter())
			prefixOp2 = "^%"; // Refers to parameter
		else if(!global)
			prefixOp2 = "%"; // Refers to local variable
		else
			prefixOp2 = "_"; // Refers to global variable
		if(op2.isVariable())
		{
			entry2 = (VariableEntry) op2;
		}
		else if(op2.isConstant()) {
			entry2 = create("TEMP" + tempCount, op2.getType());
			tempCount++;

			TokenType type2 = op2.getType();
			if (type2.equals(TokenType.INTEGER)) {
				int value2 = Integer.valueOf(op2.getName());
				gen("move", value2, entry2);
			} else //Type must be REAL
			{
				double value2 = Double.valueOf(op2.getName());
				gen("move", value2, entry2);
			}
		}
		else
		{
			System.out.println("OP2 isn't a variable nor a constant");
		}

		//Fill Quadruple
		String[] codeArray = new String[4];
		codeArray[0] = tviCode;
		codeArray[1] = prefixOp1 + entry1.getAddress();
		codeArray[2] = prefixOp2 + entry2.getAddress();
		codeArray[3] = target;
		quads.addQuad(codeArray);
	}

	public void gen(String tviCode, int num, SymbolTableEntry op) throws SymbolTableError {

		//Determine prefix for address
		String prefix;
		if(op.isParameter())
			prefix = "^%"; // Refers to parameter
		else if(!global)
			prefix = "%"; // Refers to local variable
		else
			prefix = "_"; // Refers to global variable

		VariableEntry entry = new VariableEntry("Entry");

		if(op.isVariable())
		{
			entry = (VariableEntry) op;
		}
		else if(op.isConstant()) {
			entry = create("TEMP" + tempCount, op.getType());
			tempCount++;

			TokenType type1 = op.getType();
			if (type1.equals(TokenType.INTEGER)) {
				int value1 = Integer.valueOf(op.getName());
				gen("move", value1, entry);
			} else //Type must be REAL
			{
				double value1 = Double.valueOf(op.getName());
				gen("move", value1, entry);
			}
		}
		else
		{
			System.out.println("OP isn't a variable nor a constant");
		}

		//Fill Quadruple
		String[] codeArray = new String[4];
		codeArray[0] = tviCode;
		codeArray[1] = "" + num;
		codeArray[2] = prefix + entry.getAddress();
		quads.addQuad(codeArray);
	}

	public void gen(String tviCode, double num, SymbolTableEntry op) throws SymbolTableError {

		VariableEntry entry = new VariableEntry("Entry");

		//Determine prefix for address
		String prefixOp;
		if(op.isParameter())
			prefixOp = "^%"; // Refers to parameter
		else if(!global)
			prefixOp = "%"; // Refers to local variable
		else
			prefixOp = "_"; // Refers to global variable

		if(op.isVariable())
		{
			entry = (VariableEntry) op;
		}
		else if(op.isConstant()) {
			entry = create("TEMP" + tempCount, op.getType());
			tempCount++;

			TokenType type = op.getType();
			if (type.equals(TokenType.INTEGER)) {
				int value = Integer.valueOf(op.getName());
				gen("move", value, entry);
			} else //Type must be REAL
			{
				double value = Double.valueOf(op.getName());
				gen("move", value, entry);
			}
		}
		else
		{
			System.out.println("OP isn't a variable nor a constant");
		}

		//Fill Quadruple
		String[] codeArray = new String[4];
		codeArray[0] = tviCode;
		codeArray[1] = "" + num;
		codeArray[2] = prefixOp + entry.getAddress();
		quads.addQuad(codeArray);
	}

	public void gen(String tviCode, SymbolTableEntry op1, SymbolTableEntry op2) throws SymbolTableError {
		//handles gen("ltof" id2, temp2)
		VariableEntry entry1 = new VariableEntry("Entry1");
		VariableEntry entry2 = new VariableEntry("Entry2");

		//Determine prefix for address of op1
		String prefixOp1;
		if(op1.isParameter())
			prefixOp1 = "^%"; // Refers to parameter
		else if(!global)
			prefixOp1 = "%"; // Refers to local variable
		else
			prefixOp1 = "_"; // Refers to global variable
		if(op1.isVariable())
		{
			entry1 = (VariableEntry) op1;
		}
		else if(op1.isConstant()) {
			entry1 = create("TEMP" + tempCount, op1.getType());
			tempCount++;

			TokenType type1 = op1.getType();
			if (type1.equals(TokenType.INTEGER)) {
				int value1 = Integer.valueOf(op1.getName());
				gen("move", value1, entry1);
			} else //Type must be REAL
			{
				double value1 = Double.valueOf(op1.getName());
				gen("move", value1, entry1);
			}
		}
		else
		{
			System.out.println("OP1 isn't a variable nor a constant");
		}

		//Determine prefix for address of op2
		String prefixOp2;
		if(op2.isParameter())
			prefixOp2 = "^%"; // Refers to parameter
		else if(!global)
			prefixOp2 = "%"; // Refers to local variable
		else
			prefixOp2 = "_"; // Refers to global variable
		if(op2.isVariable())
		{
			entry2 = (VariableEntry) op2;
		}
		else if(op2.isConstant()) {
			entry2 = create("TEMP" + tempCount, op2.getType());
			tempCount++;

			TokenType type2 = op2.getType();
			if (type2.equals(TokenType.INTEGER)) {
				int value2 = Integer.valueOf(op2.getName());
				gen("move", value2, entry2);
			} else //Type must be REAL
			{
				double value2 = Double.valueOf(op2.getName());
				gen("move", value2, entry2);
			}
		}
		else
		{
			System.out.println("OP2 isn't a variable nor a constant");
		}

			String[] codeArray = new String[4];
			codeArray[0] = tviCode;
			codeArray[1] = prefixOp1 + entry1.getAddress();
			codeArray[2] = prefixOp2 + entry2.getAddress();
			quads.addQuad(codeArray);
	}

	public void gen(String tviCode, SymbolTableEntry op1, int num, SymbolTableEntry op2) throws SymbolTableError {

		VariableEntry entry1 = new VariableEntry("Entry1");
		VariableEntry entry2 = new VariableEntry("Entry2");

		//Determine prefix for address of op1
		String prefixOp1;
		if(op1.isParameter())
			prefixOp1 = "^%"; // Refers to parameter
		else if(!global)
			prefixOp1 = "%"; // Refers to local variable
		else
			prefixOp1 = "_"; // Refers to global variable
		if(op1.isVariable())
		{
			entry1 = (VariableEntry) op1;
		}
		else if(op1.isConstant()) {
			entry1 = create("TEMP" + tempCount, op1.getType());
			tempCount++;

			TokenType type1 = op1.getType();
			if (type1.equals(TokenType.INTEGER)) {
				int value1 = Integer.valueOf(op1.getName());
				gen("move", value1, entry1);
			} else //Type must be REAL
			{
				double value1 = Double.valueOf(op1.getName());
				gen("move", value1, entry1);
			}
		}
		else
		{
			System.out.println("OP1 isn't a variable nor a constant");
		}

		//Determine prefix for address of op2
		String prefixOp2;
		if(op2.isParameter())
			prefixOp2 = "^%"; // Refers to parameter
		else if(!global)
			prefixOp2 = "%"; // Refers to local variable
		else
			prefixOp2 = "_"; // Refers to global variable
		if(op2.isVariable())
		{
			entry2 = (VariableEntry) op2;
		}
		else if(op2.isConstant()) {
			entry2 = create("TEMP"  + tempCount, op2.getType());
			tempCount++;

			TokenType type2 = op2.getType();
			if (type2.equals(TokenType.INTEGER)) {
				int value2 = Integer.valueOf(op2.getName());
				gen("move", value2, entry2);
			} else //Type must be REAL
			{
				double value2 = Double.valueOf(op2.getName());
				gen("move", value2, entry2);
			}
		}
		else
		{
			System.out.println("OP2 isn't a variable nor a constant");
		}

			String[] codeArray = new String[4];
			codeArray[0] = tviCode;
			codeArray[1] = prefixOp1 + entry1.getAddress();
			codeArray[2] = "" + num;
			codeArray[3] = prefixOp2 + entry2.getAddress();
			quads.addQuad(codeArray);
	}

	public void gen(String tviCode, SymbolTableEntry op1, SymbolTableEntry op2, SymbolTableEntry op3) throws SymbolTableError {

		VariableEntry entry1 = new VariableEntry("Entry1");
		VariableEntry entry2 = new VariableEntry("Entry2");
		VariableEntry entry3 = new VariableEntry("Entry3");

		//Determine prefix for address of op1
		String prefixOp1;
		if(op1.isParameter())
			prefixOp1 = "^%"; // Refers to parameter
		else if(!global)
			prefixOp1 = "%"; // Refers to local variable
		else
			prefixOp1 = "_"; // Refers to global variable
		if(op1.isVariable())
		{
			entry1 = (VariableEntry) op1;
		}
		else if(op1.isConstant()) {
			entry1 = create("TEMP" + tempCount, op1.getType());
			tempCount++;

			TokenType type1 = op1.getType();
			if (type1.equals(TokenType.INTEGER)) {
				int value1 = Integer.valueOf(op1.getName());
				gen("move", value1, entry1);
			} else //Type must be REAL
			{
				double value1 = Double.valueOf(op1.getName());
				gen("move", value1, entry1);
			}
		}
		else
		{
			System.out.println("OP1 isn't a variable nor a constant");
		}

		//Determine prefix for address of op2
		String prefixOp2;
		if(op2.isParameter())
			prefixOp2 = "^%"; // Refers to parameter
		else if(!global)
			prefixOp2 = "%"; // Refers to local variable
		else
			prefixOp2 = "_"; // Refers to global variable
		if(op2.isVariable())
		{
			entry2 = (VariableEntry) op2;
		}
		else if(op2.isConstant()) {
			entry2 = create("TEMP" + tempCount, op2.getType());
			tempCount++;

			TokenType type2 = op2.getType();
			if (type2.equals(TokenType.INTEGER)) {
				int value2 = Integer.valueOf(op2.getName());
				gen("move", value2, entry2);
			} else //Type must be REAL
			{
				double value2 = Double.valueOf(op2.getName());
				gen("move", value2, entry2);
			}
		}
		else
		{
			System.out.println("OP2 isn't a variable nor a constant");
		}

		//Determine prefix for address of op3
		String prefixOp3;
		if(op3.isParameter())
			prefixOp3 = "^%"; // Refers to parameter
		else if(!global)
			prefixOp3 = "%"; // Refers to local variable
		else
			prefixOp3 = "_"; // Refers to global variable
		if(op3.isVariable())
		{
			entry3 = (VariableEntry) op3;
		}
		else if(op3.isConstant()) {
			entry3 = create("TEMP" + tempCount, op3.getType());
			tempCount++;

			TokenType type3 = op3.getType();
			if (type3.equals(TokenType.INTEGER)) {
				int value3 = Integer.valueOf(op3.getName());
				gen("move", value3, entry3);
			} else //Type must be REAL
			{
				double value3 = Double.valueOf(op3.getName());
				gen("move", value3, entry3);
			}
		}
		else
		{
			System.out.println("OP3 isn't a variable nor a constant");
		}

			String[] codeArray = new String[4];
			codeArray[0] = tviCode;
			codeArray[1] = prefixOp1 + entry1.getAddress();
			codeArray[2] = prefixOp2 + entry2.getAddress();
			codeArray[3] = prefixOp3 + entry3.getAddress();
			quads.addQuad(codeArray);
	}

	public int typeCheck(SymbolTableEntry entry1, SymbolTableEntry entry2)
	{
		//Determine type combination

		if(entry1.getType().equals(TokenType.INTEGER) & entry2.getType().equals(TokenType.INTEGER))
		{
			//Both entries are "integer" types
			return 0;
		}
		else if(entry1.getType().equals(TokenType.REAL) & entry2.getType().equals(TokenType.REAL))
		{
			//Both entries are "real" types
			return 1;
		}
		else if(entry1.getType().equals(TokenType.REAL) & entry2.getType().equals(TokenType.INTEGER))
		{
			//First entry is an "integer" type, and the second entry is a "real" type
			return 2;
		}
		else if (entry1.getType().equals(TokenType.INTEGER) & entry2.getType().equals(TokenType.REAL))
		{
			//First entry is an "real" type, and the second entry is an "integer" type
			return 3;
		}
		else
		{//Entries should either have INTEGER or REAL as their types. Must have passed incorrect entries
			System.out.println("TypeCheck Error: entry1 is " + entry1.getType() + " and entry2 is " + entry2.getType());
			return -1;
		}
	}

	public VariableEntry create(String name, TokenType type) throws SymbolTableError {
		VariableEntry entry;
		if(global) {
			//Creating global temporary variable
			int address = globalMemory;
			globalMemory++;
			entry = new VariableEntry("$$" + name, address, type);
			globalTable.insert(entry);
		}
		else
		{
			//Creating local temporary variable
			int address = localMemory;
			localMemory++;
			entry = new VariableEntry("$$" + name, address, type);
			localTable.insert(entry);
		}

		//Return new temporary variable
		return entry;
	}

	public void printCode(Quadruples quadruples)
	{
		//Print tvi code
		quadruples.print();
	}

	public void writeCodeToFile(Quadruples quadruples) throws FileNotFoundException {
		//Write tvi code to a file specified in Quadruple class
		quadruples.write();
	}
}