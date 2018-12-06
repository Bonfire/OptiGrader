package us.to.opti_grader.optigrader;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import us.to.optigrader.optigrader.R;

public class MainActivity extends AppCompatActivity {

    private SessionHandler session;
    String JSON_STRING;

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

        session = new SessionHandler(getApplicationContext());
        if(session.isLoggedIn()){
            loadHomepage();
        }
        user user = session.getUserDetails();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        int rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_CROSSFADE;
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.rotationAnimation = rotationAnimation;
        win.setAttributes(winParams);
    }

    private void loadHomepage() {
        Intent i = new Intent(getApplicationContext(), HomepageActivity.class);
        startActivity(i);
        finish();
    }

    public final static String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";

    public void gotoSignIn(View v){
        Intent loginIntent = new Intent(this, LoginActivity.class);
        startActivity(loginIntent);
    }

    public void gotoRegister(View v){
        Intent registerIntent = new Intent(this, RegisterActivity.class);
        startActivity(registerIntent);
    }

    public void gotoJSON(View v){

        Intent jsonIntent = new Intent(this, JsonActivity.class);
        startActivity(jsonIntent);

    }



    class BackgroundTask extends AsyncTask<Void,Void,String>{

        String json_url;
        @Override
        protected void onPreExecute() {
           json_url = " https://mikalyoung1.000webhostapp.com/dbscript.php";
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(json_url);
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader= new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder=new StringBuilder();
                while((JSON_STRING = bufferedReader.readLine())!= null){

                    stringBuilder.append(JSON_STRING+"\n");
                }
                bufferedReader.close();
                inputStream.close();
                httpURLConnection.disconnect();
                return stringBuilder.toString().trim();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {

            TextView textView =(TextView) findViewById(R.id.jsontextView);
            textView.setText(result);
        }
    }
}
