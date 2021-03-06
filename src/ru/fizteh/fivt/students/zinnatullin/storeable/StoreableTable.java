package ru.fizteh.fivt.students.zinnatullin.storeable;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.students.zinnatullin.basicclasses.BasicTable;

public class StoreableTable extends BasicTable<Storeable> implements Table, AutoCloseable {
	private StoreableTableProvider currentProvider;
	private List<Class<?>> columnClasses;
	
	public StoreableTable(String path, String tableName, StoreableTableProvider currentProvider) throws IOException {
		super(path, tableName);
		columnClasses = StoreableUtils.getClasses(path);
		
		this.currentProvider = currentProvider;
	}

	public StoreableTable(String path, String tableName, StoreableTableProvider currentProvider, List<Class<?>> columnClasses) throws IOException {
		super(path, tableName);
		this.columnClasses = new ArrayList<>();
		this.columnClasses.addAll(columnClasses);
		
		this.currentProvider = currentProvider;
	}

	public Storeable put(String key, Storeable value) throws ColumnFormatException {
		if ((key == null) || (key.trim().isEmpty()) || (value == null)) {
			throw new IllegalArgumentException("null or empty parameter");
		}
		try {   
			for (int column = 0; column < columnClasses.size(); ++column) {
				Object valueColumn = value.getColumnAt(column);
				Object appropriateClass = columnClasses.get(column);
				if ((valueColumn != null) && (!(valueColumn.getClass().equals(appropriateClass)))) {
					throw new ColumnFormatException("incorrect column " + column + " format");
				}
			}
		} catch (IndexOutOfBoundsException catchedException) {
			throw new ColumnFormatException("wrong storeable");
		}
		
		try {
			value.getColumnAt(columnClasses.size());
		} catch (IndexOutOfBoundsException catchedException) {
			return super.put(key, value);
		}
		throw new ColumnFormatException("wrong storeable");
	}
	
	public int getColumnsCount() {
	    tableCloseCheck();
		return columnClasses.size();
	}

	public Class<?> getColumnType(int columnIndex) throws IndexOutOfBoundsException {
	    tableCloseCheck();
		return columnClasses.get(columnIndex);
	}

	public String serialize(Storeable value) {
		try {
			return currentProvider.serialize(this, value);
		} catch (ColumnFormatException catchedException) {
			throw new WrongTypeException(catchedException);
		}
	}
	
	public Storeable deserialize(String value) {
		try {
			return currentProvider.deserialize(this, value);
		} catch (ParseException catchedException) {
			throw new WrongTypeException(catchedException);
		}
	}
	
    public String toString() {
        return getClass().getSimpleName() + "[" + (new File(tablePath)).getAbsolutePath() + "]";
    }
    
    public void close() {
        if (!(closeIndicator)) {      
            rollback();
            currentProvider.closeTable(tableName);
            closeIndicator = true;
        }
    }
}
