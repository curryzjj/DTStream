package execution;

import common.collections.Configuration;
import components.context.TopologyContext;
import components.exception.UnhandledCaseException;
import controller.affinity.AffinityController;
import db.Database;
import durability.ftmanager.FTManager;
import durability.ftmanager.ImplFTManager.*;
import durability.logging.LoggingStrategy.ImplLoggingManager.DependencyLoggingManager;
import execution.runtime.boltThread;
import execution.runtime.executorThread;
import execution.runtime.spoutThread;
import optimization.OptimizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import transaction.TxnManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static common.CONTROL.enable_log;
import static common.CONTROL.enable_shared_state;
import static common.Constants.*;

/**
 * Created by shuhaozhang on 19/8/16.
 */
public class ExecutionManager {
    private final static Logger LOG = LoggerFactory.getLogger(ExecutionManager.class);
    public final HashMap<Integer, executorThread> ThreadMap = new HashMap<>();
    public final AffinityController AC;
    private final OptimizationManager optimizationManager;
    private final ExecutionGraph g;
    private final FTManager ftManager;
    private final FTManager loggingManager;

    public ExecutionManager(ExecutionGraph g, Configuration conf, OptimizationManager optimizationManager) {
        this.g = g;
        AC = new AffinityController(conf);
        this.optimizationManager = optimizationManager;
        try{
            switch (conf.getInt("FTOption")) {
                case 1:
                    this.ftManager = new CheckpointManager();
                    this.loggingManager = null;
                    this.ftManager.initialize(conf);
                    break;
                case 2:
                    this.ftManager = new CheckpointManager();
                    this.loggingManager = new WalManager();
                    this.ftManager.initialize(conf);
                    this.loggingManager.initialize(conf);
                    break;
                case 3:
                    this.ftManager = new CheckpointManager();
                    this.loggingManager = new PathManager();
                    this.ftManager.initialize(conf);
                    this.loggingManager.initialize(conf);
                    break;
                case 4:
                    this.ftManager = new CheckpointManager();
                    this.loggingManager = new LSNVectorManager();
                    this.ftManager.initialize(conf);
                    this.loggingManager.initialize(conf);
                    break;
                case 5:
                    this.ftManager = new CheckpointManager();
                    this.loggingManager = new DependencyManager();
                    this.ftManager.initialize(conf);
                    this.loggingManager.initialize(conf);
                    break;
                case 6:
                    this.ftManager = new CheckpointManager();
                    this.loggingManager = new CommandManager();
                    this.ftManager.initialize(conf);
                    this.loggingManager.initialize(conf);
                    break;
                default:
                    this.ftManager = null;
                    this.loggingManager = null;
                    break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadFTManger() {
        if (this.ftManager != null) {
            this.ftManager.start();
        }
        if (this.loggingManager != null) {
            this.loggingManager.start();
        }
    }


    /**
     * Launch threads for each executor in executionGraph
     * We make sure no interference among threads --> one thread one core.
     * TODO: let's think about how to due with multi-thread per core in future..
     * All executors have to sync_ratio for OM to start, so it's safe to do initialization here. E.g., initialize database.
     */
    public void distributeTasks(Configuration conf,
                                CountDownLatch latch, Database db) throws UnhandledCaseException {
        g.build_inputScheduler();
        this.loadFTManger();
        //TODO: support multi-stages later.
        if (enable_shared_state) {
            HashMap<Integer, List<Integer>> stage_map = new HashMap<>();//Stages --> Executors.
            for (ExecutionNode e : g.getExecutionNodeArrayList()) {
                stage_map.putIfAbsent(e.op.getStage(), new LinkedList<>());
                stage_map.get(e.op.getStage()).add(e.getExecutorID());
            }
            int stage = 0;//currently only stage 0 is required..
            List<Integer> integers = stage_map.get(stage);
            if (integers != null) {
                int totalThread = conf.getInt("tthread");
                int numberOfStates = conf.getInt("NUM_ITEMS");
                String schedulerType = conf.getString("scheduler");
                int app = conf.getInt("app");
                TxnManager.loggingManager = db.getLoggingManager();
                if (conf.getBoolean("isDynamic")) {
                    String schedulers=conf.getString("schedulersPool");
                    TxnManager.initSchedulerPool(conf.getString("defaultScheduler"), schedulers, totalThread, numberOfStates, app, conf.getInt("maxThreads"));
                    //Configure the bottom line for triggering scheduler switching in Collector(include the isRuntime and when to switch)
                    TxnManager.setBottomLine(conf.getString("bottomLine"));
                    if (!conf.getBoolean("isRuntime")) {
                        TxnManager.setWorkloadConfig(conf.getString("WorkloadConfig"));
                    }
                } else if (conf.getBoolean("isGroup")) {
                    TxnManager.CreateSchedulerByGroup(conf.getString("SchedulersForGroup"), totalThread, numberOfStates, app);
                } else {
                    TxnManager.CreateScheduler(schedulerType, totalThread, numberOfStates, app);
                }
                if (conf.getBoolean("isRecovery")) {
                    TxnManager.initRecoveryScheduler(conf.getInt("FTOption"),totalThread, numberOfStates, app);
                }
            }
        }
        executorThread thread = null;
        long start = System.currentTimeMillis();
        for (ExecutionNode e : g.getExecutionNodeArrayList()) {
            switch (e.operator.type) {
                case spoutType:
                    thread = launchSpout_SingleCore(e, new TopologyContext(g, db, ftManager, loggingManager, e, ThreadMap)
                            , conf, 0, latch); //TODO: schedule to numa node wisely.
                    break;
                case boltType:
                case sinkType:
                    thread = launchBolt_SingleCore(e, new TopologyContext(g, db, ftManager, loggingManager, e, ThreadMap)
                            , conf, 0, latch); //TODO: schedule to numa node wisely.
                    break;
                case virtualType:
                    if (enable_log) LOG.info("Won't launch virtual ground");
                    break;
                default:
                    throw new UnhandledCaseException("type not recognized");
            }
            if (!(conf.getBoolean("monte", false) || conf.getBoolean("simulation", false))) {
                assert thread != null;
                while (!thread.isReady()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        long end = System.currentTimeMillis();
        if (enable_log) LOG.info("It takes :" + (end - start) / 1000 + " seconds to finish launch the operators.");
    }

    private executorThread launchSpout_InCore(ExecutionNode e, TopologyContext context, Configuration conf,
                                              int node, long[] cores, CountDownLatch latch) {

        spoutThread st;
        st = new spoutThread(e, context, conf, cores, node, latch,
                ThreadMap);
        st.setDaemon(true);
        if (!(conf.getBoolean("monte", false) || conf.getBoolean("simulation", false))) {
            st.start();
        }
        ThreadMap.putIfAbsent(e.getExecutorID(), st);
        return st;
    }

    private executorThread launchBolt_InCore(ExecutionNode e, TopologyContext context, Configuration conf,
                                             int node, long[] cores, CountDownLatch latch) {
        boltThread wt;
        wt = new boltThread(e, context, conf, cores, node, latch,
                optimizationManager, ThreadMap);
        wt.setDaemon(true);
        if (!(conf.getBoolean("monte", false) || conf.getBoolean("simulation", false))) {
            wt.start();
        }
        ThreadMap.putIfAbsent(e.getExecutorID(), wt);
        return wt;
    }

    private executorThread launchSpout_SingleCore(ExecutionNode e, TopologyContext context, Configuration conf,
                                                  int node, CountDownLatch latch) {
        if (enable_log) LOG.info("Launch" + e.getOP() + " on node:" + node);
        long[] cpu;
        if (!conf.getBoolean("NAV", true)) {
            cpu = AC.requirePerCore(node);
        } else {
            cpu = new long[1];
        }

        return launchSpout_InCore(e, context, conf, node, cpu, latch);
    }

    private executorThread launchBolt_SingleCore(ExecutionNode e, TopologyContext context, Configuration conf,
                                                 int node, CountDownLatch latch) {
        if (enable_log) LOG.info("Launch bolt:" + e.getOP() + " on node:" + node);
        long[] cpu;
        if (!conf.getBoolean("NAV", true)) {
            cpu = AC.requirePerCore(node);
        } else {
            cpu = new long[1];
        }
        return launchBolt_InCore(e, context, conf, node, cpu, latch);
    }

    /**
     * stop EM
     * It stops all execution threads as well.
     */
    public void exist() {
        if (enable_log) LOG.info("Execution stops.");
        this.getSinkThread().getContext().Sequential_stopAll();
        this.closeFTM();
    }

    public void closeFTM() {
        if (this.ftManager != null) {
            this.ftManager.running = false;
            ftManager.interrupt();
        }
        if (this.loggingManager != null) {
            this.loggingManager.running = false;
            loggingManager.interrupt();
        }
    }

    public executorThread getSinkThread() {
        return ThreadMap.get(g.getSinkThread());
    }
}
