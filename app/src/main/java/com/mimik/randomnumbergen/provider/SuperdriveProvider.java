package com.mimik.randomnumbergen.provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mimik.randomnumbergen.api.SuperdriveApi;
import com.mimik.randomnumbergen.model.DriveData;
import com.mimik.randomnumbergen.model.Drives;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit2 wrapper class for {@link SuperdriveApi}
 */
public class SuperdriveProvider {
    private static final String TAG = "SuperdriveProvider";
    private static final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();
    private final String mEdgeAccessToken;
    private final String mSuperdriveUrl;

    private static SuperdriveProvider mInstance;

    private SuperdriveProvider(
            String edgeAccessToken,
            String superdriveUrl) {
        this.mEdgeAccessToken = edgeAccessToken;
        this.mSuperdriveUrl = superdriveUrl;

    }

    public static SuperdriveProvider instance(
            String edgeAccessToken,
            String superdriveUrl) {
        if (mInstance == null
                || !Objects.equals(edgeAccessToken, mInstance.mEdgeAccessToken)) {
            mInstance = new
                    SuperdriveProvider(edgeAccessToken, superdriveUrl);
        }
        return mInstance;
    }

    private SuperdriveApi localService() {
        final HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        final OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request newRequest = chain.request().newBuilder()
                            .addHeader("Authorization", "Bearer " + mEdgeAccessToken)
                            .build();
                    return chain.proceed(newRequest);
                })
                .addInterceptor(logging)
                .build();

        String url = mSuperdriveUrl;
        if (!url.endsWith("/")) {
            url = url + "/";
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build();

        return retrofit.create(SuperdriveApi.class);
    }

    public Call<Drives> getDrives(String type, String backendAccessToken) {
        return localService().getDrives(type, backendAccessToken);
    }

    public Call<DriveData> establishTunnel(String nodeId, String backendAccessToken) {
        return localService().establishTunnel(nodeId, backendAccessToken);
    }
}

