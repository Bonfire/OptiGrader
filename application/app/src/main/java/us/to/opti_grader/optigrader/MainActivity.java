package us.to.opti_grader.optigrader;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.util.Log;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    static {
        if (OpenCVLoader.initDebug()) {
            Log.i("opencv","OpenCV initialization successful");
        } else {
            Log.i("opencv","OpenCV initialization failed");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        int rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_CROSSFADE;
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.rotationAnimation = rotationAnimation;
        win.setAttributes(winParams);
    }

    public void sendMessage(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.enter_id);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    public void CameraActivity(View view) {
        Intent intent = new Intent(this, CameraActivity.class);
        //EditText editText = (EditText) findViewById(R.id.enter_id);
        //String message = editText.getText().toString();
        //intent.putExtra(EXTRA_MESSAGE, message);


        // I dont have the openCV stuff but this should start it
        startActivity(intent);
    }

    public final static String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
}
