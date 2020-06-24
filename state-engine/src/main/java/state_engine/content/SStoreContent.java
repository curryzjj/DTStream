package state_engine.content;
import state_engine.Meta.MetaTypes;
import state_engine.common.OrderLock;
import state_engine.storage.SchemaRecord;
import state_engine.storage.datatype.DataBox;
import state_engine.transaction.impl.TxnContext;

import java.util.List;
public abstract class SStoreContent implements Content {
    public final static String SStore_Content = "SStoreContent";
    @Override
    public boolean RequestReadAccess(long timestamp, List<DataBox> data, boolean[] is_ready) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void RequestCommit(long timestamp, boolean[] is_ready) {
        throw new UnsupportedOperationException();
    }
    @Override
    public boolean RequestWriteAccess(long timestamp, List<DataBox> data) {
        throw new UnsupportedOperationException();
    }
    //not in use.
    @Override
    public void RequestAbort(long timestamp) {
        throw new UnsupportedOperationException();
    }
    @Override
    public long GetLWM() {
        throw new UnsupportedOperationException();
    }
    //	@Override
//	public LWMContentImpl.XLockQueue GetXLockQueue() {
//		return null;
//	}
//
    @Override
    public SchemaRecord ReadAccess(TxnContext context, MetaTypes.AccessType accessType) {
        throw new UnsupportedOperationException();
    }
    @Override
    public SchemaRecord readValues(long ts, long previous_mark_ID, boolean clean) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void updateValues(long ts, long previous_mark_ID, boolean clean, SchemaRecord record) {
        throw new UnsupportedOperationException();
    }
    @Override
    public boolean AcquireCertifyLock() {
        throw new UnsupportedOperationException();
    }
    @Override
    public void WriteAccess(long commit_timestamp, long mark_ID, boolean clean, SchemaRecord local_record_) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void ReleaseCertifyLock() {
        throw new UnsupportedOperationException();
    }
    @Override
    public void AddLWM(long ts) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void DeleteLWM(long ts) {
        throw new UnsupportedOperationException();
    }
    @Override
    public boolean TryReadLock() {
        throw new UnsupportedOperationException();
    }
    @Override
    public boolean TryWriteLock() {
        throw new UnsupportedOperationException();
    }
    @Override
    public void ReleaseReadLock() {
        throw new UnsupportedOperationException();
    }
    @Override
    public void ReleaseWriteLock() {
        throw new UnsupportedOperationException();
    }
    @Override
    public boolean TryWriteLock(OrderLock lock, TxnContext txn_context) {
        throw new UnsupportedOperationException();
    }
    @Override
    public boolean TryReadLock(OrderLock lock, TxnContext txn_context) {
        throw new UnsupportedOperationException();
    }
    @Override
    public boolean AcquireReadLock() {
        throw new UnsupportedOperationException();
    }
    @Override
    public boolean AcquireWriteLock() {
        throw new UnsupportedOperationException();
    }
}
