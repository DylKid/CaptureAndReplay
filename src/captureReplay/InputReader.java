package captureReplay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Pixmap;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * InputReader class, reads the input from inputRecording.txt and
 * writes the state to testStateRecording.txt
 */
public class InputReader implements Runnable, Input {

    private float[] accel = new float[3];
    private float[] gyrate = new float[3];
    private float[] compass = new float[3];
    private boolean multiTouch = false;

    int keyCount = 0;
    boolean[] keys = new boolean[256];
    boolean keyJustPressed = false;
    boolean[] justPressedKeys = new boolean[256];
    int[] deltaX = new int[20];
    int[] deltaY = new int[20];
    int[] touchX = new int[20];
    int[] touchY = new int[20];
    boolean isTouched[] = new boolean[20];
    boolean justTouched = false;
    InputProcessor processor = null;

    Recorded recorded;

    InputHandler inputHandler;

    public InputReader(Recorded recorded){
        this.recorded = recorded;
    }


    public void setInputHandler(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
    }

    class KeyEvent {
        static final int KEY_DOWN = 0;
        static final int KEY_UP = 1;
        static final int KEY_TYPED = 2;

        long timeStamp;
        int type;
        int keyCode;
        char keyChar;

        @Override
        public boolean equals(Object o){
            if(o instanceof KeyEvent){
                if(keyCode == ((KeyEvent) o).keyCode && keyChar == ((KeyEvent) o).keyChar){
                    return true;
                }
            }
            return false;
        }
    }

    class TouchEvent {
        static final int TOUCH_DOWN = 0;
        static final int TOUCH_UP = 1;
        static final int TOUCH_DRAGGED = 2;

        long timeStamp;
        int type;
        int x;
        int y;
        int pointer;
    }

    class EventTrigger implements Runnable {
        TouchEvent touchEvent;
        KeyEvent keyEvent;

        public EventTrigger (TouchEvent touchEvent, KeyEvent keyEvent) {
            this.touchEvent = touchEvent;
            this.keyEvent = keyEvent;
        }

        @Override
        public void run () {
            justTouched = false;
            if (keyJustPressed) {
                keyJustPressed = false;
                for (int i = 0; i < justPressedKeys.length; i++) {
                    justPressedKeys[i] = false;
                }
            }

            if (processor != null) {
                if (touchEvent != null) {
                    switch (touchEvent.type) {
                        case TouchEvent.TOUCH_DOWN:
                            deltaX[touchEvent.pointer] = 0;
                            deltaY[touchEvent.pointer] = 0;
                            processor.touchDown(touchEvent.x, touchEvent.y, touchEvent.pointer, Input.Buttons.LEFT);
                            isTouched[touchEvent.pointer] = true;
                            justTouched = true;
                            break;
                        case TouchEvent.TOUCH_UP:
                            deltaX[touchEvent.pointer] = 0;
                            deltaY[touchEvent.pointer] = 0;
                            processor.touchUp(touchEvent.x, touchEvent.y, touchEvent.pointer, Input.Buttons.LEFT);
                            isTouched[touchEvent.pointer] = false;
                            break;
                        case TouchEvent.TOUCH_DRAGGED:
                            deltaX[touchEvent.pointer] = touchEvent.x - touchX[touchEvent.pointer];
                            deltaY[touchEvent.pointer] = touchEvent.y - touchY[touchEvent.pointer];
                            processor.touchDragged(touchEvent.x, touchEvent.y, touchEvent.pointer);
                            break;
                    }
                    touchX[touchEvent.pointer] = touchEvent.x;
                    touchY[touchEvent.pointer] = touchEvent.y;
                }
                if (keyEvent != null) {
                    switch (keyEvent.type) {
                        case KeyEvent.KEY_DOWN:
                            processor.keyDown(keyEvent.keyCode);
                            if (!keys[keyEvent.keyCode]) {
                                keyCount++;
                                keys[keyEvent.keyCode] = true;
                            }
                            keyJustPressed = true;
                            justPressedKeys[keyEvent.keyCode] = true;
                            break;
                        case KeyEvent.KEY_UP:
                            processor.keyUp(keyEvent.keyCode);
                            if (keys[keyEvent.keyCode]) {
                                keyCount--;
                                keys[keyEvent.keyCode] = false;
                            }
                            break;
                        case KeyEvent.KEY_TYPED:
                            processor.keyTyped(keyEvent.keyChar);
                            break;
                    }
                }
            } else {
                if (touchEvent != null) {
                    switch(touchEvent.type) {
                        case TouchEvent.TOUCH_DOWN:
                            deltaX[touchEvent.pointer] = 0;
                            deltaY[touchEvent.pointer] = 0;
                            isTouched[touchEvent.pointer] = true;
                            justTouched = true;
                            break;
                        case TouchEvent.TOUCH_UP:
                            deltaX[touchEvent.pointer] = 0;
                            deltaY[touchEvent.pointer] = 0;
                            isTouched[touchEvent.pointer] = false;
                            break;
                        case TouchEvent.TOUCH_DRAGGED:
                            deltaX[touchEvent.pointer] = touchEvent.x - touchX[touchEvent.pointer];
                            deltaY[touchEvent.pointer] = touchEvent.y - touchY[touchEvent.pointer];
                            break;
                    }
                    touchX[touchEvent.pointer] = touchEvent.x;
                    touchY[touchEvent.pointer] = touchEvent.y;
                }
                if (keyEvent != null) {
                    if (keyEvent.type == KeyEvent.KEY_DOWN) {
                        if (!keys[keyEvent.keyCode]) {
                            keyCount++;
                            keys[keyEvent.keyCode] = true;
                        }
                        keyJustPressed = true;
                        justPressedKeys[keyEvent.keyCode] = true;
                    }
                    if (keyEvent.type == KeyEvent.KEY_UP) {
                        if (keys[keyEvent.keyCode]) {
                            keyCount--;
                            keys[keyEvent.keyCode] = false;
                        }
                    }
                }
            }
        }
    }

    private List<KeyEvent> keyEvents;
    private RunKeyEvents runKeyEvents;

    @Override
    public void run() {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        System.out.println("READING");
        keyEvents = new ArrayList();
        runKeyEvents = new RunKeyEvents(keyEvents);

        try {
            reader = new BufferedReader(new FileReader("inputRecording.txt"));
            writer = new BufferedWriter(new FileWriter("testStateRecording.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("TYPE:" + line);
                KeyEvent keyEvent = null;
                TouchEvent touchEvent = null;
                FakeInput eventType = FakeInput.valueOf((line));
                line = reader.readLine();
                System.out.println("ACTION:" + line);
                switch(eventType){
                    case KEY_DOWN:
                        int value = Integer.parseInt(line);
                        keyEvent = new KeyEvent();
                        keyEvent.keyCode = value;
                        keyEvent.type = KeyEvent.KEY_DOWN;
                        if(inputHandler!=null){
                            inputHandler.keyDown();
                        }
                        keyEvents.add(keyEvent);
                        writeObject(recorded.getState(), writer);
                        break;
                    case KEY_UP:
                        value = Integer.parseInt(line);
                        keyEvent = new KeyEvent();
                        keyEvent.keyCode = value;
                        keyEvent.type = KeyEvent.KEY_UP;
                        writeObject(recorded.getState(), writer);
                        if(inputHandler!=null){
                            inputHandler.keyUp();
                        }
                        keyEvents.remove(keyEvent);
                    case KEY_TYPED:
                        break;
                    case WAIT:
                        value = Math.max(0,Integer.parseInt(line));
                        sleep(value);
                        break;
                    case TOUCH_UP:
                        touchEvent = parseTouchString(line);
                        if(inputHandler!=null){
                            inputHandler.touchUp(touchEvent.x, touchEvent.y, touchEvent.pointer, touchEvent.type);
                        }
                }
                Gdx.app.postRunnable(new EventTrigger(touchEvent,keyEvent));
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        try {
            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class RunKeyEvents implements Runnable{

        List<KeyEvent> keyEvents;

        RunKeyEvents(List<KeyEvent> keyEvents){
            this.keyEvents = keyEvents;
        }

        public void setKeyEvents(List<KeyEvent> keyEvents){
            this.keyEvents = keyEvents;
        }

        @Override
        public void run() {
            for(KeyEvent keyEvent : keyEvents) {
                System.out.println("Running Key Event:" + keyEvent.keyCode + " at type:" + keyEvent.type);
                Gdx.app.postRunnable(new EventTrigger(new TouchEvent(), keyEvent));
            }
        }
    }


    public TouchEvent parseTouchString(String s){
        String[] splitString = s.split(" ");
        splitString[0] = splitString[0].replace("X:","");
        splitString [1] = splitString[1].replace("Y:","");
        splitString[2] = splitString[2].replace("Pointer:", "");
        splitString[3] = splitString[3].replace("Button:","");
        int x = Integer.parseInt(splitString[0]);
        int y = Integer.parseInt(splitString[1]);
        int pointer = Integer.parseInt(splitString[2]);
        int button = Integer.parseInt(splitString[3]);
        TouchEvent touchEvent = new TouchEvent();
        touchEvent.x=x;
        touchEvent.y=y;
        touchEvent.pointer=pointer;
        touchEvent.type=button;
        return touchEvent;
    }

    private void record(Object object, List<String> fieldNames) throws NoSuchFieldException {
        for(String fieldName : fieldNames){
            Field field = object.getClass().getDeclaredField(fieldName);
            System.out.println(field.toString());
        }
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

    @Override
    public float getAccelerometerX () {
        return accel[0];
    }

    @Override
    public float getAccelerometerY () {
        return accel[1];
    }

    @Override
    public float getAccelerometerZ () {
        return accel[2];
    }

    @Override
    public float getGyroscopeX() {
        return 0;
    }

    @Override
    public float getGyroscopeY() {
        return 0;
    }

    @Override
    public float getGyroscopeZ() {
        return 0;
    }

    @Override
    public int getX () {
        return touchX[0];
    }

    @Override
    public int getX (int pointer) {
        return touchX[pointer];
    }

    @Override
    public int getY () {
        return touchY[0];
    }

    @Override
    public int getY (int pointer) {
        return touchY[pointer];
    }

    @Override
    public boolean isTouched () {
        return isTouched[0];
    }

    @Override
    public boolean justTouched () {
        return justTouched;
    }

    @Override
    public boolean isTouched (int pointer) {
        return isTouched[pointer];
    }

    @Override
    public boolean isButtonPressed (int button) {
        if (button != Buttons.LEFT) return false;
        for (int i = 0; i < isTouched.length; i++)
            if (isTouched[i]) return true;
        return false;
    }

    @Override
    public boolean isKeyPressed (int key) {
        if (key == Input.Keys.ANY_KEY) {
            return keyCount > 0;
        }
        if (key < 0 || key > 255) {
            return false;
        }
        return keys[key];
    }

    @Override
    public boolean isKeyJustPressed (int key) {
        if (key == Input.Keys.ANY_KEY) {
            return keyJustPressed;
        }
        if (key < 0 || key > 255) {
            return false;
        }
        return justPressedKeys[key];
    }

    @Override
    public void getTextInput (TextInputListener listener, String title, String text, String hint) {
        Gdx.app.getInput().getTextInput(listener, title, text, hint);
    }

    @Override
    public void setOnscreenKeyboardVisible (boolean visible) {
    }

    @Override
    public void vibrate (int milliseconds) {

    }

    @Override
    public void vibrate (long[] pattern, int repeat) {

    }

    @Override
    public void cancelVibrate () {

    }

    @Override
    public float getAzimuth () {
        return compass[0];
    }

    @Override
    public float getPitch () {
        return compass[1];
    }

    @Override
    public float getRoll () {
        return compass[2];
    }

    @Override
    public void setCatchBackKey (boolean catchBack) {

    }

    @Override
    public boolean isCatchBackKey() {
        return false;
    }

    @Override
    public void setCatchMenuKey (boolean catchMenu) {

    }

    @Override
    public boolean isCatchMenuKey() {
        return false;
    }

    @Override
    public void setInputProcessor (com.badlogic.gdx.InputProcessor processor) {
        this.processor = processor;
    }

    @Override
    public com.badlogic.gdx.InputProcessor getInputProcessor () {
        return this.processor;
    }

    ///** @return the IP addresses {@link RemoteSender} or gdx-remote should connect to. Most likely the LAN addresses if behind a NAT. */
    //public String[] getIPs () {
    //    return ips;
    //}

    @Override
    public boolean isPeripheralAvailable (Peripheral peripheral) {
        if (peripheral == Peripheral.Accelerometer) return true;
        if (peripheral == Peripheral.Compass) return true;
        if (peripheral == Peripheral.MultitouchScreen) return multiTouch;
        return false;
    }

    @Override
    public int getRotation () {
        return 0;
    }

    @Override
    public Orientation getNativeOrientation () {
        return Orientation.Landscape;
    }

    @Override
    public void setCursorCatched (boolean catched) {

    }

    @Override
    public boolean isCursorCatched () {
        return false;
    }

    @Override
    public int getDeltaX () {
        return deltaX[0];
    }

    @Override
    public int getDeltaX (int pointer) {
        return deltaX[pointer];
    }

    @Override
    public int getDeltaY () {
        return deltaY[0];
    }

    @Override
    public int getDeltaY (int pointer) {
        return deltaY[pointer];
    }

    @Override
    public void setCursorPosition (int x, int y) {
    }

    public void setCursorImage(Pixmap pixmap, int i, int i1) {

    }

    @Override
    public long getCurrentEventTime () {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void getRotationMatrix (float[] matrix) {
        // TODO Auto-generated method stub

    }
}
