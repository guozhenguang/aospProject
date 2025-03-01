package android.hardware.input;

import android.view.InputEvent;

public class InputManager {
    public static android.hardware.input.InputManager getInstance(){
        return  null;
    }

    public boolean injectInputEvent(InputEvent event, int mode){
        return true;
    }
}
