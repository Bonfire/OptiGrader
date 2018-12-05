package us.to.opti_grader.optigrader;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Date;


public class SessionHandler {
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_USERNAME = "login";
    private static final String KEY_EXPIRES = "expires";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_F_NAME = "firstName";
    private static final String KEY_L_NAME = "lastName";
    private static final String KEY_EMPTY = "";
    private static final String KEY_TOKEN = "token";
    private Context mContext;
    private SharedPreferences.Editor mEditor;
    private SharedPreferences mPreferences;

    public SessionHandler(Context mContext) {
        this.mContext = mContext;
        mPreferences = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.mEditor = mPreferences.edit();
    }

    /**
     * Logs in the user by saving user details and setting session
     *
     * @param login
     * @param fName
     * @param fName
     * @param token
     *
     */
    public void loginUser(String login, String fName, String lName, String token) {
        mEditor.putString(KEY_USERNAME, login);
        mEditor.putString(KEY_F_NAME, fName);
        mEditor.putString(KEY_L_NAME, lName);
        mEditor.putString(KEY_TOKEN, token);
        Date date = new Date();

        //Set user session for next 7 days
        long millis = date.getTime() + (7 * 24 * 60 * 60 * 1000);
        mEditor.putLong(KEY_EXPIRES, millis);
        mEditor.commit();
    }

    /**
     * Checks whether user is logged in
     *
     * @return
     */
    public boolean isLoggedIn() {
        Date currentDate = new Date();

        long millis = mPreferences.getLong(KEY_EXPIRES, 0);

        /* If shared preferences does not have a value
         then user is not logged in
         */
        if (millis == 0) {
            return false;
        }
        Date expiryDate = new Date(millis);

        /* Check if session is expired by comparing
        current date and Session expiry date
        */
        return currentDate.before(expiryDate);
    }

    /**
     * Fetches and returns user details
     *
     * @return user details
     */
    public user getUserDetails() {
        //Check if user is logged in first
        if (!isLoggedIn()) {
            return null;
        }
        user user = new user();
        user.setToken(mPreferences.getString(KEY_TOKEN, KEY_EMPTY));
        user.setUsername(mPreferences.getString(KEY_USERNAME, KEY_EMPTY));
        user.setFullName(mPreferences.getString(KEY_F_NAME, KEY_EMPTY),mPreferences.getString(KEY_L_NAME, KEY_EMPTY));
        user.setSessionExpiryDate(new Date(mPreferences.getLong(KEY_EXPIRES, 0)));

        return user;
    }

    /**
     * Logs out user by clearing the session
     */
    public void logoutUser(){
        mEditor.clear();
        mEditor.commit();
    }

}