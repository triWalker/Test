package symbolTable;

import lex.TokenType;

public class VariableEntry extends SymbolTableEntry
{
    private String name; //Name of variable
    private int address; //Position in memory
    private TokenType type;
    private boolean funcResult = false; // Will be true if this variable entry is being used as a function result
    private boolean param = false; // Will be true if this variable entry is a parameter

    public VariableEntry(String name)
    {
        super(name);
    }

    public VariableEntry(String name, int address, TokenType type)
    {
        super(name, type);
        this.address = address;
    }

    public int getAddress()
    {
        return address;
    }

    public void setAsFuncResult(boolean bool)
    {
        funcResult = bool;
    }

    public void setAsParam(boolean bool)
    {
        param = bool;
    }

    public void setType(TokenType tknType)
    {//Can only change type if variable is a function result
        if(funcResult)
            type = tknType;
        else
            System.out.println("Variable is not a function result, cannot change its type.");
    }

    @Override
    public boolean isVariable() {
        return true;
    }

    @Override
    public boolean isFunctionResult(){
        return funcResult;
    }

    @Override
    public boolean isParameter()
    {
        return param;
    }
}