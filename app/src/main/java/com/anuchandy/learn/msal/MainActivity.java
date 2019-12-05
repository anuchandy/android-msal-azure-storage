package com.anuchandy.learn.msal;

import android.os.Bundle;

import com.jakewharton.threetenabp.AndroidThreeTen;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.PublicClientApplication;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.app.Activity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.microsoft.identity.client.*;
import com.microsoft.identity.client.exception.*;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    final static String SCOPES [] =  {"https://anustorageandroid.blob.core.windows.net/.default" };
    final static String STORAGE_URL = "https://anustorageandroid.blob.core.windows.net/firstcontainer?restype=container&comp=list";
    private static final String TAG = MainActivity.class.getSimpleName();
    Button callStorageButton;
    Button signOutButton;

    private PublicClientApplication sampleApp;
    private IAuthenticationResult authResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidThreeTen.init(this);
        setContentView(R.layout.activity_main);

        callStorageButton = (Button) findViewById(R.id.callStorage);
        signOutButton = (Button) findViewById(R.id.clearCache);

        callStorageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onCallStorageClicked();
            }
        });

        signOutButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onSignOutClicked();
            }
        });
        sampleApp = new PublicClientApplication(
                this.getApplicationContext(),
                R.raw.auth_config);

        sampleApp.getAccounts(new PublicClientApplication.AccountsLoadedCallback() {
            @Override
            public void onAccountsLoaded(final List<IAccount> accounts) {
                if (!accounts.isEmpty()) {
                    sampleApp.acquireTokenSilentAsync(SCOPES, accounts.get(0), getAuthSilentCallback());
                } else {
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateSuccessUI() {
        callStorageButton.setVisibility(View.INVISIBLE);
        signOutButton.setVisibility(View.VISIBLE);
        findViewById(R.id.welcome).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.welcome)).setText("Welcome, " +
                authResult.getAccount().getUsername());
        findViewById(R.id.graphData).setVisibility(View.VISIBLE);
    }

    private void updateSignedOutUI() {
        callStorageButton.setVisibility(View.VISIBLE);
        signOutButton.setVisibility(View.INVISIBLE);
        findViewById(R.id.welcome).setVisibility(View.INVISIBLE);
        findViewById(R.id.graphData).setVisibility(View.INVISIBLE);
        ((TextView) findViewById(R.id.graphData)).setText("No Data");

        Toast.makeText(getBaseContext(), "Signed Out!", Toast.LENGTH_SHORT)
                .show();
    }

    private void onCallStorageClicked() {
        sampleApp.acquireToken(getActivity(), SCOPES, getAuthInteractiveCallback());
    }

    public Activity getActivity() {
        return this;
    }

    private AuthenticationCallback getAuthSilentCallback() {
        return new AuthenticationCallback() {

            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Log.d(TAG, "Successfully authenticated");
                authResult = authenticationResult;
                callStorageAPI();
                updateSuccessUI();
            }

            @Override
            public void onError(MsalException exception) {
                Log.d(TAG, "Authentication failed: " + exception.toString());

                if (exception instanceof MsalClientException) {
                } else if (exception instanceof MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                } else if (exception instanceof MsalUiRequiredException) {
                    /* Tokens expired or no session, retry with interactive */
                }
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "User cancelled login.");
            }
        };
    }

    private AuthenticationCallback getAuthInteractiveCallback() {
        return new AuthenticationCallback() {

            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Log.d(TAG, "Successfully authenticated");
                Log.d(TAG, "ID Token: " + authenticationResult.getIdToken());
                authResult = authenticationResult;
                callStorageAPI();
                updateSuccessUI();
            }

            @Override
            public void onError(MsalException exception) {
                /* Failed to acquireToken */
                Log.d(TAG, "Authentication failed: " + exception.toString());

                if (exception instanceof MsalClientException) {
                } else if (exception instanceof MsalServiceException) {
                }
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "User cancelled login.");
            }
        };
    }

    private void onSignOutClicked() {
        sampleApp.getAccounts(new PublicClientApplication.AccountsLoadedCallback() {
            @Override
            public void onAccountsLoaded(final List<IAccount> accounts) {

                if (accounts.isEmpty()) {
                } else {
                    for (final IAccount account : accounts) {
                        sampleApp.removeAccount(
                                account,
                                new PublicClientApplication.AccountsRemovedCallback() {
                                    @Override
                                    public void onAccountsRemoved(Boolean isSuccess) {
                                        if (isSuccess) {
                                            // successfully removed account
                                        } else {
                                            // failed to remove account
                                        }
                                    }
                                });
                    }
                }

                updateSignedOutUI();
            }
        });
    }

    private void callStorageAPI() {
        Log.d(TAG, "Starting OkHttp request to storage");

        if (authResult.getAccessToken() == null) {return;}

        DateTimeFormatter httpDateTimeFormatter = DateTimeFormatter
                .ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
                .withZone(ZoneId.of("UTC"))
                .withLocale(Locale.US);

        OkHttpClient client = new OkHttpClient.Builder()
                .build();

        okhttp3.Request okRequest = new okhttp3.Request.Builder()
                .url(STORAGE_URL)
                .get()
                .addHeader("Authorization", "Bearer " + authResult.getAccessToken())
                .addHeader("x-ms-version", "2019-02-02")
                .addHeader("Date", httpDateTimeFormatter.format(OffsetDateTime.now()))
                .addHeader("Accept", "*/*")
                .build();

        client.newCall(okRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.d(TAG, "Response: " + e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                Log.d(TAG, "Response: " + response.body().string());
            }
        });
    }

    private void updateGraphUI(JSONObject graphResponse) {
        TextView graphText = findViewById(R.id.graphData);
        graphText.setText(graphResponse.toString());
    }
}
