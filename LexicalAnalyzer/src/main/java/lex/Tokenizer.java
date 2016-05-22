package lex;

import errors.LexicalError;
import symbolTable.KeywordTable;
import symbolTable.SymbolTable;
import symbolTable.SymbolTableEntry;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/*
 * TODO: Assignment #1
 */
public class Tokenizer {
	private CharStream stream = null;

	/**
	 * The KeywordTable is a SymbolTable that comes with all of the KeywordEntries
	 * already inserted.
	 */
	private KeywordTable keywordTable;
	private SymbolTable table;

	private Token token;
	private TokenType lastTokenType;
	private int MAX_SIZE = 40;

	public Tokenizer(String filename) throws IOException, LexicalError {
		super();
		init(new CharStream(filename));
	}

	/**
	 * Used during testing to read files from the classpath.
	 */
	public Tokenizer(URL url) throws IOException, LexicalError {
		super();
		init(new CharStream(url));
	}

	public Tokenizer(File file) throws IOException, LexicalError {
		super();
		init(new CharStream(file));
	}

	protected void init(CharStream stream) {
		this.stream = stream;
		keywordTable = new KeywordTable();
		table = new SymbolTable();
		token = new Token();

		//Variable will help determine if "+/-" is a unary operator or an addop
		lastTokenType = null;
	}

	public int getLineNumber() {
		return stream.lineNumber();
	}

	public Token getNextToken() throws LexicalError {
		token.clear();
		char c = stream.currentChar();

		if (c == CharStream.BLANK)
			c = stream.currentChar();
		if (c == CharStream.EOF)
		{
			token.setType(TokenType.ENDOFFILE);
			token.setValue("EOF");
			lastTokenType = token.getType();
		}
		else if (Character.isLetter(c))
			token = readIdentifier(c);
		else if (Character.isDigit(c))
			token = readNumber(c, "");
		else
			token = readSymbol(c);

		return token;
	}


	// TODO Much (much) more code goes here...
	protected Token readIdentifier(char character) throws LexicalError {
		String lexeme = "";

		while (Character.isDigit(character) || Character.isLetter(character)) {
			if (lexeme.length() >= MAX_SIZE) {
				//Finish reading in identifier, then return notification that identifier is too long
				String idTooLong = lexeme;
				while (Character.isDigit(character) || Character.isLetter(character)) {
					String letterOverLimit = Character.toString(character);
					idTooLong= idTooLong + letterOverLimit;
					character = stream.currentChar();
				}
				//Determine how many characters are past the limit
				int overLimit = idTooLong.length() - MAX_SIZE;
				//Send Alert
				throw LexicalError.IdentifierTooLong(idTooLong, overLimit);
			} else {
				//Lexeme is not too long so far, continue appending characters
				String value = Character.toString(character);
				lexeme = lexeme + value;
				character = stream.currentChar();
			}
		}

		//Return character that was pulled after last digit or letter. This would be the character that ended the loop
		stream.pushBack(character);

		//Check if lexeme is a keyword
		SymbolTableEntry keywordCheck = keywordTable.lookup(lexeme);
		if (keywordCheck != null) {
			//Lexeme is a keyword
			token.setType(keywordCheck.getType());
			token.setValue(lexeme.toUpperCase());
		}
		else
		{
			//Lexeme is an identifier, not a keyword
			token.setType(TokenType.IDENTIFIER);
			token.setValue(lexeme);
		}

		//Save last token type. It may be needed later
		lastTokenType = token.getType();

		return token;
	}

	protected Token readNumber(char character, String num) throws LexicalError {

		//Will help determine whether the current number is an integer or a real
		boolean notInteger = false;
		//Can only have one E in a real number
		boolean seenE = false;

		while (character != CharStream.BLANK) {
			String value = Character.toString(character);
			if (Character.isDigit(character)) {
				//Append new digit to current string of digits
				num = num + value;
				character = stream.currentChar();
			}
			//Current Character is not a digit
			//Check if it is the exponential
			else if(value.toUpperCase().equals("E") & !seenE){
				//Need lookahead to determine specific case
				Character lookahead = stream.currentChar();
				String lookaheadValue = Character.toString(lookahead);

				//Check if a unary operator follows E.
				if(lookaheadValue.equals("+") || lookaheadValue.equals("-"))
				{
					//Need another lookahead
					Character lookFurtherAhead = stream.currentChar();
					String lookFurtherAheadValue = Character.toString(lookFurtherAhead);

					//Check if a digit follows unary operator
					if(Character.isDigit(lookFurtherAhead))
					{//Digit followed unary operator.
						// The current number is not an integer
						notInteger = true;
						//Has seen E. Another one is not allowed
						seenE = true;
						//Append recent characters from stream, and continue looking for digits
						num = num + value + lookaheadValue + lookFurtherAheadValue;

						//Get next character
						character = stream.currentChar();
					}
					else
					{
						//A digit did not follow the unary operator. Push back characters after
						//last digit was seen. End loop
						stream.pushBack(lookFurtherAhead);
						stream.pushBack(lookahead);
						stream.pushBack(character);
						break;
					}
				}
				//Check if a digit follows E
				else if(lookahead.isDigit(lookahead))
				{//Current number is not an integer
					notInteger = true;
					//E has been seen
					seenE = true;
					//Append recent characters, and continue looking for digits
					num = num + value + lookaheadValue;

					//Get next character
					character = stream.currentChar();
				}
				//Check if E is the end of current number
				else if(lookahead == CharStream.BLANK)
				{
					//Append E, then end the loop
					num = num + value;
					break;
				}
				else //Character must not be associated with current number. Push it back and end the loop
					stream.pushBack(lookahead);
			}
			//Check if current character is a "." Also, make sure dot is not coming after an E
			else if (value.equals(".") & !seenE) {
				// Need a lookahead to determine specific case
				Character lookahead = stream.currentChar();
				String lookaheadValue = Character.toString(lookahead);

				if (lookaheadValue.equals(".")) {
					//Doubledot Case, push the two dots back onto the stack
					stream.pushBack(lookahead);
					stream.pushBack(character);
					break;
				}
				else if (Character.isDigit(lookahead) & !notInteger) {
					//The next character is a digit. Thus the current number is real. After appending the dot and new
					// digit, continue streaming characters
					num = num + value;
					num = num + lookaheadValue;
					notInteger = true;
					character = stream.currentChar();
				} else if (Character.isDigit(lookahead)) {
					//Current number is not an integer
					// A "." was already seen for the current number. Two dots are not allowed in a real number
					throw LexicalError.SingleDot(getLineNumber());
				} else {
					//"." must have represented an endmarker
					stream.pushBack(lookahead);
					stream.pushBack(character);
					break;
				}
			}
			else
			{
				//Return character, it must not be associated with current number. End the loop
				stream.pushBack(character);
				break;
			}
		}

		if (notInteger) {
			//Number is a real
			token.setType(TokenType.REALCONSTANT);
		} else {
			//Number is an integer
			token.setType(TokenType.INTCONSTANT);
		}

		token.setValue(num);

		lastTokenType = token.getType();

		return token;
	}

	protected Token readSymbol(char character) throws LexicalError {

		String value = Character.toString(character);

		if (value.equals("<")) {
			//Need a lookahead to determine specific case
			Character lookahead = stream.currentChar();
			String lookaheadValue = Character.toString(lookahead);

			if (lookahead.equals(CharStream.BLANK)) {
				//"Less Than" Case
				token.setType(TokenType.RELOP);
				token.setValue("3");
			} else if (lookaheadValue.equals("=")) {
				//"LESS THAN OR EQUAL" Case
				token.setType(TokenType.RELOP);
				token.setValue("5");
			} else if (lookaheadValue.equals(">")) {
				//"NOT EQUAL" Case
				token.setType(TokenType.RELOP);
				token.setValue("2");
			} else {
				//Lookahead character is not associated with "<". Return it and create token for "less than" operation
				stream.pushBack(lookahead);
				token.setType(TokenType.RELOP);
				token.setValue("3");
			}
		} else if (value.equals(">")) {
			//Need a lookahead to determine specific case
			Character lookahead = stream.currentChar();
			String lookaheadValue = Character.toString(lookahead);

			if (lookahead.equals(CharStream.BLANK)) {
				//"Greater Than" Case
				token.setType(TokenType.RELOP);
				token.setValue("4");
			} else if (lookaheadValue.equals("=")) {
				//"GREATER THAN OR EQUAL" Case
				token.setType(TokenType.RELOP);
				token.setValue("6");
			} else {
				//Must be a character not associated with ">". Return it and create token for  "greater than" operation
				stream.pushBack(lookahead);
				token.setType(TokenType.RELOP);
				token.setValue("4");
			}
		} else if (value.equals("=")) {
			//"Equality Check" Case
			token.setType(TokenType.RELOP);
			token.setValue("1");
		} else if (value.equals("+")) {
			if (lastTokenType != null) {
				if (lastTokenType.equals(TokenType.IDENTIFIER) || lastTokenType.equals(TokenType.REALCONSTANT) ||
						lastTokenType.equals(TokenType.INTCONSTANT) || lastTokenType.equals(TokenType.RIGHTPAREN) ||
						lastTokenType.equals(TokenType.RIGHTBRACKET)) {
					//"+" is an ADDOP
					token.setType(TokenType.ADDOP);
					token.setValue("1");
				} else {
					//Last Token type was not null, but "+" is not an ADDOP, so it must be a unary operator
					token.setType(TokenType.UNARYPLUS);
					token.setValue(value);
				}
			} else {
				// Last Token Type was null, "+" must be a unary operator
				token.setType(TokenType.UNARYPLUS);
				token.setValue(value);
			}
		} else if (value.equals("-")) {
			if (lastTokenType != null) {
				if (lastTokenType.equals(TokenType.IDENTIFIER) || lastTokenType.equals(TokenType.REALCONSTANT) ||
						lastTokenType.equals(TokenType.INTCONSTANT) || lastTokenType.equals(TokenType.RIGHTPAREN) ||
						lastTokenType.equals(TokenType.RIGHTBRACKET)) {
					//"-" is an ADDOP
					token.setType(TokenType.ADDOP);
					token.setValue("2");
				} else {
					//Last Token type was not null, but "-" is not an ADDOP, so it must be a unary operator
					token.setType(TokenType.UNARYMINUS);
					token.setValue(value);
				}
			}
			else {
				// Last Token Type was null, "-" must be a unary operator
				token.setType(TokenType.UNARYMINUS);
				token.setValue(value);
			}
		} else if (value.equals("*")) {
			//"Multiplication Case"
			token.setType(TokenType.MULOP);
			token.setValue("1");
		} else if (value.equals("/")) {
			//"Division Case"
			token.setType(TokenType.MULOP);
			token.setValue("2");
		} else if (value.equals(",")) {
			//Comma
			token.setType(TokenType.COMMA);
			token.setValue(value);
		} else if (value.equals("(")) {
			//Left Parenthesis
			token.setType(TokenType.LEFTPAREN);
			token.setValue(value);
		} else if (value.equals(")")) {
			//Right Parenthesis
			token.setType(TokenType.RIGHTPAREN);
			token.setValue(value);
		} else if (value.equals("}")) {
			//Stream skips comments
			//A right curly brace at this point would be a bad comment error. Missing Left Curly Brace
			throw LexicalError.MissingBrace(getLineNumber());
		}
		else if (value.equals(";")) {
			//Semicolon
			token.setType(TokenType.SEMICOLON);
			token.setValue(value);
		} else if (value.equals(":")) {
			//Need a lookahead to determine specific case
			Character lookahead = stream.currentChar();
			String lookaheadValue = Character.toString(lookahead);

			if (lookaheadValue.equals("=")) {
				//"Assignment Operator" Case
				token.setType(TokenType.ASSIGNOP);
				token.setValue(":=");
			}
			else {
				// "Colon" Case, push lookahead back
				stream.pushBack(lookahead);
				token.setType(TokenType.COLON);
				token.setValue(value);
			}
		} else if (value.equals(".")) {
			//Need a lookahead to determine specific case
			Character lookahead = stream.currentChar();
			String lookaheadValue = Character.toString(lookahead);

			if (lookaheadValue.equals(".")) {
				//"Doubledot" Case
				token.setType(TokenType.DOUBLEDOT);
				token.setValue("..");
			}
			else {
				//"Endmarker" Case, push lookahead back
				stream.pushBack(lookahead);
				token.setType(TokenType.ENDMARKER);
				token.setValue(value);
			}
		} else if (value.equals("[")){
			// Left Bracket
			token.setType(TokenType.LEFTBRACKET);
			token.setValue(value);
		} else {
			// Right Bracket
			token.setType(TokenType.RIGHTBRACKET);
			token.setValue(value);
		}

		lastTokenType = token.getType();

		return token;
	}
}