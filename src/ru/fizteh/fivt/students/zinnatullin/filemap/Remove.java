package ru.fizteh.fivt.students.zinnatullin.filemap;

import java.io.IOException;

import ru.fizteh.fivt.students.zinnatullin.basicclasses.BasicState;
import ru.fizteh.fivt.students.zinnatullin.shell.Shell;
import ru.fizteh.fivt.students.zinnatullin.storeable.WrongTypeException;

public class Remove extends BasicFileMapCommand {
	public Remove(BasicState currentState) {
		super(currentState, "remove", 1);
	}
	public void executeCommand(String[] arguments, Shell usedShell) throws IOException {
		String value = null;
		try {
			value = currentState.remove(arguments[0]);
		} catch (WrongTypeException catchedException) {
			throw new IOException("wrong type (" + catchedException.getMessage() + ")");
		}
		
		if (value == null) {
			usedShell.curShell.getOutStream().println("not found");
		} else {
			usedShell.curShell.getOutStream().println("removed");
		}
	}	
}