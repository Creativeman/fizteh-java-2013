package ru.fizteh.fivt.students.irinaGoltsman.multifilehashmap.utests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.irinaGoltsman.multifilehashmap.DBStoreable;
import ru.fizteh.fivt.students.irinaGoltsman.multifilehashmap.DBTableProvider;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class ParallelTableTest {
    private static Table table;
    private static TableProvider tableProvider;
    private List<Class<?>> columnTypes = new ArrayList<>();
    @Rule
    public TemporaryFolder rootDBDirectory = new TemporaryFolder();

    @Before
    public void createTable() throws IOException, ParseException {
        columnTypes.add(Integer.class);
        tableProvider = new DBTableProvider(rootDBDirectory.newFolder());
        table = tableProvider.createTable("testTable", columnTypes);
    }

    @Test
    public void concurrentPutAndCommit() throws IOException{
        Thread thread1 = new Thread(new Runnable() {
            public void run() {
                try{
                Thread.sleep(1000);
                } catch (InterruptedException e){
                    //
                }
                Storeable row = new DBStoreable(columnTypes);
                row.setColumnAt(0, 1);
                table.put("key1", row);
                Assert.assertEquals(1, table.size());
                try {
                    table.commit();
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                    return;
                }
                table.put("key2", row);
                Assert.assertEquals(2, table.size());
                try {
                    table.commit();
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        });
        Thread thread2 = new Thread(thread1);
        Thread thread3 = new Thread(thread1);
        thread1.start();
        thread2.start();
        thread3.start();
        try {
            thread1.join();
            thread2.join();
            thread3.join();
        } catch (InterruptedException e) {
            //
        }
        table.remove("key1");
        table.remove("key2");
        Assert.assertEquals(2, table.commit());
    }
}
