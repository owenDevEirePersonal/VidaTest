package com.deveire.dev.vidatest;

import android.os.Build;
import android.support.v4.app.NotificationCompatSideChannelService;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.api.Listener;
import com.amazon.identity.auth.device.api.authorization.AuthCancellation;
import com.amazon.identity.auth.device.api.authorization.AuthorizationManager;
import com.amazon.identity.auth.device.api.authorization.AuthorizeListener;
import com.amazon.identity.auth.device.api.authorization.AuthorizeRequest;
import com.amazon.identity.auth.device.api.authorization.AuthorizeResult;
import com.amazon.identity.auth.device.api.authorization.ProfileScope;
import com.amazon.identity.auth.device.api.authorization.Scope;
import com.amazon.identity.auth.device.api.authorization.ScopeFactory;
import com.amazon.identity.auth.device.api.authorization.User;
import com.amazon.identity.auth.device.api.workflow.RequestContext;
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity
{
    private RequestContext requestContext;

    private Button amazonLoginButton;
    private Button vidaButton;

    private boolean isLoggedInAsOfLastCheck;

    private String userProfileName;

    private static final String PRODUCT_ID = "VidaTest1";
    private static final String PRODUCT_DSN = Build.SERIAL;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestContext = RequestContext.create(this);

        vidaButton = (Button) findViewById(R.id.vidaButton);
        vidaButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Log.i("Vida", "Button Pressed, starting vida");
                Log.i("Vida", "Stored Amazon Account Details:" + userProfileName);
            }
        });

        isLoggedInAsOfLastCheck = false;

        amazonLoginButton = (Button) findViewById(R.id.amazonLoginButton);
        amazonLoginButton.setOnClickListener(new loginButtonOnClickListener());

        requestContext.registerListener(new AuthorizeListener() {

            /* Authorization was completed successfully. */
            @Override
            public void onSuccess(AuthorizeResult result) {
                /* Your app is now authorized for the requested scopes */
                vidaButton.setEnabled(true);
                Log.i("AmazonLogin", "Login Success");
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        amazonLoginButton.setText("Logout");
                    }
                });
            }

            /* There was an error during the attempt to authorize the
            application. */
            @Override
             public void onError(AuthError ae) {
                /* Inform the user of the error */
                vidaButton.setEnabled(false);
                Log.i("AmazonLogin", "Login Error: " + ae.toString());
            }

            /* Authorization was cancelled before it could be completed. */
            @Override
            public void onCancel(AuthCancellation cancellation) {
                /* Reset the UI to a ready-to-login state */
                vidaButton.setEnabled(false);
                Log.i("AmazonLogin", "Login Cancel");
            }
        });

        userProfileName = "Not Found";


    }

    @Override
    protected void onResume()
    {
        super.onResume();
        requestContext.onResume();
    }

    private void loginAmazon()
    {

        final JSONObject scopeData = new JSONObject();
        final JSONObject productInstanceAttributes = new JSONObject();

        try
        {
            productInstanceAttributes.put("deviceSerialNumber", PRODUCT_DSN);
            scopeData.put("productInstanceAttributes", productInstanceAttributes);
            scopeData.put("productID", PRODUCT_ID);
            AuthorizationManager.authorize(new AuthorizeRequest
                    .Builder(requestContext).addScope(ScopeFactory.scopeNamed("alexa:all", scopeData))
                    .forGrantType(AuthorizeRequest.GrantType.ACCESS_TOKEN)
                    .shouldReturnUserData(false)
                    .build());
        }
        catch (JSONException e)
        {

        }

        //fetchUserDetails();

    }

    private void fetchUserDetails()
    {
        User.fetch(this, new Listener<User,AuthError>() {

            /* fetch completed successfully. */
            @Override
            public void onSuccess(User user) {
                Log.i("AmazonLogin", "User Fetch Successful");
                final String account = user.getUserId();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        userProfileName = account;
                        Log.i("AmazonLogin","User Profile = " + account);
                    }
                });
            }

            /* There was an error during the attempt to get the profile. */
            @Override
            public void onError(AuthError ae) {
                Log.e("AmazonLogin", "User Fetch Error: " + ae.toString());
                /* Retry or inform the user of the error */
            }
        });
    }

    private void isLoggedInForStart()
    {
        AuthorizationManager.getToken(this, new Scope[]{ProfileScope.profile()}, new Listener<AuthorizeResult, AuthError>()
        {
            @Override
            public void onSuccess(AuthorizeResult result) {
                if (result.getAccessToken() != null) {
                    /* The user is signed in */
                    Log.i("AmazonLogin", "OnStart: User is still signed in.");
                    isLoggedInAsOfLastCheck = true;
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            amazonLoginButton.setText("Logout");
                        }
                    });
                } else {
                    /* The user is not signed in */
                    Log.i("AmazonLogin", "OnStart: User is not signed in.");
                    isLoggedInAsOfLastCheck = false;
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            amazonLoginButton.setText("Login");
                        }
                    });
                }
            }

            @Override
            public void onError(AuthError ae) {
                /* The user is not signed in */
                Log.e("AmazonLogin", "OnStart: Error getting token: " + ae.toString());
                isLoggedInAsOfLastCheck = false;
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        amazonLoginButton.setText("Login");
                    }
                });
            }
        });
    }

    private void logoutAmazon()
    {
        AuthorizationManager.signOut(getApplicationContext(), new Listener<Void, AuthError>() {
            @Override
            public void onSuccess(Void response) {
                // Set logged out state in UI
                Log.i("AmazonLogin", "Successfully Logged Out");
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        amazonLoginButton.setText("Login");
                    }
                });

            }
            @Override
            public void onError(AuthError authError) {
                // Log the error
                Log.i("AmazonLogin", "Error loggingout getting token: " + authError.toString());
            }
        });
    }

    private class loginButtonOnClickListener implements View.OnClickListener
    {
        @Override
        public void onClick(View v)
        {
            AuthorizationManager.getToken(getApplicationContext(), new Scope[]{ProfileScope.profile()}, new Listener<AuthorizeResult, AuthError>()
            {
                @Override
                public void onSuccess(AuthorizeResult result) {
                    if (result.getAccessToken() != null) {
                    /* The user is signed in */
                        Log.i("AmazonLogin", " User is still signed in.");
                        isLoggedInAsOfLastCheck = true;
                        logoutAmazon();

                    } else {
                    /* The user is not signed in */
                        Log.i("AmazonLogin", " User is not signed in.");
                        isLoggedInAsOfLastCheck = false;
                        loginAmazon();
                    }
                }

                @Override
                public void onError(AuthError ae) {
                /* The user is not signed in */
                    Log.e("AmazonLogin", "OnStart: Error getting token: " + ae.toString());
                    isLoggedInAsOfLastCheck = false;
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            amazonLoginButton.setText("Login");
                        }
                    });
                }
            });
        }
    }
}

//API key: eyJhbGciOiJSU0EtU0hBMjU2IiwidmVyIjoiMSJ9.eyJ2ZXIiOiIzIiwiZW5kcG9pbnRzIjp7ImF1dGh6IjoiaHR0cHM6Ly93d3cuYW1hem9uLmNvbS9hcC9vYSIsInRva2VuRXhjaGFuZ2UiOiJodHRwczovL2FwaS5hbWF6b24uY29tL2F1dGgvbzIvdG9rZW4ifSwiY2xpZW50SWQiOiJhbXpuMS5hcHBsaWNhdGlvbi1vYTItY2xpZW50Ljc3Y2U3YmEyNDI4OTRiN2ZhOGYzNDA4ODgxOWJjNmFmIiwiYXBwRmFtaWx5SWQiOiJhbXpuMS5hcHBsaWNhdGlvbi4zNDZlMjY2NzI1YzY0OTY2ODE5MmNhOTZmYThjMTViMCIsImlzcyI6IkFtYXpvbiIsInR5cGUiOiJBUElLZXkiLCJwa2ciOiJjb20uZGV2ZWlyZS5kZXYudmlkYXRlc3QiLCJhcHBWYXJpYW50SWQiOiJhbXpuMS5hcHBsaWNhdGlvbi1jbGllbnQuYmRjNWEzMmI0OWNmNDJhMGFhODMwNDg2MWVlNDlhOTAiLCJ0cnVzdFBvb2wiOm51bGwsImFwcHNpZ1NoYTI1NiI6IjA1OjIwOjAwOkYyOjU4OjJBOjUzOjREOkM2OjI5OkQ2OjE2OjYyOkEwOkEzOjVBOjBCOjQ2Ojk5OjQwOjIyOjlGOkRFOjJCOkI5OkQxOkY1OjVBOjNCOkNBOkQ4OjJBIiwiYXBwc2lnIjoiQjM6Q0E6NTY6NTY6MEQ6Rjc6NUU6QzM6QkI6RDg6NkI6OUU6RDM6MEU6NjY6QjEiLCJhcHBJZCI6ImFtem4xLmFwcGxpY2F0aW9uLWNsaWVudC5iZGM1YTMyYjQ5Y2Y0MmEwYWE4MzA0ODYxZWU0OWE5MCIsImlkIjoiN2U3YTljMjUtMTcyNC00N2U2LTkwMWUtZmVmMjgzMjVjMDZmIiwiaWF0IjoiMTUyOTQ4ODk2ODgyOCJ9.Jpps5XhxwW9TYFjF/arRDZDsOxW1y69Ngc6kzHxkrWKLiog3OomsHEmFC5nSDFx/N8Mc4sOFMfN5PImg6KmwLUtzj0C05ZPKtiwvc7NJtRRuX3zSwFG31MZaPBDjCZcKnpDzJiWlZYeT1IUhfPVflIHzH5Gb0DJfkmSpjxQajz1QycDE2ZvxXvAvsFVkDhepaZzR69GSUIc/sL47l3Lq/khIT04LSQZ/fVbF+f7FKjP7fwV2I/UyGBN/3YlXo5UKBtlmtg/As8q1zGSileCQ0o2M39LIsdJDw4MEkmUWZri+y74lOOfi5Mypf9TDFI5ly+Vpb5hJB7P6OTK8CCk7XQ==