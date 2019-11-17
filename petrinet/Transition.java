package petrinet;

import java.util.Collection;
import java.util.Map;

public class Transition<T> {

    Map<T, Integer> input;                  // (place, weight)
    Collection<T> reset;
    private Collection<T> inhibitor;
    Map<T, Integer> output;                 // (place, weight)

    public Transition(Map<T, Integer> input, Collection<T> reset, Collection<T> inhibitor, Map<T, Integer> output) {
        this.input = input;
        this.reset = reset;
        this.inhibitor = inhibitor;
        this.output = output;
    }

    boolean isEnabled(Map<T, Integer> state) {
        for (Map.Entry<T, Integer> inputArc : this.input.entrySet()) {
            int tokens = state.getOrDefault(inputArc.getKey(), 0);
            tokens -= inputArc.getValue();
            if (tokens < 0) {
                return false;
            }
        }

        for (T inhibitorArc : this.inhibitor) {
            int tokens = state.getOrDefault(inhibitorArc, 0);
            if (tokens != 0) {
                return false;
            }
        }

        return true;
    }
}
