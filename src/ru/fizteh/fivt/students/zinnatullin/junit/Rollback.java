package ru.fizteh.fivt.students.zinnatullin.junit;

import java.io.IOException;

import ru.fizteh.fivt.students.zinnatullin.basicclasses.BasicDataBaseState;
import ru.fizteh.fivt.students.zinnatullin.basicclasses.BasicTable;
import ru.fizteh.fivt.students.zinnatullin.multifilehashmap.BasicMultiFileHashMapCommand;
import ru.fizteh.fivt.students.zinnatullin.shell.Shell;

public class Rollback<ElementType, TableType extends BasicTable<ElementType>> extends BasicMultiFileHashMapCommand<ElementType, TableType> {
	public Rollback(BasicDataBaseState<ElementType, TableType> dataBaseState) {
		super(dataBaseState, "rollback", 0);
	}	
	public void executeCommand(String[] arguments, Shell usedShell) throws IOException {
		usedShell.curShell.getOutStream().println(dataBaseState.getUsedTable().rollback());
	}	
}
