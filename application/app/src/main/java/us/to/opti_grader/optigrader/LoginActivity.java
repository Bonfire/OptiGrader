package us.to.opti_grader.optigrader;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import us.to.optigrader.optigrader.R;

public class LoginActivity extends AppCompatActivity {

    String username, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        writeJSON();



    }

    public void dologin(View v){
        EditText emailEdit;
        EditText passwordEdit;


        emailEdit= (EditText)findViewById(R.id.loginEmail);
        passwordEdit=(EditText)findViewById(R.id.loginPassword);

        Log.v("EditText", emailEdit.getText().toString());
        username=emailEdit.getText().toString();
        //password=passwordEdit.getText().toString();
        password=findViewById(R.id.loginPassword).toString();


    }

    public void writeJSON() {
        JSONObject object = new JSONObject();
        try {
            object.put("login", username);
            object.put("password", password);

            SendDeviceDetails sendDetails = new SendDeviceDetails();
            sendDetails.execute("https://optigrader.mahabal.org:8080/login", object.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void openCapture(View v){

        Intent openCapture = new Intent(this, CameraActivity.class);
        startActivity(openCapture);


    }
    private class SendDeviceDetails extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            String data = "";

            HttpURLConnection httpURLConnection = null;
            try {

                httpURLConnection = (HttpURLConnection) new URL(params[0]).openConnection();
                httpURLConnection.setRequestMethod("POST");

                httpURLConnection.setDoOutput(true);

                DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
                wr.writeBytes("PostData=" + params[1]);
                wr.flush();
                wr.close();

                InputStream in = httpURLConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(in);

                int inputStreamData = inputStreamReader.read();
                while (inputStreamData != -1) {
                    char current = (char) inputStreamData;
                    inputStreamData = inputStreamReader.read();
                    data += current;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            }

            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.e("TAG", result); // this is expecting a response code to be sent from your server upon receiving the POST data
        }
    }
}


