package ru.fizteh.fivt.students.zinnatullin.basicclasses;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ru.fizteh.fivt.students.zinnatullin.storeable.WrongTypeException;

abstract public class BasicTable<ElementType> {
	private volatile FilesMap<ElementType> filesMap;
	private ThreadLocal<HashMap<String, ElementType>> putDiff;
	private ThreadLocal<HashSet<String>> removeDiff;
	private ReadWriteLock readWriteLock;
	protected boolean closeIndicator;
	protected String tablePath;
	protected String tableName;   

	public BasicTable(String path, String tableName) throws IOException {
		this.filesMap = new FilesMap<ElementType>(path, this);
		this.tableName = tableName;
		
		tablePath = path;
		
		readWriteLock = new ReentrantReadWriteLock(true);
		closeIndicator = false;
		
		putDiff = new ThreadLocal<HashMap<String, ElementType>>() {
		    protected HashMap<String, ElementType> initialValue() {
		        return new HashMap<String, ElementType>();
		    }
		};
		removeDiff = new ThreadLocal<HashSet<String>>() {
		    protected HashSet<String> initialValue() {
		        return new HashSet<String>();
		        }
		};
	}
	
	public String getName() {
	    tableCloseCheck();
		return tableName;
	}

	public ElementType get(String key) {
	    tableCloseCheck();
	    
		checkKey(key);
		
		if (removeDiff.get().contains(key)) {
			return null;
		}
		if (putDiff.get().containsKey(key)) {
		    return putDiff.get().get(key);
		}
		
		readWriteLock.readLock().lock();      
        try {
            return filesMap.getFileMapForKey(key).getCurrentTable().get(key);
        } finally {
            readWriteLock.readLock().unlock();
        }
	}

	public ElementType put(String key, ElementType value) {
	    tableCloseCheck();
	    
		checkKey(key);
		if (value == null) {
			throw new IllegalArgumentException("null or empty parameter");
		}
				
		ElementType overwrite = get(key);
		
		putDiff.get().put(key, value);
		removeDiff.get().remove(key);
		
		return overwrite;
	}
	
	public ElementType remove(String key) {
	    tableCloseCheck();
	    
		checkKey(key);
		ElementType removed = get(key);
		if (removed != null) {
			removeDiff.get().add(key);
		}
		putDiff.get().remove(key);
		return removed;
	}

	public int size() {
	    tableCloseCheck();
	    
		readWriteLock.readLock().lock();
		
		int previousSize = filesMap.getSize();
		for (String key: putDiff.get().keySet()) {
			if (filesMap.getFileMapForKey(key).getCurrentTable().get(key) == null) {
				++previousSize;
			}
		}	
		Iterator<String> removeDiffIterator = removeDiff.get().iterator();
	    while(removeDiffIterator.hasNext()){
	        String key = removeDiffIterator.next();
	        if (filesMap.getFileMapForKey(key).getCurrentTable().get(key) != null) {
	        	--previousSize;
	        }
	    }
	    
	    readWriteLock.readLock().unlock();
	    
	    return previousSize;
	}

	public int commit() throws IOException {
	    tableCloseCheck();
	    
		readWriteLock.writeLock().lock();	 
		int changesNumber = getChangesNumber();
		if (changesNumber != 0) {
			autoCommit();
		}	
		filesMap.writeData();  
		readWriteLock.writeLock().unlock();
		
		putDiff.get().clear();
		removeDiff.get().clear();
		
		return changesNumber;		
	}
	
	public void autoCommit() { 
		for (String key: putDiff.get().keySet()) {
			filesMap.getFileMapForKey(key).getCurrentTable().put(key, putDiff.get().get(key));
		}		
		Iterator<String> removeDiffIterator = removeDiff.get().iterator();
	    while(removeDiffIterator.hasNext()){
	        String key = removeDiffIterator.next();
	        filesMap.getFileMapForKey(key).getCurrentTable().remove(key);
	    }
	}
	
	public int rollback() {
	    tableCloseCheck();
	    
		int changesNumber = getChangesNumber();
		
		putDiff.get().clear();
		removeDiff.get().clear();
		return changesNumber;  
	}

	public int getChangesNumber() {
	    tableCloseCheck();
	    
		int changesNumber = 0;
		Iterator<String> removeDiffIterator = removeDiff.get().iterator();
		
		readWriteLock.readLock().lock();
		try {
		    while(removeDiffIterator.hasNext()) {
		        String key = removeDiffIterator.next();
		        if (filesMap.getFileMapForKey(key).getCurrentTable().get(key) != null) {
		            ++changesNumber;
		        }
		    }
		    for (String key: putDiff.get().keySet()) {
		        try {
		            if (!serialize((putDiff.get().get(key))).equals(serialize(filesMap.getFileMapForKey(key).getCurrentTable().get(key)))) {
		                ++changesNumber;
		            }
		        } catch (IOException catchedException) {
		            throw new WrongTypeException(catchedException.getMessage());
		        }
		    }	
		} finally {
		    readWriteLock.readLock().unlock();
		}
	    
		return changesNumber;
	}
	
	public FilesMap<ElementType> getFilesMap() {
		return filesMap;
	}
	
	private void checkKey(String key) {
		if ((key == null) || (key.trim().isEmpty())) {
			throw new IllegalArgumentException("null or empty key");
		}
		
		Pattern pattern = null;
        pattern = Pattern.compile("\\s+");
        Matcher matcher = pattern.matcher(key);
		
		if (matcher.find()) {
			throw new IllegalArgumentException("incorrect key");
		}
	}
	
    protected void tableCloseCheck() {
        if (closeIndicator) {
            throw new IllegalStateException("table is closed");
        }
    }
	
    public abstract String serialize(ElementType value) throws IOException;	
    public abstract ElementType deserialize(String value) throws IOException;
}
