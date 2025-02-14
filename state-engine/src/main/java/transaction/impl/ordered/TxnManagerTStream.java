package transaction.impl.ordered;

import db.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import profiler.MeasureTools;
import profiler.Metrics;
import scheduler.impl.recovery.RScheduler;
import storage.SchemaRecord;
import storage.StorageManager;
import storage.TableRecord;
import transaction.context.TxnContext;
import transaction.impl.TxnManagerDedicatedAsy;
import utils.SOURCE_CONTROL;

import java.util.LinkedList;
import java.util.concurrent.BrokenBarrierException;

import static content.common.CommonMetaTypes.AccessType.INSERT_ONLY;
import static content.common.ContentCommon.loggingRecord_type;
import static transaction.context.TxnAccess.Access;
import static utils.FaultToleranceConstants.LOGOption_path;

public class TxnManagerTStream extends TxnManagerDedicatedAsy {
    private static final Logger log = LoggerFactory.getLogger(TxnManagerTStream.class);
    public TxnManagerTStream(StorageManager storageManager, String thisComponentId, int thisTaskId, int numberOfStates, int thread_countw, String schedulerType) {
        super(storageManager, thisComponentId, thisTaskId, thread_countw, numberOfStates, schedulerType);
    }

    @Override
    public boolean InsertRecord(TxnContext txn_context, String table_name, SchemaRecord record, LinkedList<Long> gap) throws DatabaseException {
        record.is_visible_ = false;
        TableRecord tb_record = new TableRecord(record, this.thread_count_);
        if (storageManager_.getTable(table_name).InsertRecord(tb_record)) {//maybe we can also skip this for testing purpose.
            if (!tb_record.content_.TryWriteLock()) {
                return false;
            } else {
            }
            record.is_visible_ = true;
            Access access = access_list_.NewAccess();
            access.access_type_ = INSERT_ONLY;
            access.access_record_ = tb_record;
            access.local_record_ = null;
            access.table_id_ = table_name;
            access.timestamp_ = 0;
            return true;
        } else {
            return true;
        }
    }

    /**
     * This is the API: SP-Layer inform the arrival of checkpoint, which informs the TP-Layer to start evaluation.
     *
     * @param thread_Id
     * @param mark_ID
     * @param num_events
     * @return time spend in tp evaluation.
     */
    @Override
    public void start_evaluate(int thread_Id, long mark_ID, int num_events) throws InterruptedException, BrokenBarrierException {
        SOURCE_CONTROL.getInstance().preStateAccessBarrier(thread_Id);//sync for all threads to come to this line to ensure chains are constructed for the current batch.
        MeasureTools.BEGIN_TXN_TIME_MEASURE(thread_Id);
        MeasureTools.BEGIN_SCHEDULE_EXPLORE_TIME_MEASURE(thread_Id);
        if (enableGroup) {
            schedulerByGroup.get(getGroupId(thread_Id)).start_evaluation(context, mark_ID, num_events);
        } else {
            scheduler.start_evaluation(context, mark_ID, num_events);
        }
        MeasureTools.END_SCHEDULE_EXPLORE_TIME_MEASURE(thread_Id);
        MeasureTools.SCHEDULE_TIME_RECORD(thread_Id, num_events);
        MeasureTools.END_TXN_TIME_MEASURE(thread_Id);
        SOURCE_CONTROL.getInstance().postStateAccessBarrier(thread_Id);
        //Sync to switch scheduler(more overhead) decide by the mark_ID or runtime information
        MeasureTools.BEGIN_SCHEDULER_SWITCH_TIME_MEASURE(thread_Id);
        if (enableDynamic && collector.timeToSwitch(mark_ID, thread_Id, currentSchedulerType.get(thread_Id))) {
            if (loggingRecord_type != LOGOption_path || Metrics.RecoveryPerformance.stopRecovery[thread_Id]) {
                String schedulerType = collector.getDecision(thread_Id);
                this.SwitchScheduler(schedulerType, thread_Id, mark_ID);
                this.switchContext(schedulerType);
                SOURCE_CONTROL.getInstance().waitForSchedulerSwitch(thread_Id);
            }
        }
        MeasureTools.END_SCHEDULER_SWITCH_TIME_MEASURE(thread_Id);
    }
    public void switch_scheduler(int thread_Id, long mark_ID) {
        if (scheduler instanceof RScheduler) {
            SOURCE_CONTROL.getInstance().waitForSchedulerSwitch(thread_Id);
            String schedulerType = collector.getDecision(thread_Id);
            this.SwitchScheduler(schedulerType, thread_Id, mark_ID);
            this.switchContext(schedulerType);
            SOURCE_CONTROL.getInstance().waitForSchedulerSwitch(thread_Id);
        }
    }
}
