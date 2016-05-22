/*
 * Copyright 2016 Vassar College
 * All rights reserverd.
 */

package errors;

import lex.TokenType;

public class SyntaxError extends CompilerError
{
	   public SyntaxError(Type errorNumber, String message)
	   {
	      super(errorNumber, message);
	   }

	   // Factory methods to generate the lexical exception types.

	   public static SyntaxError BadToken(TokenType t, int line)
	   {
	      return new SyntaxError(Type.BAD_TOKEN,
	                              ">>> ERROR on line " + line + " : unexpected " + t);
	   }

	public static SyntaxError BadToken(TokenType t, int line, String value)
	{
		return new SyntaxError(Type.BAD_TOKEN,
				">>> ERROR on line " + line + " : unexpected " + t + " [" + value + "]");
	}

	public static SyntaxError BadToken(TokenType expected, TokenType found, int line)
	{
		return new SyntaxError(Type.BAD_TOKEN,
				">>> ERROR on line " + line + " : expected " + expected + ", but found " + found + ".");
	}

	public static SyntaxError MissingSemicolon(int line)
	{
		return new SyntaxError(Type.BAD_TOKEN,
				">>> ERROR on line " + line + " : missing semicolon before next statement.");
	}

	public static SyntaxError UnexpectedEnd(int line)
	{
		return new SyntaxError(Type.BAD_TOKEN,
				">>> ERROR on line " + line + " : 'end' appears incorrectly. Need a preceding 'begin' and/or " +
						"successfully complete previous statement.");
	}

}
