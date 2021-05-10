package profiler;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import static common.meta.MetaTypes.kMaxThreadNum;
public class Metrics {
    public static int COMPUTE_COMPLEXITY = 10;//default setting. 1, 10, 100
    public static int POST_COMPUTE_COMPLEXITY = 1;
    //change to 3 for S_STORE testing.
    public static int NUM_ACCESSES = 3;//10 as default setting. 2 for short transaction, 10 for long transaction.? --> this is the setting used in YingJun's work. 16 is the default value_list used in 1000core machine.
    public static int NUM_ITEMS = 1_000_000;//1. 1_000_000; 2. ? ; 3. 1_000  //1_000_000 YCSB has 16 million records, Ledger use 200 million records.
    public static int H2_SIZE;
    private static final Metrics ourInstance = new Metrics();

    public DescriptiveStatistics[] total = new DescriptiveStatistics[kMaxThreadNum];//overhead_total time spend in txn.
    public DescriptiveStatistics[] number_of_ocs_processed = new DescriptiveStatistics[kMaxThreadNum];//overhead_total time spend in txn.
    public DescriptiveStatistics[] numberOf_transactional_events_processed = new DescriptiveStatistics[kMaxThreadNum];//overhead_total time spend in txn.

    public DescriptiveStatistics[] txn_total = new DescriptiveStatistics[kMaxThreadNum];//overhead_total time spend in txn.
    public DescriptiveStatistics[] txn_processing_total = new DescriptiveStatistics[kMaxThreadNum];//overhead_total time spend in txn.
    public DescriptiveStatistics[] state_access_total = new DescriptiveStatistics[kMaxThreadNum];//overhead_total time spend in txn.
    public DescriptiveStatistics[] barriers_total = new DescriptiveStatistics[kMaxThreadNum];//overhead_total time spend in txn.
    public DescriptiveStatistics[] calculate_levels_total = new DescriptiveStatistics[kMaxThreadNum];//overhead_total time spend in txn.
    public DescriptiveStatistics[] iterative_processing_useful_total = new DescriptiveStatistics[kMaxThreadNum];//overhead_total time spend in txn.

    public DescriptiveStatistics[] pre_txn_total = new DescriptiveStatistics[kMaxThreadNum];
    public DescriptiveStatistics[] create_oc_total = new DescriptiveStatistics[kMaxThreadNum];
    public DescriptiveStatistics[] dependency_checking_total = new DescriptiveStatistics[kMaxThreadNum];
    public DescriptiveStatistics[] dependency_outoforder_overhead_total = new DescriptiveStatistics[kMaxThreadNum];
    public DescriptiveStatistics[] db_access_time = new DescriptiveStatistics[kMaxThreadNum];

    public DescriptiveStatistics[] stream_total = new DescriptiveStatistics[kMaxThreadNum];//overhead_total time spend in txn.
    public DescriptiveStatistics[] overhead_total = new DescriptiveStatistics[kMaxThreadNum];//overhead_total time spend in txn.

    public DescriptiveStatistics[] index_ratio = new DescriptiveStatistics[kMaxThreadNum];//index
    public DescriptiveStatistics[] useful_ratio = new DescriptiveStatistics[kMaxThreadNum];//useful_work time.
    public DescriptiveStatistics[] sync_ratio = new DescriptiveStatistics[kMaxThreadNum];// sync_ratio lock_ratio and order.
    public DescriptiveStatistics[] lock_ratio = new DescriptiveStatistics[kMaxThreadNum];// sync_ratio lock_ratio and order.

    public DescriptiveStatistics[] submit_time_total = new DescriptiveStatistics[kMaxThreadNum];// sync_ratio lock_ratio and order.
    public DescriptiveStatistics[] submit_time_barrier_total = new DescriptiveStatistics[kMaxThreadNum];// sync_ratio lock_ratio and order.
    public DescriptiveStatistics[] submit_time_overhead_total = new DescriptiveStatistics[kMaxThreadNum];// sync_ratio lock_ratio and order.
    public DescriptiveStatistics[] submit_time_extra_param_1_total = new DescriptiveStatistics[kMaxThreadNum];// sync_ratio lock_ratio and order.
    public DescriptiveStatistics[] submit_time_extra_param_2_total = new DescriptiveStatistics[kMaxThreadNum];// sync_ratio lock_ratio and order.
    public DescriptiveStatistics[] submit_time_extra_param_3_total = new DescriptiveStatistics[kMaxThreadNum];// sync_ratio lock_ratio and order.

    public DescriptiveStatistics[] get_next_time_total = new DescriptiveStatistics[kMaxThreadNum];// sync_ratio lock_ratio and order.
    public DescriptiveStatistics[] get_next_overhead_time_total = new DescriptiveStatistics[kMaxThreadNum];// sync_ratio lock_ratio and order.
    public DescriptiveStatistics[] get_next_barrier_time_total = new DescriptiveStatistics[kMaxThreadNum];// sync_ratio lock_ratio and order.
    public DescriptiveStatistics[] get_next_thread_wait_time_total = new DescriptiveStatistics[kMaxThreadNum];// sync_ratio lock_ratio and order.
    public DescriptiveStatistics[] get_next_extra_param_1_total = new DescriptiveStatistics[kMaxThreadNum];// sync_ratio lock_ratio and order.
    public DescriptiveStatistics[] get_next_extra_param_2_total = new DescriptiveStatistics[kMaxThreadNum];// sync_ratio lock_ratio and order.
    public DescriptiveStatistics[] get_next_extra_param_3_total = new DescriptiveStatistics[kMaxThreadNum];// sync_ratio lock_ratio and order.

    // Op id, descriptive
//    public Map<String, DescriptiveStatistics> useful_ratio = new HashMap<>();//useful_work time.
//    public Map<String, DescriptiveStatistics> abort_ratio = new HashMap<>();//abort
//    public Map<String, DescriptiveStatistics> ts_allocation = new HashMap<>();//timestamp allocation
//    public Map<String, DescriptiveStatistics> index_ratio = new HashMap<>();//index
//    public Map<String, DescriptiveStatistics> sync_ratio = new HashMap<>();// sync_ratio lock_ratio and order.
//    public Map<String, DescriptiveStatistics> exe_ratio = new HashMap<>();//not in use.
//    public Map<String, DescriptiveStatistics> order_wait = new HashMap<>();//order sync_ratio
//    public Map<String, DescriptiveStatistics> enqueue_time = new HashMap<>();//event enqueue
    //TODO: single op for now. per task/thread.
//    public DescriptiveStatistics[] ts_allocation = new DescriptiveStatistics[kMaxThreadNum];//timestamp allocation
    public DescriptiveStatistics[] average_tp_core = new DescriptiveStatistics[kMaxThreadNum];// average tp processing time per thread without considering synchronization.
    public DescriptiveStatistics[] average_tp_submit = new DescriptiveStatistics[kMaxThreadNum];// average tp processing time per thread without considering synchronization.
    public DescriptiveStatistics[] average_tp_w_syn = new DescriptiveStatistics[kMaxThreadNum];// average tp processing time per thread with synchronization.
    public DescriptiveStatistics[] average_txn_construct = new DescriptiveStatistics[kMaxThreadNum];
//    public Map<Integer, DescriptiveStatistics> exe_ratio = new HashMap<>();//not in use.
    /**
     * Specially for T-Stream..
     */
    public DescriptiveStatistics[] enqueue_time = new DescriptiveStatistics[kMaxThreadNum];//event enqueue
    private Metrics() {
    }
    public static Metrics getInstance() {
        return ourInstance;
    }
    /**
     * Initilize all metric counters.
     */
    public void initilize(String ID, int num_access) {
//        exe_ratio.put(ID, new DescriptiveStatistics());
//        useful_ratio.put(ID, new DescriptiveStatistics());
//        abort_ratio.put(ID, new DescriptiveStatistics());
//        index_ratio.put(ID, new DescriptiveStatistics());
//        sync_ratio.put(ID, new DescriptiveStatistics());
//        ts_allocation.put(ID, new DescriptiveStatistics());
//        enqueue_time.put(ID, new DescriptiveStatistics());
        NUM_ACCESSES = num_access;
    }
    public void initilize(int task) {

        total[task] = new DescriptiveStatistics();
        number_of_ocs_processed[task] = new DescriptiveStatistics();
        numberOf_transactional_events_processed[task] = new DescriptiveStatistics();

        txn_total[task] = new DescriptiveStatistics();
        txn_processing_total[task] = new DescriptiveStatistics();
        state_access_total[task] = new DescriptiveStatistics();
        calculate_levels_total[task] = new DescriptiveStatistics();
        iterative_processing_useful_total[task] = new DescriptiveStatistics();

        pre_txn_total[task] = new DescriptiveStatistics();
        create_oc_total[task] = new DescriptiveStatistics();
        dependency_checking_total[task] = new DescriptiveStatistics();
        barriers_total[task] = new DescriptiveStatistics();

        dependency_outoforder_overhead_total[task] = new DescriptiveStatistics();
        db_access_time[task] = new DescriptiveStatistics();

        stream_total[task] = new DescriptiveStatistics();
        overhead_total[task] = new DescriptiveStatistics();
        useful_ratio[task] = new DescriptiveStatistics();
        index_ratio[task] = new DescriptiveStatistics();
        sync_ratio[task] = new DescriptiveStatistics();
        lock_ratio[task] = new DescriptiveStatistics();
        average_tp_core[task] = new DescriptiveStatistics();
        average_txn_construct[task] = new DescriptiveStatistics();
        average_tp_submit[task] = new DescriptiveStatistics();
        average_tp_w_syn[task] = new DescriptiveStatistics();

        submit_time_total[task] = new DescriptiveStatistics();
        submit_time_barrier_total[task] = new DescriptiveStatistics();
        submit_time_overhead_total[task] = new DescriptiveStatistics();
        submit_time_extra_param_1_total[task] = new DescriptiveStatistics();
        submit_time_extra_param_2_total[task] = new DescriptiveStatistics();
        submit_time_extra_param_3_total[task] = new DescriptiveStatistics();

        get_next_time_total[task] = new DescriptiveStatistics();
        get_next_overhead_time_total[task] = new DescriptiveStatistics();
        get_next_barrier_time_total[task] = new DescriptiveStatistics();
        get_next_thread_wait_time_total[task] = new DescriptiveStatistics();
        get_next_extra_param_1_total[task] = new DescriptiveStatistics();
        get_next_extra_param_2_total[task] = new DescriptiveStatistics();
        get_next_extra_param_3_total[task] = new DescriptiveStatistics();

    }
}
