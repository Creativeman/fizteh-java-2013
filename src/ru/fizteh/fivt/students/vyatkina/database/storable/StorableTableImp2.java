package ru.fizteh.fivt.students.vyatkina.database.storable;


import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.students.vyatkina.database.StorableTable;
import ru.fizteh.fivt.students.vyatkina.database.superior.DatabaseUtils;
import ru.fizteh.fivt.students.vyatkina.database.superior.TableProviderUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static ru.fizteh.fivt.students.vyatkina.database.superior.TableProviderUtils.createFileForKeyIfNotExists;
import static ru.fizteh.fivt.students.vyatkina.database.superior.TableProviderUtils.fileForKey;

public class StorableTableImp2 implements StorableTable {

    private final String name;
    private final StorableTableProviderImp tableProvider;
    private final StorableRowShape shape;
    private AtomicBoolean isClosed = new AtomicBoolean(false);
    private Map<String, Storeable> mainMap;
    private ThreadLocal<Map<String, Storeable>> localMap = new ThreadLocal<Map<String, Storeable>>() {
        protected Map<String, Storeable> initialValue() {
            return new HashMap<>();
        }
    };
    private final ReadWriteLock tableKeeper = new ReentrantReadWriteLock(true);

    public StorableTableImp2(String name, StorableRowShape shape, StorableTableProviderImp tableProvider) {
        this.name = name;
        this.shape = shape;
        this.tableProvider = tableProvider;
    }

    @Override
    public String getName() {
        isClosedCheck();
        return name;
    }

    @Override
    public Storeable get(String key) {
        if (localMap.get().containsKey(key)) {
            return localMap.get().get(key);
        } else {
            try {
                tableKeeper.readLock().lock();
                return mainMap.get(key);
            }
            finally {
                tableKeeper.readLock().unlock();
            }
        }
    }

    @Override
    public Storeable put(String key, Storeable value) throws ColumnFormatException {
        Storeable oldValue = null;
        try {
            tableKeeper.readLock().lock();
            if (localMap.get().containsKey(key)) {
                oldValue = localMap.get().get(key);
            } else if (mainMap.containsKey(key)) {
                oldValue = mainMap.get(key);
            }
        }
        finally {
            tableKeeper.readLock().unlock();
        }
        localMap.get().put(key, value);
        return oldValue;
    }

    @Override
    public Storeable remove(String key) {
        Storeable oldValue = null;
        try {
            tableKeeper.readLock().lock();
            if (localMap.get().containsKey(key)) {
                oldValue = localMap.get().get(key);
            } else if (mainMap.containsKey(key)) {
                oldValue = mainMap.get(key);
            }
        }
        finally {
            tableKeeper.readLock().unlock();
        }
        localMap.get().put(key, null);
        return oldValue;
    }

    @Override
    public int size() {
        int size = mainMap.size();
        try {
            tableKeeper.readLock().lock();
            for (Map.Entry<String, Storeable> entry : localMap.get().entrySet()) {
                if (!mainMap.containsKey(entry.getKey())) {
                    if (entry.getValue() != null) {
                        ++size;
                    }
                } else {
                    if (entry.getValue() == null) {
                        --size;
                    }
                }
            }
        }
        finally {
            tableKeeper.readLock().unlock();
        }
        return size;
    }

    @Override
    public int commit() throws IOException {
        Map<Path, List<DatabaseUtils.KeyValue>> databaseChanges = new HashMap<>();
        Path tableLocation = tableProvider.tableDirectory(name);
        int commitChanges = difference();
        try {
            tableKeeper.writeLock().lock();
            Set<Path> filesChanged = mergeTables();
            for (Path file : filesChanged) {
                Files.deleteIfExists(file);
                databaseChanges.put(file, new ArrayList<DatabaseUtils.KeyValue>());
            }

            for (Map.Entry<String, Storeable> entry : mainMap.entrySet()) {
                Path fileKeyIn = fileForKey(entry.getKey(), tableLocation);
                if (filesChanged.contains(fileKeyIn)) {
                    createFileForKeyIfNotExists(entry.getKey(), tableLocation);
                    String value = tableProvider.serialize(this, entry.getValue());
                    DatabaseUtils.KeyValue keyValue = new DatabaseUtils.KeyValue(entry.getKey(), value);
                    databaseChanges.get(fileKeyIn).add(keyValue);
                }
            }

            TableProviderUtils.writeTable(databaseChanges);
        }
        finally {
            tableKeeper.writeLock().unlock();
        }
        return commitChanges;
    }

    private Set<Path> mergeTables() {
        Set<Path> filesChanged = new HashSet<>();
        Path tableLocation = tableProvider.tableDirectory(name);
        for (Map.Entry<String, Storeable> entry : localMap.get().entrySet()) {
            if (mainMap.containsKey(entry.getKey()) && !mainMap.get(entry.getKey()).equals(entry.getValue())) {
                if (entry.getValue() != null) {
                    mainMap.put(entry.getKey(), entry.getValue());
                } else {
                    mainMap.remove(entry.getKey());
                }
                filesChanged.add(fileForKey(entry.getKey(), tableLocation));
            } else {
                if (entry.getValue() != null) {
                    mainMap.put(entry.getKey(), entry.getValue());
                    filesChanged.add(fileForKey(entry.getKey(), tableLocation));
                }
            }
        }
        localMap.get().clear();
        return filesChanged;
    }

    @Override
    public int rollback() {
        int rollbackSize = difference();
        localMap.get().clear();
        return rollbackSize;
    }

    public int difference() {
        int diff = 0;
        try {
            tableKeeper.readLock().lock();
            for (Map.Entry<String, Storeable> entry : localMap.get().entrySet()) {
                if (mainMap.containsKey(entry.getKey())) {
                    if (!mainMap.get(entry.getKey()).equals(entry.getValue())) {
                        ++diff;
                    }
                } else {
                    if (entry.getValue() != null) {
                        ++diff;
                    }
                }
            }
        }
        finally {
            tableKeeper.readLock().unlock();
        }
        return diff;
    }

    @Override
    public int unsavedChanges() {
        return difference();
    }

    @Override
    public void putValuesFromDisk(Map<String, Storeable> diskValues) {
        try {
            tableKeeper.writeLock().lock();
            this.mainMap = diskValues;
        }
        finally {
            tableKeeper.writeLock().unlock();
        }
    }

    @Override
    public int getColumnsCount() {
        isClosedCheck();
        return shape.getColumnsCount();
    }

    @Override
    public Class<?> getColumnType(int columnIndex) throws IndexOutOfBoundsException {
        isClosedCheck();
        return shape.getColumnType(columnIndex);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + tableProvider.tableDirectory(name) + "]";
    }

    @Override
    public void close() {
        rollback();
        tableProvider.removeReference(this);
        isClosed.set(true);
    }

    private void isClosedCheck() {
        if (isClosed.get()) {
            throw new IllegalStateException("Table " + name + "is closed");
        }
    }
}
