package symbolTable;

import java.util.ArrayList;
import java.util.List;
import lex.TokenType;

public class FunctionEntry extends SymbolTableEntry
{
    public String name; // Name of function
    public int numberOfParameters; //Number of parameters required to execute this function
    public ArrayList<SymbolTableEntry> parameterInfo; //Contains function's parameter details
    public VariableEntry result; //Will have variable entry with a specific type. This entry will be returned when function is called

    public FunctionEntry(String name)
    {
        super(name);
        parameterInfo = new ArrayList<>();
    }

    public FunctionEntry(String name, int numberOfParameters, ArrayList parameterInfo, VariableEntry result)
    {
        super(name);
        this.numberOfParameters = numberOfParameters;
        this.parameterInfo = parameterInfo;
        this.result = result;
    }

    public void addParamInfo(ArrayList info)
    {
        //Add new parameter details
        parameterInfo.addAll(info);
    }

    public void setResult(VariableEntry res)
    {
        result = res;
    }

    public void setResultType(TokenType type)
    {
        result.setType(type);
    }

    public VariableEntry getResult()
    {
        return result;
    }

    public TokenType getResultType()
    {
        return result.getType();
    }

    public void setNumberOfParameters(int num)
    {
        numberOfParameters = num;
    }

    public int getNumberOfParameters()
    {
        return numberOfParameters;
    }

    @Override
    public boolean isFunction() {
        return true;
    }
}