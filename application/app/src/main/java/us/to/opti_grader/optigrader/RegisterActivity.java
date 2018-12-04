package us.to.opti_grader.optigrader;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import us.to.optigrader.optigrader.R;

public class RegisterActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

    }

    public void register(View v){
        submitRegistration();
    }



    public JsonObject submitRegistration() {
        String firstName, lastName, login, password;

        login=findViewById(R.id.registerEM).toString();
        password=findViewById(R.id.registerPW).toString();
        firstName=findViewById(R.id.registerFN).toString();
        lastName=findViewById(R.id.registerLN).toString();


        try {
            JsonObject payload = new JsonObject();


            payload.addProperty("firstName", firstName);
            payload.addProperty("lastname", lastName);
            payload.addProperty("login", login);
            payload.addProperty("password", password);

            final Document document = Jsoup.connect("http://10.0.2.2:80/register.php")
                    .requestBody(payload.toString())
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .post();
            //ensure that the response is not null
            final String response = document.text();
            //Assertions.assertNotNull(response, "API response was a null string");
            // ensure that the response is  json object
            final JsonObject object = new JsonParser().parse(response).getAsJsonObject();

            //Assertions.assertNotNull(object, "API response was not a valid JSON object.");
            return object;
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Assertions.fail("unable to connect to API.");
        return new JsonObject();

    }
//endoftest

    public void writeJSON(View v) {
        String login=findViewById(R.id.registerEM).toString();
        String password=findViewById(R.id.registerPW).toString();
        String firstName=findViewById(R.id.registerFN).toString();
        String lastName=findViewById(R.id.registerLN).toString();
        String email;
        //email=findViewById(R.id.registerEM).getText().toString();



        JSONObject payload = new JSONObject();
        try {
            payload.put("firstName", firstName);
            payload.put("lastName", lastName);
            payload.put("login", login);
            payload.put("password", password);
            //payload.put("email", email);

            /*
            payload.put("firstName", "mike");
            payload.put("lastName", "doe");
            payload.put("login", "mike@gmail.com");
            payload.put("password", "somepass");
            */

            SendDeviceDetails sendDetails = new SendDeviceDetails();
            sendDetails.execute("http://10.0.2.2:80/project/register.php", payload.toString());

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
