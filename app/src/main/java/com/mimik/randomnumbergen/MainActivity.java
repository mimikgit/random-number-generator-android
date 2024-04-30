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
import com.mimik.randomnumbergen.model.DriveData;
import com.mimik.randomnumbergen.model.Drives;
import com.mimik.randomnumbergen.provider.SuperdriveProvider;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    EdgeMobileClient edgeMobileClient;
    String nodeId;
    String accessToken;
    String randomNumberRoot;
    String superdriveRoot;
    Button getButton;
    Button superdriveButton;

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

        superdriveButton = findViewById(R.id.btn_superdrive);
        superdriveButton.setOnClickListener(this::onSuperdriveClicked);
    }

    private void startEdge() {
        if (edgeMobileClient.startEdgeSynchronously()) { // Start edgeEngine runtime
            runOnUiThread(() -> Toast.makeText(
                    MainActivity.this,
                    "edgeEngine started!",
                    Toast.LENGTH_LONG).show());
            authorizeEdge();
        } else {
            runOnUiThread(() -> Toast.makeText(
                    MainActivity.this,
                    "edgeEngine failed to start!",
                    Toast.LENGTH_LONG).show());
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
                        runOnUiThread(() -> Toast.makeText(
                                MainActivity.this,
                                "Error getting access token! "
                                        + edgeRequestError.getErrorMessage(),
                                Toast.LENGTH_LONG).show());
                    }

                    // A valid return makes the Access Token available by way of
                    // the method, edgeMobileClient.getCombinedAccessTokens()
                    @Override
                    public void onResponse(EdgeRequestResponse edgeRequestResponse) {

                        // Get all the token that are stored within the
                        // edgeMobileClient
                        CombinedAuthResponse tokens
                                = edgeMobileClient.getCombinedAccessTokens();

                        // Extract the Access Token from the tokens object and assign
                        // it to the class variable, accessToken
                        accessToken = tokens.getMimikTokens().getAccessToken();
                        runOnUiThread(() -> Toast.makeText(
                                MainActivity.this,
                                "Got access token!",
                                Toast.LENGTH_LONG).show());

                        nodeId = edgeMobileClient.getInfo().getNodeId();

                        // Deploy edge microservices now that an access token
                        // has been generated
                        deployRandomNumberMicroservice();
                        deploySuperdriveMicroservice();
                    }
                }
        );
    }

    // Deploy the random number edge microservice, used for generating random numbers
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
            runOnUiThread(() -> Toast.makeText(
                    MainActivity.this,
                    "Failed to deploy microservice! "
                            + status.error.getMessage(),
                    Toast.LENGTH_LONG).show());
        } else {
            // Store the microservice API root URI in the class variable,
            // randomNumberRoot
            randomNumberRoot =
                    status.response.getContainer().getApiRootUri().toString();
            // Display a message indicating a successful microservice deployment
            runOnUiThread(() -> {
                Toast.makeText(
                        MainActivity.this,
                        "Successfully deployed random number microservice!",
                        Toast.LENGTH_LONG).show();
                getButton.setEnabled(true);
            });
        }
    }

    // Deploy the superdrive edge microservice, used for detecting other edge devices
    // with superdrive deployed
    private void deploySuperdriveMicroservice() {

        // Create microservice deployment configuration, dependent
        // on microservice implementation
        MicroserviceDeploymentConfig config = new MicroserviceDeploymentConfig();

        // set the name that will represent the microservice
        config.setName("superdrive-v1");

        // Get the tar file that represents the edge microservice
        //but stored in the project's file system as a Gradle resource
        config.setResourceStream(getResources().openRawResource(
                R.raw.superdrive_v1_2_2_0));

        // Set the filename that by which the edge client will identify
        // the microservice internally. This filename is associated internally
        // with the resource stream initialized above
        config.setFilename("superdrive_v1_2_2_0.tar");

        // Declare the URI by  which the application code will access
        // the microservice
        config.setApiRootUri(Uri.parse("/superdrive/v1"));

        // Declare microservice-specific environment variables
        Map<String, String> env = new HashMap<>();
        env.put("MCM.WEBSOCKET_SUPPORT", "false");
        // Use edge local mDS service
        env.put("uMDS", String.format(
                Locale.getDefault(),
                "http://127.0.0.1:%d/mds/v1",
                edgeMobileClient.getEdgePort()));
        config.setEnvVariables(env);

        // Deploy edge microservice using the client library instance variable
        MicroserviceDeploymentStatus status =
                edgeMobileClient.deployEdgeMicroservice(accessToken, config);
        if (status.error != null) {
            // Display microservice deployment error
            runOnUiThread(() -> Toast.makeText(
                    MainActivity.this,
                    "Failed to deploy microservice! "
                            + status.error.getMessage(),
                    Toast.LENGTH_LONG).show());
        } else {
            // Store the microservice API root URI in the class variable,
            // superdriveRoot
            superdriveRoot = status.response.getContainer().getApiRootUri().toString();
            // Display a message indicating a successful microservice deployment
            runOnUiThread(() -> {
                Toast.makeText(
                        MainActivity.this,
                        "Successfully deployed superdrive microservice!",
                        Toast.LENGTH_LONG).show();
                superdriveButton.setEnabled(true);
            });
        }
    }

    // Query superdrive microservice for other edge devices with superdrive
    // deployed
    private void onSuperdriveClicked(View ignoredView) {
        // Format the local superdrive microservice URL
        String superdriveUrl = String.format(
                Locale.getDefault(),
                "http://127.0.0.1:%d%s/",
                // use the client to get the default localhost port
                edgeMobileClient.getEdgePort(),
                superdriveRoot);

        // Run network calls on a different thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Request other edge devices that have deployed superdrive
                // using the account cluster tied to the Developer ID Token
                retrofit2.Response<Drives> response =
                        SuperdriveProvider.instance(accessToken, superdriveUrl)
                                .getDrives("account", "null")
                                .execute();
                if (response.isSuccessful()) {
                    Drives drives = response.body();

                    // Find the first other device that isn't our own device
                    DriveData device = drives.data.stream()
                            .filter(x -> !nodeId.equals(x.id))
                            .findFirst().orElse(null);
                    if (device == null) {
                        runOnUiThread(() -> Toast.makeText(
                                MainActivity.this,
                                "Unable to find another device!",
                                Toast.LENGTH_LONG).show());
                    } else {
                        runOnUiThread(() -> Toast.makeText(
                                MainActivity.this,
                                "Found other devices! Using first one!",
                                Toast.LENGTH_LONG).show());
                        // Connect to the other device
                        connectToDevice(device);
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(
                            MainActivity.this,
                            "Superdrive query failed! " + response.message(),
                            Toast.LENGTH_LONG).show());
                }
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(
                        MainActivity.this,
                        "Superdrive query exception! " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private void connectToDevice(DriveData device) {
        // Format the local superdrive microservice URL
        String superdriveUrl = String.format(
                Locale.getDefault(),
                "http://127.0.0.1:%d%s/",
                // use the client to get the default localhost port
                edgeMobileClient.getEdgePort(),
                superdriveRoot);

        try {
            // Request a connection to an edge device, establishing a tunnel
            // URL if necessary
            retrofit2.Response<DriveData> response =
                    SuperdriveProvider.instance(accessToken, superdriveUrl)
                            .establishTunnel(device.id, "null")
                            .execute();
            if (response.isSuccessful()) {
                DriveData data = response.body();
                if (data.url.isEmpty()) {
                    // Device URL doesn't exist, couldn't establish a connection
                    runOnUiThread(() -> Toast.makeText(
                            MainActivity.this,
                            "Unable to connect to other device!",
                            Toast.LENGTH_LONG).show());
                } else {
                    // Device URL exists or was populated. We can use it to
                    // access the device
                    runOnUiThread(() -> Toast.makeText(
                            MainActivity.this,
                            String.format(
                                    "Connected to other device with url %s!",
                                    data.url),
                            Toast.LENGTH_LONG).show());
                    // Try getting a random number from the device. This will
                    // only succeed if the device has also deployed the random
                    // number microservice
                    requestRandomNumber(String.format(
                            "%s%s/randomNumber",
                            data.url,
                            randomNumberRoot));
                }
            } else {
                runOnUiThread(() -> Toast.makeText(
                        MainActivity.this,
                        "Superdrive query failed! " + response.message(),
                        Toast.LENGTH_LONG).show());
            }
        } catch (IOException e) {
            runOnUiThread(() -> Toast.makeText(
                    MainActivity.this,
                    "Superdrive query failed! " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
        }
    }

    private void onGetClicked(View ignoredView) {
        if (randomNumberRoot == null || edgeMobileClient.getEdgePort() == -1) {
            Toast.makeText(
                    MainActivity.this,
                    "Edge is not ready yet!",
                    Toast.LENGTH_LONG).show();
            return;
        }
        // Request a random number from the local random number microservice
        requestRandomNumber(String.format(
                Locale.getDefault(),
                "http://127.0.0.1:%d%s/randomNumber",
                // use the client to get the default localhost port
                edgeMobileClient.getEdgePort(),
                randomNumberRoot));
    }

    // Request a random number from a given URL
    private void requestRandomNumber(String url) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(
                    @NotNull Call call,
                    @NotNull IOException e) {
                // Display microservice request error
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(
                        MainActivity.this,
                        "Failed to communicate with microservice! "
                                + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(
                    @NotNull Call call,
                    @NotNull final Response response) {
                if (!response.isSuccessful()) {
                    // Display microservice unknown error
                    runOnUiThread(() -> Toast.makeText(
                            MainActivity.this,
                            "Microservice returned unexpected code! "
                                    + response,
                            Toast.LENGTH_LONG).show());
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