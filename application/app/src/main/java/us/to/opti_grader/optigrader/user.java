package us.to.opti_grader.optigrader;

import java.util.Date;

public class user{
    String login;
    String fullName;
    String fName;
    String lName;
    Date sessionExpiryDate;

    public void setUsername(String login) {
        this.login = login;
    }

    public void setFullName(String fName, String lName) {
        this.fullName = fName+" "+lName;
    }

    public void setSessionExpiryDate(Date sessionExpiryDate) {
        this.sessionExpiryDate = sessionExpiryDate;
    }

    public String getUsername() {
        return login;
    }

    public String getFullName() {
        return fullName;
    }

    public Date getSessionExpiryDate() {
        return sessionExpiryDate;
    }
}