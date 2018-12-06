package us.to.opti_grader.optigrader;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import us.to.optigrader.optigrader.R;

//import com.google.common.hash.Hashing;



public class LoginActivity extends AppCompatActivity {
    private static final String KEY_STATUS = "status";
    private static final String KEY_MESSAGE = "status";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_F_NAME = "firstName";
    private static final String KEY_L_NAME = "lastName";
    private static final String KEY_USERNAME = "login";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_TOKEN = "token";

    private static final String KEY_EMPTY = "";
    private EditText emailEdit;
    private EditText passwordEdit;
    private String login;
    private String password;
    private ProgressDialog pDialog;
    private String login_url = "https://optigrader.mahabal.org:8080/login";
    //private String login_url = "http://10.0.2.2:80/member/login.php";
    private SessionHandler session;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionHandler(getApplicationContext());
        if(session.isLoggedIn()){
            loadHomepage();
        }

        setContentView(R.layout.activity_login);
        emailEdit = (EditText)findViewById(R.id.loginEmail);
        passwordEdit=(EditText)findViewById(R.id.loginPassword);


        Button loginbtn = findViewById(R.id.loginBtn);

        loginbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Retrieve the data entered in the edit texts

                login=emailEdit.getText().toString().toLowerCase();
                password=passwordEdit.getText().toString();

                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    String text = password;
                    // Change this to UTF-16 if needed
                    md.update(text.getBytes(StandardCharsets.UTF_8));
                    byte[] digest = md.digest();
                    String hex = String.format("%064x", new BigInteger(1, digest));
                    password = hex;
                } catch (NoSuchAlgorithmException e)
                {}

                if (validateInputs()) {
                    login();
                }
            }
        });

    }





    private void loadHomepage() {
        Intent i = new Intent(getApplicationContext(), HomepageActivity.class);
        startActivity(i);
        finish();

    }



    // copy test

    private void login() {
        //displayLoader();
        JSONObject request = new JSONObject();
        try {
            //Populate the request parameters
            request.put(KEY_USERNAME, login);
            request.put(KEY_PASSWORD, password);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest jsArrayRequest = new JsonObjectRequest
                (Request.Method.POST, login_url, request, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //pDialog.dismiss();
                        try {
                            //Check if user got logged in successfully

                            if (response.has("token")) {
                                //session.loginUser(login,response.getString(KEY_F_NAME),response.getString(KEY_L_NAME));
                                session.loginUser(login,"fName","fName", response.getString(KEY_TOKEN));
                                loadHomepage();

                            }else if(response instanceof JSONObject ){
                                Toast.makeText(getApplicationContext(),
                                        response.getString(KEY_STATUS), Toast.LENGTH_SHORT).show();

                            }
                            else{
                                Toast.makeText(getApplicationContext(),"incorrect login",Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //pDialog.dismiss();

                        //Display error message whenever an error occurs
                        //Toast.makeText(getApplicationContext(),
                          //      error.getMessage(), Toast.LENGTH_SHORT).show();
                        Toast.makeText(getApplicationContext(),"incorrect login",Toast.LENGTH_SHORT).show();

                    }
                });

        // Access the RequestQueue through your singleton class.
        MySingleton.getInstance(this).addToRequestQueue(jsArrayRequest);
    }

    private boolean validateInputs() {
        if(KEY_EMPTY.equals(login)){
            emailEdit.setError("Username cannot be empty");
            emailEdit.requestFocus();
            return false;
        }
        if(KEY_EMPTY.equals(password)){
            passwordEdit.setError("Password cannot be empty");
            passwordEdit.requestFocus();
            return false;
        }
        return true;
    }

    //copy test







    public void dologin(View v) {
        String login, password;
        EditText emailEdit = (EditText)findViewById(R.id.loginEmail);
        EditText passwordEdit=(EditText)findViewById(R.id.loginPassword);

        login=emailEdit.getText().toString();
        password=passwordEdit.getText().toString();

        JSONObject payload = new JSONObject();
        try {

            //payload.put("firstName", firstName);
            //payload.put("lastName", lastName);
            payload.put("login", login);
            payload.put("password", password);
            //payload.put("email", email);

            /*
            payload.put("firstName", "mike");
            payload.put("lastName", "doe");
            payload.put("login", "mike@gmail.com");
            payload.put("password", "somepass");
            payload.put("email", "email");
            */

            SendDeviceDetails sendDetails = new SendDeviceDetails();
            sendDetails.execute("http://10.0.2.2:80/project/register.php", payload.toString());
            finish();

        } catch (JSONException e) {
            e.printStackTrace();
        }
        //return payload;

    }

    private class SendDeviceDetails extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            String data = "";

            HttpURLConnection httpURLConnection = null;
            try {

                httpURLConnection = (HttpURLConnection) new URL(params[0]).openConnection();
                httpURLConnection.setRequestMethod("POST");

                //testing
                //httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                httpURLConnection.setDoOutput(true);

                DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
                wr.writeBytes(/*"PostData=" + */params[1]);
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
                //finish();
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


