package symbolTable;


import lex.TokenType;

public class ArrayEntry extends SymbolTableEntry
{
    private String name;
    private int address, upperBound, lowerBound;
    private TokenType type;
    private boolean param = false; // Will be true if this array entry is a parameter

    public ArrayEntry(String name)
    {
        super(name);
    }

    public ArrayEntry(String name, int address, TokenType type, int upperBound, int lowerBound)
    {
        super(name, type);
        this.address = address;
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
    }

    public int getAddress()
    {
        return address;
    }

    public int getLBound()
    {
        return lowerBound;
    }

    public int getUBound()
    {
        return upperBound;
    }

    public void setAsParam(boolean bool)
    {
        param = bool;
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public boolean isParameter()
    {
        return param;
    }
}