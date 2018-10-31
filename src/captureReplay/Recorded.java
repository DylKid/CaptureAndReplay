package captureReplay;

/**
 * An interface for a class inside the game logic to implement.
 * It is neccesary for the testing suite to function
 */
public interface Recorded {

    /**
     * Implement this method to define what state is recorded on each input event
     * @return An Object that singularly represents the state of the Game
     * It is up to you what this state is and what is useful to you to record and compare
     */
    public Object getState();

    /**
     * Implement this method to begin recording, see the github readme for an example
     */
    public void setRecording();

}
