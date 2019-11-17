package petrinet;

import java.util.Collection;
import java.util.concurrent.Semaphore;

public class QueueEntry<T> {
    Semaphore waiting;
    Collection<Transition<T>> transitions;

    public QueueEntry(Semaphore waiting, Collection<Transition<T>> transitions) {
        this.waiting = waiting;
        this.transitions = transitions;
    }
}
