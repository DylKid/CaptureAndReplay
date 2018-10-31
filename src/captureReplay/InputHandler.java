package captureReplay;

/**
 * An interface for manually triggering events, this is a convieniance interface only if
 * the game being used cannot deal with actual input.
 */
public interface InputHandler {
    public boolean touchUp(int x, int y, int pointer, int button);
    public boolean keyDown();
    public boolean keyUp();
}
