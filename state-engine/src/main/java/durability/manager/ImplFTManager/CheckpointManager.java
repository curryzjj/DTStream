package durability.manager.ImplFTManager;

import common.collections.Configuration;
import common.collections.OsUtils;
import common.io.LocalFS.LocalDataOutputStream;
import common.tools.Serialize;
import durability.manager.FTManager;
import durability.snapshot.SnapshotResult.SnapshotCommitInformation;
import durability.snapshot.SnapshotResult.SnapshotResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static common.CONTROL.enable_log;
import static utils.FaultToleranceConstants.*;

public class CheckpointManager extends FTManager {
    private final Logger LOG = LoggerFactory.getLogger(CheckpointManager.class);
    private int parallelNum;
    private ConcurrentHashMap<Long, List<FaultToleranceStatus>> callCommit;
    //<snapshotId, SnapshotCommitInformation>
    private ConcurrentHashMap<Long, SnapshotCommitInformation> registerSnapshot = new ConcurrentHashMap<>();
    private String metaPath;
    private Queue<Long> uncommittedId = new ConcurrentLinkedQueue<>();
    private long pendingSnapshotId;
    @Override
    public void initialize(Configuration config) {
        this.parallelNum = config.getInt("parallelNum");
        metaPath = config.getString("rootFilePath") + OsUtils.OS_wrapper("snapshot") + OsUtils.OS_wrapper("MetaData");
        File file = new File(this.metaPath);
        if (file.exists()) {
            file.delete();
            file.mkdirs();
            if (enable_log) LOG.info("CheckpointManager initialize successfully");
        }
        this.setName("CheckpointManager");
    }

    @Override
    public boolean spoutRegister(long snapshotId) {
        if (this.registerSnapshot.containsKey(snapshotId)) {
            //TODO: if these are too many uncommitted snapshot, notify the spout not to register
            LOG.info("SnapshotID has been registered already");
            return false;
        } else {
            this.registerSnapshot.put(snapshotId, new SnapshotCommitInformation(snapshotId));
            this.uncommittedId.add(snapshotId);
            callCommit.put(snapshotId, initCallCommit());
            return true;
        }
    }

    @Override
    public boolean sinkRegister(long snapshot) {
        return false;
    }

    @Override
    public boolean boltRegister(int partitionId, FaultToleranceStatus status, SnapshotResult snapshotResult) {
        this.registerSnapshot.get(snapshotResult.snapshotId).snapshotResults.add(snapshotResult);
        this.callCommit.get(snapshotResult.snapshotId).set(partitionId, FaultToleranceStatus.Snapshot);
        return true;
    }

    @Override
    public void Listener() throws IOException {
        while (running) {
            if (all_register()) {
                if (callCommit.containsKey(FaultToleranceStatus.Snapshot)) {
                    snapshotComplete(pendingSnapshotId);
                    LOG.info("CheckpointManager received all register and commit snapshot");
                    if (uncommittedId.size() != 0) {
                        this.pendingSnapshotId = uncommittedId.poll();
                    } else {
                        this.pendingSnapshotId = 0;
                    }
                    LOG.info("The number of uncommitted snapshot");
                } else {
                    //TODO: add other operation, such as recovery
                }
            }
        }
    }

    @Override
    public void run() {
        LOG.info("CheckpointManager starts!");
        try {
            Listener();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            File file = new File(this.metaPath);
            if (file.exists()) {
                file.delete();
            }
            LOG.info("CheckpointManager stops");
        }
    }

    private boolean all_register() {
        if (pendingSnapshotId == 0) {
            if (uncommittedId.size() != 0) {
                pendingSnapshotId = uncommittedId.poll();
                return !this.callCommit.get(pendingSnapshotId).contains(FaultToleranceStatus.NULL);
            } else {
                return false;
            }
        } else {
            return !this.callCommit.get(pendingSnapshotId).contains(FaultToleranceStatus.NULL);
        }
    }

    private List<FaultToleranceStatus> initCallCommit() {
        List<FaultToleranceStatus> statuses = new Vector<>();
        for (int i = 0; i < parallelNum; i++) {
            statuses.add(FaultToleranceStatus.NULL);
        }
        return statuses;
    }

    private void snapshotComplete(long snapshotId) throws IOException {
        SnapshotCommitInformation snapshotCommitInformation = this.registerSnapshot.get(snapshotId);
        LocalDataOutputStream localDataOutputStream = new LocalDataOutputStream(new File(this.metaPath));
        DataOutputStream dataOutputStream = new DataOutputStream(localDataOutputStream);
        byte[] result = Serialize.serializeObject(snapshotCommitInformation);
        int length = result.length;
        dataOutputStream.writeInt(length);
        dataOutputStream.write(result);
        dataOutputStream.close();
        this.registerSnapshot.remove(snapshotId);
        LOG.info("CheckpointManager commit the checkpoint to the current.log");
    }
}
