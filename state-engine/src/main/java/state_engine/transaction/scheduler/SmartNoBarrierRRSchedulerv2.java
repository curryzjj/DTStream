package state_engine.transaction.scheduler;

import state_engine.common.OperationChain;
import state_engine.profiler.MeasureTools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class SmartNoBarrierRRSchedulerv2 implements IScheduler, OperationChain.IOnDependencyResolvedListener {

    private ArrayList<OperationChain> leftOvers;
    private ArrayList<OperationChain> withDependents;

    private AtomicInteger totalSubmitted;
    private AtomicInteger totalProcessed;

    public SmartNoBarrierRRSchedulerv2(int tp) {
        leftOvers = new ArrayList<>();
        withDependents = new ArrayList<>();

        totalSubmitted = new AtomicInteger(0);
        totalProcessed = new AtomicInteger(0);
    }

    @Override
    public void submitOcs(int threadId, Collection<OperationChain> ocs) {
        for (OperationChain oc : ocs) {
            oc.updateDependencyLevel();
            int dLevel = oc.getDependencyLevel();
            if(dLevel==0) {
                if(oc.hasDependents())
                    withDependents.add(oc);
                else
                    leftOvers.add(oc);
            }else {
                oc.setOnOperationChainChangeListener(this);
            }
        }
        totalSubmitted.addAndGet(ocs.size());
    }

    @Override
    public void onDependencyResolvedListener(OperationChain oc) {
        if(oc.hasDependents())
            withDependents.add(oc);
        else
            leftOvers.add(oc);
    }

    @Override
    public OperationChain next(int threadId) {
        OperationChain oc = getOcForThreadAndDLevel(threadId);
        MeasureTools.BEGIN_GET_NEXT_OVERHEAD_TIME_MEASURE(threadId);
        while(oc==null) {
            if(areAllOCsScheduled(threadId))
                break;
            oc = getOcForThreadAndDLevel(threadId);
        }
        MeasureTools.END_GET_NEXT_OVERHEAD_TIME_MEASURE(threadId);
        if(oc!=null)
            totalProcessed.incrementAndGet();
        return oc;
    }

    protected OperationChain getOcForThreadAndDLevel(int threadId) {
//        OperationChain oc = withDependents.poll();
//        if(oc==null)
//            oc = leftOvers.poll();
        return null;
    }

    @Override
    public boolean areAllOCsScheduled(int threadId) {
        return totalProcessed.get()==totalSubmitted.get();
    }

    @Override
    public void reSchedule(int threadId, OperationChain oc) {

    }

    @Override
    public boolean isReSchedulingEnabled() {
        return false;
    }


    @Override
    public void reset() {
        leftOvers = new ArrayList<>();
        withDependents = new ArrayList<>();
        totalSubmitted = new AtomicInteger(0);
        totalProcessed = new AtomicInteger(0);
    }

}
