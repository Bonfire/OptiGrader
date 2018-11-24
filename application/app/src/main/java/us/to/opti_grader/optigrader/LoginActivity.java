package us.to.opti_grader.optigrader;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import us.to.optigrader.optigrader.R;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    public void openCapture(View v){
        Intent openCapture = new Intent(this, CameraActivity.class);
        startActivity(openCapture);
    }
}
