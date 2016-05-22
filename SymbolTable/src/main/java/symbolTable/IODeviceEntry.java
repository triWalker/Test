package symbolTable;


public class IODeviceEntry extends SymbolTableEntry
{
    private String name; //Name of io device
    private Boolean reserved;

    public IODeviceEntry(String name)
    {
        super(name);

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
    public boolean isVariable() {
        return true;
    }
}