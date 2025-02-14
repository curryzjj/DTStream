package storage;

import common.collections.Configuration;
import common.collections.OsUtils;
import db.DatabaseException;
import durability.ftmanager.FTManager;
import durability.snapshot.SnapshotOptions;
import durability.snapshot.SnapshotResult.SnapshotResult;
import durability.snapshot.SnapshotStrategy.ImplSnapshotStrategy.InMemorySnapshotStrategy;
import storage.datatype.DataBox;
import storage.table.BaseTable;
import storage.table.RecordSchema;
import storage.table.ShareTable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class StorageManager {
    /**
     * Add for snapshot
     * */
    private final InMemorySnapshotStrategy snapshotStrategy;
    public Map<String, BaseTable> tables;
    int table_count;

    public StorageManager(Configuration configuration) {
        tables = new ConcurrentHashMap<>();
        snapshotStrategy = new InMemorySnapshotStrategy(tables,
                new SnapshotOptions(configuration.getInt("parallelNum"), "None"),
                configuration.getString("rootFilePath") + OsUtils.OS_wrapper("snapshot"));
    }

    public BaseTable getTable(String tableName) throws DatabaseException {
        if (!tables.containsKey(tableName)) {
            throw new DatabaseException("Table: " + tableName + " does not exist");
        }
        return tables.get(tableName);
    }

    public void InsertRecord(String tableName, TableRecord record, int partition_id) throws DatabaseException {
        BaseTable tab = getTable(tableName);
        tab.InsertRecord(record, partition_id);
    }

    /**
     * Create a new table in this database.
     *
     * @param s         the table schema
     * @param tableName the name of the table
     * @throws DatabaseException
     */
    public synchronized void createTable(RecordSchema s, String tableName, int partition_num, int num_items) throws DatabaseException {
        if (tables.containsKey(tableName)) {
            throw new DatabaseException("Table name already exists");
        }
        tables.put(tableName, new ShareTable(s, tableName, true, partition_num, num_items));//here we decide which table to use.
        this.snapshotStrategy.registerTable(tableName, s);
        table_count++;
    }

    /**
     * TODO: to be implemented.
     * Create a new table in this database with an index on each of the given column names.
     * NOTE: YOU CAN NOT DELETE/UPDATE FROM THIS TABLE IF YOU CHOOSE TO BUILD INDICES!!
     *
     * @param s            the table schema
     * @param tableName    the name of the table
     * @param indexColumns the list of unique columnNames on the maintain an index on
     * @throws DatabaseException
     */
    public synchronized void createTableWithIndices(RecordSchema s, String tableName, List<String> indexColumns, int partition_num, int num_items) throws DatabaseException {
        if (tables.containsKey(tableName)) {
            throw new DatabaseException("SimpleTable name already exists");
        }
        List<String> schemaColNames = s.getFieldNames();
        List<DataBox> schemaColType = s.getFieldTypes();
        HashSet<String> seenColNames = new HashSet<>();
        List<Integer> schemaColIndex = new ArrayList<>();
        for (String col : indexColumns) {
            if (!schemaColNames.contains(col)) {
                throw new DatabaseException("Column desired for index does not exist");
            }
            if (seenColNames.contains(col)) {
                throw new DatabaseException("Column desired for index has been duplicated");
            }
            seenColNames.add(col);
            schemaColIndex.add(schemaColNames.indexOf(col));
        }
        tables.put(tableName, new ShareTable(s, tableName, true, partition_num, num_items));
        for (int i : schemaColIndex) {
            String colName = schemaColNames.get(i);
            DataBox colType = schemaColType.get(i);
            String indexName = tableName + "," + colName;
            //this.indexLookup.put(indexName, new BtreeIndex(colType, indexName, this.fileDir));
        }
    }

    /**
     * Delete a table in this database.
     *
     * @param tableName the name of the table
     * @return true if the database was successfully deleted
     */
    public synchronized boolean dropTable(String tableName) throws IOException {
        if (!tables.containsKey(tableName)) {
            return false;
        }
        tables.get(tableName).close();
        tables.remove(tableName);
        return true;
    }

    /**
     * Delete all tables from this database.
     */
    public synchronized void dropAllTables() throws IOException {
        List<String> tableNames = new ArrayList<>(tables.keySet());
        for (String s : tableNames) {
            dropTable(s);
        }
    }

    public void asyncSnapshot(long snapshotId, int partitionId, FTManager ftManager) throws IOException {
        this.snapshotStrategy.asyncSnapshot(snapshotId, partitionId, ftManager);
    }
    public void syncReloadDatabase(SnapshotResult snapshotResult) throws IOException, ExecutionException, InterruptedException {
        this.snapshotStrategy.syncRecoveryFromSnapshot(snapshotResult);
    }

    /**
     * Close this database.
     */
    public synchronized void close() throws IOException {
        for (BaseTable t : tables.values()) {
            t.close();
        }
        tables.clear();
    }
}
