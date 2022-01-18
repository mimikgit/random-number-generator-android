package com.mimik.randomnumbergen;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mimik.edgemobileclient.EdgeMobileClient;
import com.mimik.edgemobileclient.EdgeRequestError;
import com.mimik.edgemobileclient.EdgeRequestResponse;
import com.mimik.edgemobileclient.EdgeResponseHandler;
import com.mimik.edgemobileclient.authobject.CombinedAuthResponse;
import com.mimik.edgemobileclient.authobject.DeveloperTokenLoginConfig;
import com.mimik.edgemobileclient.edgeservice.EdgeConfig;
import com.mimik.edgemobileclient.microserviceobjects.MicroserviceDeploymentConfig;
import com.mimik.edgemobileclient.microserviceobjects.MicroserviceDeploymentStatus;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.Executors;


import java.util.Random;

public class MainActivity extends AppCompatActivity {
    EdgeMobileClient edgeMobileClient;
    String accessToken;
    String randomNumberRoot;
    Button getButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Instantiate new instance of edgeEngine runtime
        edgeMobileClient = new EdgeMobileClient(this, new EdgeConfig());

        // Start edge using a new thread so as not to slow down activity creation
        Executors.newSingleThreadExecutor().execute(this::startEdge);

        getButton = findViewById(R.id.btn_get);
        getButton.setOnClickListener(this::onGetClicked);
    }

    private void startEdge() {
        if (edgeMobileClient.startEdge()) { // Start edgeEngine runtime
            runOnUiThread(() -> {
                Toast.makeText(
                        MainActivity.this,
                        "edgeEngine started!",
                        Toast.LENGTH_LONG).show();
            });
            try {
                Thread.sleep(1000); // Wait for edge to finish startup
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            authorizeEdge();
        } else {
            runOnUiThread(() -> {
                Toast.makeText(
                        MainActivity.this,
                        "edgeEngine failed to start!",
                        Toast.LENGTH_LONG).show();
            });
        }
    }

    private void authorizeEdge() {
        // Get the DEVELOPER_ID_TOKEN from the BuildConfig settings
        String developerIdToken = BuildConfig.DEVELOPER_ID_TOKEN;

        String clientId = BuildConfig.CLIENT_ID; // The Client ID

        // Create mimik configuration object for Developer ID Token login
        DeveloperTokenLoginConfig config = new DeveloperTokenLoginConfig();

        // Set the root URL
        config.setAuthorizationRootUri(BuildConfig.AUTHORIZATION_ENDPOINT);

        // Set the value for the DEVELOPER_ID_TOKEN
        config.setDeveloperToken(developerIdToken);

        // Set the value for the CLIENT_ID
        config.setClientId(clientId);

        // Login to the edgeCloud
        edgeMobileClient.loginWithDeveloperToken(
                this,
                config,
                new EdgeResponseHandler() {
                    @Override
                    public void onError(EdgeRequestError edgeRequestError) {
                        // Display error message
                        runOnUiThread(() -> {
                            Toast.makeText(
                                    MainActivity.this,
                                    "Error getting access token! " + edgeRequestError.getErrorMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
                    }

                    // A valid return makes the Access Token available by way of
                    // the method, edgeMobileClient.getCombinedAccessTokens()
                    @Override
                    public void onResponse(EdgeRequestResponse edgeRequestResponse) {

                        // Get all the token that are stored within the
                        // edgeMobileClient
                        CombinedAuthResponse tokens = edgeMobileClient.getCombinedAccessTokens();

                        // Extract the Access Token from the tokens object and assign
                        // it to the class variable, accessToken
                        accessToken = tokens.getMimikTokens().getAccessToken();
                        runOnUiThread(() -> {
                            Toast.makeText(
                                    MainActivity.this,
                                    "Got access token!",
                                    Toast.LENGTH_LONG).show();
                        });

                        // Deploy edge microservice now that an access token
                        // has been generated
                        deployRandomNumberMicroservice();
                    }
                }
        );
    }

    private void deployRandomNumberMicroservice() {

        // Create microservice deployment configuration, dependent
        // on microservice implementation
        MicroserviceDeploymentConfig config = new MicroserviceDeploymentConfig();

        // set the name that will represent the microservice
        config.setName("randomnumber-v1");

        // Get the tar file that represents the edge microservice
        //but stored in the project's file system as a Gradle resource
        config.setResourceStream(getResources().openRawResource(R.raw.randomnumber_v1));

        // Set the filename that by which the edge client will identify
        // the microservice internally. This filename is associated internally
        // with the resource stream initialized above
        config.setFilename("randomnumber_v1.tar");

        // Declare the URI by  which the application code will access
        // the microservice
        config.setApiRootUri(Uri.parse("/randomnumber/v1"));

        // Deploy edge microservice using the client library instance variable
        MicroserviceDeploymentStatus status =
                edgeMobileClient.deployEdgeMicroservice(accessToken, config);
        if (status.error != null) {
            // Display microservice deployment error
            runOnUiThread(() -> {
                    Toast.makeText(
                            MainActivity.this,
                            "Failed to deploy microservice! " + status.error.getMessage(),
                            Toast.LENGTH_LONG).show();
       });
        } else {
            // Store the microservice API root URI in the class variable,
            // randomNumberRoot
            randomNumberRoot = status.response.getContainer().getApiRootUri().toString();
            // Display a message indicating a successful microservice deployment
            runOnUiThread(() -> {
                    Toast.makeText(
                            MainActivity.this,
                            "Successfully deployed microservice!",
                            Toast.LENGTH_LONG).show();
                    getButton.setEnabled(true);
           });
        }
    }

    private void onGetClicked(View view) {
        if (randomNumberRoot == null || edgeMobileClient.getEdgePort() == -1) {
            Toast.makeText(
                    MainActivity.this,
                    "Edge is not ready yet!",
                    Toast.LENGTH_LONG).show();
            return;
        }
        // Construct an API request for the edge microservice
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(String.format(
                        "http://127.0.0.1:%d%s/randomNumber",
                        // use the client to get the default localhost port
                        edgeMobileClient.getEdgePort(),
                        randomNumberRoot)) // root URI determined by microservice deployment
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(
                    @NotNull Call call,
                    @NotNull IOException e) {
                // Display microservice request error
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(
                            MainActivity.this,
                            "Failed to communicate with microservice! " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(
                    @NotNull Call call,
                    @NotNull final Response response) throws IOException {
                if (!response.isSuccessful()) {
                    // Display microservice unknown error
                    runOnUiThread(() -> {
                        Toast.makeText(
                                MainActivity.this,
                                "Microservice returned unexpected code! " + response,
                                Toast.LENGTH_LONG).show();
                    });
                } else {
                    // Display microservice response
                    runOnUiThread(() -> {
                        try {
                            Toast.makeText(
                                    MainActivity.this,
                                    "Got " + response.body().string(),
                                    Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        });
    }
}