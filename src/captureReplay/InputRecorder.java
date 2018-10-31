package captureReplay;

import com.badlogic.gdx.InputProcessor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * InputRecorder class records the input and writes it to file
 */
public class InputRecorder implements InputProcessor {

    private BufferedWriter inputWriter;
    private BufferedWriter stateWriter;
    private static InputRecorder instance;
    private static long previousTime;
    private Recorded recorded;
    private static boolean timing;


    public InputRecorder(String inputFileName, String stateFileName, Recorded recorded, boolean timing) {
        try {
            inputWriter = new BufferedWriter(new FileWriter(inputFileName));
            stateWriter = new BufferedWriter(new FileWriter(stateFileName));
            this.recorded = recorded;
            this.timing = timing;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static InputRecorder getInstance(Recorded recorder, boolean timing) {
        if(instance == null){
            previousTime = System.nanoTime();
            System.out.println(previousTime);
            instance = new InputRecorder("inputRecording.txt", "stateRecording.txt",recorder, timing);
            return instance;
        }
        return instance;
    }

    @Override
    public boolean keyDown(int keycode) {
        try {
            writeObject(recorded.getState(), stateWriter);
            if(previousTime > 0){
                long waitTime = System.nanoTime() - previousTime;
                int miliseconds = Math.round(waitTime / 1000000);
                inputWriter.write("WAIT\n");
                inputWriter.write(miliseconds + "\n");
            }
            System.out.println("KEY_DOWN " + keycode);
            inputWriter.write("KEY_DOWN\n");
            inputWriter.write(keycode + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        previousTime = System.nanoTime();
        try {
            inputWriter.flush();
            stateWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        try {
            writeObject(recorded.getState(), stateWriter);
            if(previousTime > 0){
                long waitTime = System.nanoTime() - previousTime;
                int miliseconds = Math.round(waitTime / 1000000);
                inputWriter.write("WAIT\n");
                inputWriter.write(miliseconds + "\n");
            }
            System.out.println("KEY_UP " + keycode);
            inputWriter.write("KEY_UP\n");
            inputWriter.write(keycode + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        previousTime = System.nanoTime();
        try {
            inputWriter.flush();
            stateWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        System.out.format("X:%d Y:%d Pointer:%d Button:%d", screenX, screenY, pointer, button);
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            System.out.println(recorded.getState().toString());

        try {
            writeObject(recorded.getState(), stateWriter);
            if(previousTime > 0){
                long waitTime = System.nanoTime() - previousTime;
                int miliseconds = Math.round(waitTime / 1000000);
                inputWriter.write("WAIT\n");
                inputWriter.write(miliseconds + "\n");
            } else {
                sleep(100);
            }
            String touchString = String.format("X:%d Y:%d Pointer:%d Button:%d", screenX, screenY, pointer, button);
            System.out.println("TOUCH_UP" + touchString);
            inputWriter.write("TOUCH_UP\n");
            inputWriter.write(touchString + "\n");

        } catch (IOException e) {
            e.printStackTrace();
        }
        previousTime = System.nanoTime();
        try {
            inputWriter.flush();
            stateWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return true;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }

    private void writeObject(Object object, BufferedWriter writer) throws IOException {
        String writeString = "";
        writeString = object.getClass().getCanonicalName() + "\n";
        writeString = writeString + object.toString() + "\n";
        System.out.println("Writing:\n" + writeString);
        writer.write(writeString);
    }

    private static void sleep(long milis){
        try {
            Thread.sleep(milis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
