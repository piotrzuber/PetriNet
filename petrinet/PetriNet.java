package petrinet;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

public class PetriNet<T> {

    private Map<T, Integer> state;
    private Semaphore mutex;
    private LinkedList<QueueEntry<T>> threadQueue;

    public PetriNet(Map<T, Integer> initial, boolean fair) {
        this.state = initial;
        this.mutex = new Semaphore(1, fair);
        this.threadQueue = new LinkedList<>();
    }

    public Set<Map<T, Integer>> reachable(Collection<Transition<T>> transitions) {
        Set<Map<T, Integer>> markings = new HashSet<>();
        LinkedList<Map<T, Integer>> queue = new LinkedList<>();

        // execute BFS over all reachable states
        try {
            this.mutex.acquire();
            queue.add(this.state);
            this.mutex.release();
        } catch (InterruptedException ex) {
            System.out.println("Thread interrupted: " + Thread.currentThread().getName());
        }
        while (!queue.isEmpty()) {
            Map<T, Integer> temporaryState = queue.poll();
            markings.add(temporaryState);
            for (Transition<T> transition : transitions) {
                if (transition.isEnabled(temporaryState)) {
                    for (Map.Entry<T, Integer> inputArc : transition.input.entrySet()) {
                        int tokens = temporaryState.getOrDefault(inputArc.getKey(), 0);
                        tokens -= inputArc.getValue();
                        temporaryState.put(inputArc.getKey(), tokens);
                    }
                    for (T resetArc : transition.reset) {
                        temporaryState.put(resetArc, 0);
                    }
                    for (Map.Entry<T, Integer> outputArc : transition.output.entrySet()) {
                        int tokens = temporaryState.getOrDefault(outputArc.getKey(), 0);
                        tokens += outputArc.getValue();
                        temporaryState.put(outputArc.getKey(), tokens);
                    }
                }
            }
            if (!markings.contains(temporaryState)) {
                queue.add(temporaryState);
            }
        }

        for (Map<T, Integer> marking : markings) {
            for (Map.Entry<T, Integer> place : marking.entrySet()) {
                if (place.getValue() == 0) {
                    marking.remove(place.getKey());
                }
            }
        }

        return markings;
    }

    public Transition<T> fire(Collection<Transition<T>> transitions) throws InterruptedException {
        Transition<T> enabled;
        this.mutex.acquire();
        enabled = isAnyEnabled(transitions);
        if (enabled == null) {
            Semaphore waiting = new Semaphore(0);
            QueueEntry<T> queueEntry = new QueueEntry<>(waiting, transitions);
            this.threadQueue.add(queueEntry);
            this.mutex.release();
            waiting.acquire();
            this.mutex.acquire();
            enabled = isAnyEnabled(transitions);
        }
        if (enabled != null) {
            for (Map.Entry<T, Integer> inputArc : enabled.input.entrySet()) {
                int tokens = this.state.getOrDefault(inputArc.getKey(), 0);
                tokens -= inputArc.getValue();
                this.state.put(inputArc.getKey(), tokens);
            }
            for (T resetArc : enabled.reset) {
                this.state.put(resetArc, 0);
            }
            for (Map.Entry<T, Integer> outputArc : enabled.output.entrySet()) {
                int tokens = this.state.getOrDefault(outputArc.getKey(), 0);
                tokens += outputArc.getValue();
                this.state.put(outputArc.getKey(), tokens);
            }
        }

        for (QueueEntry<T> entry : this.threadQueue) {
            if (isAnyEnabled(entry.transitions) != null) {
                entry.waiting.release();
                this.threadQueue.remove(entry);
                this.mutex.release();
                break;
            }
        }

        return enabled;
    }

    private Transition<T> isAnyEnabled(Collection<Transition<T>> transitions) {
        for (Transition<T> transition : transitions) {
            if (transition.isEnabled(this.state)) {
                return transition;
            }
        }

        return null;
    }
}