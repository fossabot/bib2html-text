package server.modules;

import global.model.DefaultResult;
import global.model.IPartialResult;
import server.events.*;
import server.events.IEventListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Maximilian Schirm
 *         created 05.12.2016
 *         <p>
 *         This is a Singleton class with immediate instantiation
 */
public class PartialResultCollector implements IEventListener {

    private static final PartialResultCollector INSTANCE = new PartialResultCollector();
    private final ConcurrentMap<String, Collection<IPartialResult>> mappingClientIDtoFinishedPartialResults;
    private final ConcurrentMap<String, Integer> mappingClientIDtoExpectedResultsSize;

    private PartialResultCollector() {
        EventManager.getInstance().registerListener(this);
        mappingClientIDtoFinishedPartialResults = new ConcurrentHashMap<>();
        mappingClientIDtoExpectedResultsSize = new ConcurrentHashMap<>();

        //Starts the update loop
        TimerTask updateLoop = new TimerTask() {
            @Override
            public void run() {
                update();
//                Log.log("Partial Result Collector - Update task did another run.", LogLevel.LOW);
            }
        };
        Timer timer = new Timer();
        timer.schedule(updateLoop, 0, 2000);
    }

    public Collection<String> getOutstandingRequests() {
        Collection<String> requests = new ArrayList<>();
        for (String clientID : mappingClientIDtoExpectedResultsSize.keySet()) {
            int expectedCount = mappingClientIDtoExpectedResultsSize.get(clientID);
            Collection<IPartialResult> finished = mappingClientIDtoFinishedPartialResults.get(clientID);
            int finishedCount = (finished == null) ? 0 : finished.size();
            requests.add(clientID + " : Expected " + expectedCount + " , Finished " + finishedCount + ", Difference : " + (expectedCount - finishedCount) + ".");
        }
        return requests;
    }

    protected static PartialResultCollector getInstance() {
        return INSTANCE;
    }

    @Override
    public void notify(IEvent toNotify) {
        if (toNotify instanceof ReceivedPartialResultEvent) {
            IPartialResult partialResult = ((ReceivedPartialResultEvent) toNotify).getPartialResult();
            String id = partialResult.getIdentifier().getClientID();
            Collection<IPartialResult> presentResults = mappingClientIDtoFinishedPartialResults.get(id);
            if (presentResults == null) {
                presentResults = new HashSet<>();
            }
            presentResults.add(partialResult);
            mappingClientIDtoFinishedPartialResults.put(id, presentResults);
        } else if (toNotify instanceof ReceivedErrorEvent) {
            ReceivedErrorEvent tempEvent = (ReceivedErrorEvent) toNotify;
            String id = tempEvent.getResultID();
            int currentSize = mappingClientIDtoExpectedResultsSize.get(id);
            mappingClientIDtoExpectedResultsSize.put(id, currentSize - 1);
        } else if (toNotify instanceof RequestAcceptedEvent) {
            String id = ((RequestAcceptedEvent) toNotify).getRequestID();
            int size = ((RequestAcceptedEvent) toNotify).getReqSize();
            mappingClientIDtoExpectedResultsSize.put(id, size);
            mappingClientIDtoFinishedPartialResults.remove(id);
        }
    }

    /**
     * Checks whether any of the results is finished creating yet and - if that is the case - sends out a FinishedCollectingResultEvent.
     */
    private synchronized void update() {
        mappingClientIDtoExpectedResultsSize.forEach((key, size) -> {
            Collection<IPartialResult> parts = mappingClientIDtoFinishedPartialResults.get(key);
            if (parts != null) {
                EventManager.getInstance().publishEvent(new ProgressUpdateEvent(key, getProgressForClientID(key)));
                if (parts.size() == size) {
                    EventManager.getInstance().publishEvent(new FinishedCollectingResultEvent(DefaultResult.buildResultfromPartials(parts)));
                    mappingClientIDtoFinishedPartialResults.remove(key);
                    mappingClientIDtoExpectedResultsSize.remove(key);
                }
            }
        });
    }

    @Override
    public Set<Class<? extends IEvent>> getEvents() {
        Set<Class<? extends IEvent>> evts = new HashSet<>();
        evts.add(ReceivedErrorEvent.class);
        evts.add(ReceivedPartialResultEvent.class);
        evts.add(RequestAcceptedEvent.class);
        return evts;
    }

    private synchronized double getProgressForClientID(String clientID){
        double toFinish = mappingClientIDtoExpectedResultsSize.get(clientID);
        double finished = mappingClientIDtoFinishedPartialResults.get(clientID).size();
        return finished/toFinish;
    }
}
