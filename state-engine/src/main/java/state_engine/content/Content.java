package state_engine.content;
import state_engine.Meta.MetaTypes;
import state_engine.common.OrderLock;
import state_engine.storage.SchemaRecord;
import state_engine.storage.datatype.DataBox;
import state_engine.transaction.impl.TxnContext;

import java.util.List;
public interface Content {
    int CCOption_LOCK = 0;
    int CCOption_OrderLOCK = 1;
    int CCOption_LWM = 2;
    int CCOption_TStream = 3;
    int CCOption_SStore = 4;
    int CCOption_OTS = 5;//ordered timestamp
    boolean TryReadLock();
    boolean TryWriteLock();
    void SetTimestamp(long timestamp);
    long GetTimestamp();
    void ReleaseReadLock();
    void ReleaseWriteLock();
    /**
     * new API for ordering guarantee
     */
    boolean TryWriteLock(OrderLock lock, TxnContext txn_context) throws InterruptedException;
    boolean TryReadLock(OrderLock lock, TxnContext txn_context) throws InterruptedException;
    boolean AcquireReadLock();
    boolean AcquireWriteLock();
    //TO.
    boolean RequestWriteAccess(long timestamp, List<DataBox> data);
    boolean RequestReadAccess(long timestamp, List<DataBox> data, boolean[] is_ready);
    void RequestCommit(long timestamp, boolean[] is_ready);
    void RequestAbort(long timestamp);
    //LWM
    long GetLWM();
    //	LWMContentImpl.XLockQueue GetXLockQueue();
    SchemaRecord ReadAccess(TxnContext context, MetaTypes.AccessType accessType);
    SchemaRecord readPreValues(long ts);
    SchemaRecord readValues(long ts, long previous_mark_ID, boolean clean);
    void clean_map(long mark_ID);
    void updateValues(long ts, long previous_mark_ID, boolean clean, SchemaRecord record);
    void updateMultiValues(long ts, long previous_mark_ID, boolean clean, SchemaRecord record);
    boolean AcquireCertifyLock();
    SchemaRecord ReadAccess(long ts, long mark_ID, boolean clean, MetaTypes.AccessType accessType);
    void WriteAccess(long commit_timestamp, long mark_ID, boolean clean, SchemaRecord local_record_);
    void ReleaseCertifyLock();
    void AddLWM(long ts);
    void DeleteLWM(long ts);
    boolean TryLockPartitions();
    void LockPartitions();
    void UnlockPartitions();
}
