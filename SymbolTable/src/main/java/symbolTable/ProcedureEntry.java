package symbolTable;

import java.util.ArrayList;


public class ProcedureEntry extends SymbolTableEntry
{
    public String name; //Name of procedure
    public int numberOfParameters; //Number of parameters required to execute this procedure
    public ArrayList<SymbolTableEntry> parameterInfo; //Contains procedure's parameter details
    public Boolean reserved; // Will be true if procedure entry has a special name, like "read" or "write" or the name of the program

    public ProcedureEntry(String name, int numberOfParameters)
    {
        super(name);
        this.numberOfParameters = numberOfParameters;
        parameterInfo = new ArrayList<>();
    }

    public ProcedureEntry(String name, int numberOfParameters, ArrayList parameterInfo)
    {
        super(name);
        this.numberOfParameters = numberOfParameters;
        this.parameterInfo = parameterInfo;
    }

    public void setNumberOfParameters(int num)
    {
        numberOfParameters = num;
    }

    public int getNumberOfParameters()
    {
        return numberOfParameters;
    }

    public void addParamInfo(ArrayList info)
    {
        //Add new parameter details
        parameterInfo.addAll(info);
    }

    public void setReserved(Boolean bool)
    {
        reserved = bool;
    }

    @Override
    public boolean isReserved() {
        return reserved;
    }

    @Override
    public boolean isProcedure() {
        return true;
    }
}