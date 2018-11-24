package us.to.opti_grader.optigrader;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import us.to.optigrader.optigrader.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void gotoSignIn(View v){
        Intent loginIntent = new Intent(this, LoginActivity.class);
        startActivity(loginIntent);
    }

    public void gotoRegister(View v){
        Intent registerIntent = new Intent(this, RegisterActivity.class);
        startActivity(registerIntent);
    }
}
