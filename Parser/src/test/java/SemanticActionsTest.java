import errors.*;
import lex.TokenType;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import parser.Parser;
import symbolTable.ArrayEntry;
import symbolTable.ProcedureEntry;
import symbolTable.SymbolTableEntry;
import symbolTable.VariableEntry;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */

public class SemanticActionsTest
{
	private static final File RESOURCE = new File("src/test/resources");

	protected Parser parser;

	@After
	public void cleanup()
	{
		parser = null;
	}

	protected void init(String filename) throws IOException, LexicalError, SyntaxError, SemanticError, SymbolTableError {
		init(new File(RESOURCE, filename));
	}

	protected void init(File file) throws IOException, LexicalError, SemanticError, SyntaxError, SymbolTableError {
		assertTrue("File not found:" + file.getPath(), file.exists());
		parser = new Parser(file);
		parser.parse();
	}

	@Test
	public void testArray() throws LexicalError, SemanticError, SyntaxError, IOException, SymbolTableError {
		System.out.println("SemanticActionsTest.testArray");
		init("array.pas");
		assert(true);
	}

	@Test
	public void testArray2() throws LexicalError, SemanticError, SyntaxError, IOException, SymbolTableError {
		System.out.println("SemanticActionsTest.testArray2");
		init("array2.pas");
		assert(true);
	}

	@Test
	public void testArrayRef() throws LexicalError, SemanticError, SyntaxError, IOException, SymbolTableError {
		System.out.println("SemanticActionsTest.testArrayRef");
		init("arrayref.pas");
		assert(true);
	}

	@Test
	public void testBoolean() throws LexicalError, SemanticError, SyntaxError, IOException, SymbolTableError {
		System.out.println("SemanticActionsTest.testBoolean");
		init("bool.pas");
		assert(true);
	}

	@Test
	public void testExpression() throws LexicalError, SemanticError, SyntaxError, IOException, SymbolTableError {
		System.out.println("SemanticActionsTest.testExpression");
		init("expression.pas");
		assert(true);
	}

	@Test
	public void testExpressionTest() throws LexicalError, SemanticError, SyntaxError, IOException, SymbolTableError {
		System.out.println("SemanticActionsTest.testExpTest");
		init("exptest.pas");
		assert(true);
	}

	@Test
	public void testSimpleTest() throws LexicalError, SemanticError, SyntaxError, IOException, SymbolTableError {
		System.out.println("SemanticActionsTest.testSimpleTest");
		init("simple.pas");
		assert(true);
	}

	@Test
	public void testFibonacci() throws LexicalError, SemanticError, SyntaxError, IOException, SymbolTableError {
		System.out.println("SemanticActionsTest.testFib");
		init("fib.pas");
		assert(true);
	}

	@Test
	public void testIf() throws LexicalError, SemanticError, SyntaxError, IOException, SymbolTableError {
		System.out.println("SemanticActionsTest.testIf");
		init("if.pas");
		assert(true);
	}

	@Test
	public void testMOD() throws LexicalError, SemanticError, SyntaxError, IOException, SymbolTableError {
		System.out.println("SemanticActionsTest.testMOD");
		init("mod.pas");
		assert(true);
	}

	@Test
	public void testMOD2() throws LexicalError, SemanticError, SyntaxError, IOException, SymbolTableError {
		System.out.println("SemanticActionsTest.testMOD2");
		init("mod2.pas");
		assert(true);
	}

	@Test
	public void testProcOne() throws LexicalError, SemanticError, SyntaxError, IOException, SymbolTableError {
		System.out.println("SemanticActionsTest.testProcOne");
		init("one.pas");
		assert(true);
	}

	@Test
	public void testOr() throws LexicalError, SemanticError, SyntaxError, IOException, SymbolTableError {
		System.out.println("SemanticActionsTest.testOr");
		init("or.pas");
		assert(true);
	}

	@Test
	public void testProc() throws LexicalError, SemanticError, SyntaxError, IOException, SymbolTableError {
		System.out.println("SemanticActionsTest.testProc");
		init("proc.pas");
		assert(true);
	}

	@Test
	public void testFunc() throws LexicalError, SemanticError, SyntaxError, IOException, SymbolTableError {
		System.out.println("SemanticActionsTest.testFunc");
		init("func.pas");
		assert(true);
	}

/*
	@Test
	public void testBuiltIns() throws LexicalError, SemanticError, SyntaxError, IOException, SymbolTableError {
		System.out.println("SemanticActionsTest.testBuiltIns");
		init("builtins.pas");
		checkBuiltIn("main");
		checkBuiltIn("read");
		checkBuiltIn("write");
		checkIO("input");
		checkIO("output");
	}

	private void checkIO(String name) throws SemanticError
	{
		SymbolTableEntry entry = parser.lookup("input");
		assertTrue(name + " is not a varible", entry.isVariable());
		assertTrue(name + " is not reserved", entry.isReserved());
		checkIsA(entry, 2);
	}

	@Test
	public void testCaseSensitive() throws LexicalError, SemanticError, SyntaxError, IOException, SymbolTableError {
		System.out.println("SemanticActionsTest.testCaseSensitive");
		init("variable_test.pas");
		SymbolTableEntry a = parser.lookup("a");
		assertNotNull(a);
		SymbolTableEntry A = parser.lookup("A");
		assertNotNull(A);
		assertTrue("Lookup returned different objects.", a == A);
	}

	@Test
	public void variableTest() throws IOException, LexicalError, SemanticError, SyntaxError, SymbolTableError {
		System.out.println("SemanticActionsTest.variableTest");
		init("variable_test.pas");
		checkVariable("a", TokenType.INTEGER, 1);
		checkVariable("b", TokenType.INTEGER, 0);
		checkVariable("c", TokenType.REAL, 2);
	}

	@Test
	public void arrayDeclTest() throws IOException, LexicalError, SemanticError, SyntaxError, SymbolTableError {
		System.out.println("SemanticActionsTest.arrayDeclTest");
		init("array_decl_test.pas");
		checkArrayEntry("x", TokenType.REAL, 0, 1, 5);
		checkArrayEntry("y", TokenType.INTEGER, 5, 15, 100);
	}

	@Test
	public void bigTest() throws LexicalError, SemanticError, SyntaxError, IOException, SymbolTableError {
		System.out.println("SemanticActionsTest.bigTest");
		init("big_test.pas");
		checkVariable("a", TokenType.INTEGER, 1);
		checkVariable("b", TokenType.INTEGER, 0);
		checkArrayEntry("x", TokenType.REAL, 2, 1, 5);
		checkArrayEntry("y", TokenType.INTEGER, 7, 15, 100);
		checkVariable("c", TokenType.REAL, 93);
		checkVariable("d", TokenType.INTEGER, 94);
	}

	@Test
	public void duplicateVariable() throws LexicalError, SyntaxError, IOException, SymbolTableError {
		System.out.println("SemanticActionsTest.duplicateVariable");
		expectException("duplicate_variable.pas", CompilerError.Type.MULTI_DECL);
	}

	@Test
	public void reservedNameAsVariable() throws LexicalError, IOException, SyntaxError, SymbolTableError {
		System.out.println("SemanticActionsTest.reservedNameAsVariable");
		expectException("reserved_word.pas", CompilerError.Type.RESERVED_NAME);
	}

	@Test
	public void keywordAsVariable() throws LexicalError, IOException, SyntaxError, SymbolTableError {
		System.out.println("SemanticActionsTest.keywordAsVariable");
		expectException("keyword.pas", CompilerError.Type.RESERVED_NAME);
	}
*/
	private void expectException(String path, CompilerError.Type expected) throws LexicalError, IOException, SyntaxError, SymbolTableError {
		try
		{
			init(path);
			fail("Expected exception not thrown: " + expected);
		}
		catch (SemanticError e)
		{
			assertEquals("Wrong exception type thrown", expected, e.getType());
		}
	}

	private void checkBuiltIn(String name) throws SemanticError
	{
		SymbolTableEntry entry = parser.lookup(name);
		assertNotNull(entry);
		assertEquals("Wrong entry returned", name.toUpperCase(), entry.getName());
		assertTrue(name + " is not a prodedure", entry.isProcedure());
		assertTrue(name + " is not a reserved", entry.isReserved());
		checkIsA(entry, 2);

		assertTrue("Not a ProcedureEntry", entry instanceof ProcedureEntry);
	}

	private void checkArrayEntry(String name, TokenType type, int address, int lower, int upper) throws SemanticError
	{
		SymbolTableEntry entry = parser.lookup(name);
		assertNotNull(entry);
		assertEquals("Wrong entry returned", name.toUpperCase(), entry.getName());
		assertTrue("Entry is not an array.", entry.isArray());

		checkIsA(entry);
		ArrayEntry array = (ArrayEntry) entry;
		assertEquals("Wrong address assigned.", address, array.getAddress());
		checkType(type, entry.getType());
		assertEquals("Lower bound is wrong", lower, array.getLBound());
		assertEquals("Upper bound is wrong", upper, array.getUBound());
	}

	private void checkVariable(String name, TokenType type, int address) throws SemanticError
	{
		SymbolTableEntry entry = parser.lookup(name);
		assertNotNull(entry);
		assertEquals("Wrong entry returned", name.toUpperCase(), entry.getName());
		assertTrue(entry.isVariable());
		checkIsA(entry);
		VariableEntry ve = (VariableEntry) entry;
		assertEquals("Wrong address assigned.", address, ve.getAddress());
		checkType(type, entry.getType());
	}

	private void print(SymbolTableEntry e)
	{
		System.out.println(e.getName() + ": " + e.getType());
	}

	private void checkType(TokenType expected, TokenType actual)
	{
		assertNotNull(actual);
		String message = String.format("Invalid type. Expected: %s Actual: %s", expected, actual);
		assertEquals(message, expected, actual);
	}

	private void checkIsA(SymbolTableEntry e)
	{
		checkIsA(e, 1);
	}

	private void checkIsA(SymbolTableEntry e, int expected)
	{
		int count = 0;

		if (e.isArray()) ++count;
		if (e.isConstant()) ++count;
		if (e.isFunction()) ++count;
		if (e.isFunctionResult()) ++count;
		if (e.isKeyword()) ++count;
		if (e.isParameter()) ++count;
		if (e.isProcedure()) ++count;
		if (e.isReserved()) ++count;
		if (e.isVariable()) ++count;

		assertEquals("Invalid symbol table entry for " + e.getName(), expected, count);
	}


}
