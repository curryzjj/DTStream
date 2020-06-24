package application.sink;

import application.sink.helper.stable_sink_helper;
import application.util.datatypes.StreamValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sesame.components.operators.api.BaseSink;
import sesame.execution.ExecutionGraph;
import sesame.execution.runtime.tuple.JumboTuple;
import sesame.execution.runtime.tuple.impl.Tuple;

/**
 * @author mayconbordin
 */
public class ForwardSink extends BaseSink {
    private static final Logger LOG = LoggerFactory.getLogger(ForwardSink.class);
    private static final long serialVersionUID = -8569894070135181479L;

    public ForwardSink() {
        super(LOG);
    }

    @Override
    public void initialize(int thread_Id, int thisTaskId, ExecutionGraph graph) {
        super.initialize(thread_Id, thisTaskId, graph);
        stable_sink_helper helper = new stable_sink_helper(LOG
                , config.getInt("runtimeInSeconds")
                , config.getString("metrics.output"), config.getDouble("predict", 0), 0, thread_Id, false);
    }

    @Override
    public void execute(Tuple in) throws InterruptedException {
//not in use
    }

    @Override
    public void execute(JumboTuple in) throws InterruptedException {
        int bound = in.length;
        final long bid = in.getBID();
        for (int i = 0; i < bound; i++) {
            collector.emit(bid, new StreamValues(in.getMsg(i)));
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
