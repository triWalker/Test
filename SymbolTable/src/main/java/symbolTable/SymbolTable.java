package symbolTable;

import errors.SymbolTableError;

import java.util.HashMap;

public class SymbolTable {

	private HashMap<String, SymbolTableEntry> symTable;

	public SymbolTable() {

		symTable = new HashMap<>();

	}

	public SymbolTable (int size) throws SymbolTableError {

		symTable = new HashMap<>(size);
		//installBuiltins(this);

	}

	public SymbolTableEntry lookup (String key)
	{
		return symTable.get(key.toUpperCase());
	}

	public void insert(SymbolTableEntry entry) throws SymbolTableError
	{
		// Use uppercase version of key so one word will match to one entry, regardless of its case.
		// Ex. add, aDd and ADD should return the same result
		String key = entry.getName().toUpperCase();

		if(symTable.containsKey(key))
		{
			//Entry already exists in the table
			throw SymbolTableError.DuplicateEntry(key);
		}
		else
		{
			//Safe to insert the new entry
			symTable.put(key, entry);
		}

	}

	public int size() {
		return symTable.size();
	}

	//Print entries in the symbol table
	public void dumpTable () {

		System.out.println("Elements of symbol table are " + symTable.values());

	}

	public static void installBuiltins(SymbolTable symbolTable) throws SymbolTableError {

		//Builtins input and output will be inserted into table when semantic action #9 is called

		ProcedureEntry main = new ProcedureEntry("MAIN",0);
		ProcedureEntry read = new ProcedureEntry("READ",0);
		ProcedureEntry write = new ProcedureEntry("WRITE",0);
		//IODeviceEntry input = new IODeviceEntry("INPUT");
		//IODeviceEntry output = new IODeviceEntry("OUTPUT");

		main.setReserved(true);
		read.setReserved(true);
		write.setReserved(true);
		//input.setReserved(true);
		//output.setReserved(true);

		symbolTable.insert(main);
		symbolTable.insert(read);
		symbolTable.insert(write);
		//symbolTable.insert(input);
		//symbolTable.insert(output);
	}

}
