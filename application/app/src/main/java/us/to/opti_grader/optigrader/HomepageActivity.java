package us.to.opti_grader.optigrader;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import us.to.optigrader.optigrader.R;

public class HomepageActivity extends AppCompatActivity {
    private String login_url = "https://optigrader.mahabal.org:8080/login";
    private SessionHandler session;
    private static final String KEY_NID = "id";
    private static final String KEY_USERNAME = "login";
    private static final String KEY_SCORE= "score";
    private static final String KEY_MESSAGE = "message";
    String score = "null";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);
        session = new SessionHandler(getApplicationContext());
        user user = session.getUserDetails();
        TextView welcomeText = findViewById(R.id.welcomeText);

        //welcomeText.setText("Welcome "+user.getFullName()+", your session will expire on "+user.getSessionExpiryDate());
        welcomeText.setText("Welcome "+user.getFullName()+", your score is "+ showGrade(user));

        Button logoutBtn = findViewById(R.id.logoutBtn);

        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                session.logoutUser();
                Intent i = new Intent(HomepageActivity.this, MainActivity.class);
                startActivity(i);
                finish();

            }
        });
    }

    private String showGrade(user user) {
        //displayLoader();
        String login = user.login;

        JSONObject request = new JSONObject();
        try {
            //Populate the request parameters
            request.put(KEY_USERNAME, login);


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

                            if (response.getInt(KEY_SCORE)>0 && response.getInt(KEY_SCORE)<500) {

                                score = response.getString(KEY_SCORE);


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

                        //Display error message whenever an error occurs
                        Toast.makeText(getApplicationContext(),
                                error.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });


        // Access the RequestQueue through your singleton class.
        MySingleton.getInstance(this).addToRequestQueue(jsArrayRequest);
        return score;
    }


}
