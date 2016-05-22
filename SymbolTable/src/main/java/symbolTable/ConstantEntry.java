package symbolTable;

import lex.TokenType;

public class ConstantEntry extends SymbolTableEntry
{
    private String name;
    private TokenType type;

    public ConstantEntry(String name)
    {
        super(name);
    }

    public ConstantEntry(String name, TokenType type)
    {
        super(name, type);
    }

    @Override
    public boolean isConstant() {
        return true;
    }
}