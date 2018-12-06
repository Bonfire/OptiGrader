package us.to.opti_grader.optigrader;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import us.to.optigrader.optigrader.R;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String KEY_STATUS = "status";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_F_NAME = "firstName";
    private static final String KEY_L_NAME = "lastName";
    private static final String KEY_NID = "id";
    private static final String KEY_USERNAME = "login";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_PROF = "user_mode";


    private static final String KEY_EMPTY = "";

    private EditText emailEdit;
    private EditText passwordEdit;
    private EditText etConfirmPassword;
    private EditText etConfirmEmail;
    private EditText eFName;
    private EditText eLName;
    private EditText eNID;

    private String login;
    private String password;
    private String confirmPassword;
    private String confirmEmail;
    private String NID;
    private String fName;
    private String lName;

    CheckBox ch1;





    private ProgressDialog pDialog;
    private String register_url = "https://optigrader.mahabal.org:8080/register";
    //private String register_url = "http://10.0.2.2:80/member/register.php";
    private SessionHandler session;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionHandler(getApplicationContext());
        setContentView(R.layout.activity_register);

        emailEdit = findViewById(R.id.registerEM);
        passwordEdit = findViewById(R.id.registerPW);
        etConfirmPassword = findViewById(R.id.cregisterPW);
        etConfirmEmail = findViewById(R.id.cregisterEM);
        eFName = findViewById(R.id.registerFN);
        eLName = findViewById(R.id.registerLN);
        //eNID = findViewById(R.id.registerNID);

        Button registerbtn = findViewById(R.id.registerBtn);
        ch1=(CheckBox)findViewById(R.id.instructorcheckBox);

        registerbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Retrieve the data entered in the edit texts
                login = emailEdit.getText().toString().toLowerCase().trim();
                confirmEmail=etConfirmEmail.getText().toString().trim().toLowerCase();
                password = passwordEdit.getText().toString().trim();
                confirmPassword = etConfirmPassword.getText().toString().trim();
                lName = eLName.getText().toString().trim();
                fName = eFName.getText().toString().trim();
                //NID = eNID.getText().toString().trim();




                if (validateInputs()) {
                    registerUser();
                }

            }
        });


    }

    private boolean validateInputs() {
        if (KEY_EMPTY.equals(fName)) {
            eFName.setError("First Name cannot be empty");
            eFName.requestFocus();
            return false;

        }
        if (KEY_EMPTY.equals(lName)) {
            eLName.setError("Last Name cannot be empty");
            eLName.requestFocus();
            return false;

        }
        if (KEY_EMPTY.equals(login)) {
            emailEdit.setError("Login cannot be empty");
            emailEdit.requestFocus();
            return false;
        }
        if (KEY_EMPTY.equals(password)) {
            passwordEdit.setError("Password cannot be empty");
            passwordEdit.requestFocus();
            return false;
        }

        if (KEY_EMPTY.equals(confirmPassword)) {
            etConfirmPassword.setError("Confirm Password cannot be empty");
            etConfirmPassword.requestFocus();
            return false;
        }
        if (KEY_EMPTY.equals(confirmEmail)) {
            etConfirmEmail.setError("Confirm Email cannot be empty");
            etConfirmEmail.requestFocus();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Password and Confirm Password does not match");
            etConfirmPassword.requestFocus();
            return false;
        }
        if (!login.equals(confirmEmail)) {
            etConfirmEmail.setError("Email and Confirm Email does not match");
            etConfirmEmail.requestFocus();
            return false;
        }

        return true;
    }

    private void loadHomepage() {
        Intent i = new Intent(getApplicationContext(), HomepageActivity.class);
        startActivity(i);
        finish();

    }

    private void registerUser() {

        JSONObject request = new JSONObject();

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

        //password= Hashing.sha256().hashString(password, StandardCharsets.UTF_8).toString();
        try {
            //Populate the request parameters
            request.put(KEY_USERNAME, login);
            request.put(KEY_PASSWORD, password);
            request.put(KEY_F_NAME, fName);
            request.put(KEY_L_NAME, lName);
            //request.put(KEY_NID, NID);

            if(ch1.isChecked()){
                request.put(KEY_PROF, "1");
            }
            else
                request.put(KEY_PROF, "0");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest jsArrayRequest = new JsonObjectRequest
                (Request.Method.POST, register_url, request, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, response.toString());
                        //pDialog.dismiss();
                        try {
                            //Check if user got registered successfully
                            if (response.has(KEY_TOKEN)) {
                                //Set the user session
                                session.loginUser(login,fName,lName, response.getString(KEY_TOKEN));
                                loadHomepage();

                            }else if(response.getInt(KEY_STATUS) == 1){
                                //Display error message if username is already existsing
                                emailEdit.setError("User already exists");
                                emailEdit.requestFocus();

                            }else{

                                Toast.makeText(getApplicationContext(),
                                        response.getString(KEY_MESSAGE), Toast.LENGTH_SHORT).show();

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //pDialog.dismiss();
                        Log.d("error.Response", error.toString());

                        //Display error message whenever an error occurs
                        Toast.makeText(getApplicationContext(),
                                error.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });

        // Access the RequestQueue through your singleton class.
        MySingleton.getInstance(this).addToRequestQueue(jsArrayRequest);
    }



//endoftest






}
