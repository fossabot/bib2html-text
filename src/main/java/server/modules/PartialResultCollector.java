package server.modules;

import global.model.Result;
import server.events.*;
import server.events.EventListener;
import global.model.PartialResult;

import java.util.*;

/**
 * @author Maximilian Schirm (denkbares GmbH)
 * @created 05.12.2016
 * <p>
 * This is a Singleton class with immediate instantiation
 */
public class PartialResultCollector implements EventListener {

    private static final PartialResultCollector INSTANCE = new PartialResultCollector();
    private HashMap<String, Collection<PartialResult>> mappingClientIDtoFinishedPartialResults;
    private HashMap<String, Integer> mappingClientIDtoExpectedResultsSize;

    private PartialResultCollector() {
        EventManager.getInstance().registerListener(this);
        mappingClientIDtoExpectedResultsSize = new HashMap<>();
        mappingClientIDtoFinishedPartialResults = new HashMap<>();

        //Starts the update loop
        TimerTask updateLoop = new TimerTask() {
            @Override
            public void run() {
                update();
                System.out.println("LOG: Partial Result Collector - Update task did another run.");
            }
        };
        Timer timer = new Timer();
        timer.schedule(updateLoop, 0, 1000);
    }

    public static PartialResultCollector getInstance() {
        return INSTANCE;
    }

    @Override
    public void notify(Event toNotify) {
        if (toNotify instanceof ReceivedPartialResultEvent) {
            ReceivedPartialResultEvent tempEvent = (ReceivedPartialResultEvent) toNotify;
            PartialResult partialResult = tempEvent.getPartialResult();
            String id = partialResult.getIdentifier().getIdentificationSequence();
            Collection<PartialResult> presentResults = mappingClientIDtoFinishedPartialResults.get(id);
            presentResults.add(partialResult);
            mappingClientIDtoFinishedPartialResults.put(id, presentResults);

        } else if (toNotify instanceof ReceivedErrorEvent) {
            ReceivedErrorEvent tempEvent = (ReceivedErrorEvent) toNotify;
            String id = tempEvent.getResultID();
            int currentSize = mappingClientIDtoExpectedResultsSize.get(id);
            mappingClientIDtoExpectedResultsSize.put(id, currentSize - 1);
        } else if (toNotify instanceof RequestAcceptedEvent) {
            RequestAcceptedEvent tempEvent = (RequestAcceptedEvent) toNotify;
            String id = tempEvent.getRequestID();
            int size = tempEvent.getReqSize();
            mappingClientIDtoExpectedResultsSize.put(id, size);
            mappingClientIDtoFinishedPartialResults.remove(id);
        }
    }

    /**
     * Checks whether any of the results is finished creating yet and - if that is the case - sends out a FinishedCollectingResultEvent.
     */
    private synchronized void update() {
        mappingClientIDtoExpectedResultsSize.forEach((key, size) -> {
            Collection<PartialResult> parts = mappingClientIDtoFinishedPartialResults.get(key);
            if (parts.size() == size)
                EventManager.getInstance().publishEvent(new FinishedCollectingResultEvent(Result.fromPartials(parts)));
        });
    }

    @Override
    public Set<Class<? extends Event>> getEvents() {
        Set<Class<? extends Event>> evts = new HashSet<>();
        evts.add(ReceivedErrorEvent.class);
        evts.add(ReceivedPartialResultEvent.class);
        evts.add(RequestAcceptedEvent.class);
        return evts;
    }
}
