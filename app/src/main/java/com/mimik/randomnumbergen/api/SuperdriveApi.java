package com.mimik.randomnumbergen.api;

import com.mimik.randomnumbergen.model.DriveData;
import com.mimik.randomnumbergen.model.Drives;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Superdrive Microservice API definition
 */
public interface SuperdriveApi {
    /**
     * Get a list of drive devices (edge nodes) by cluster type
     * @param clusters The type of cluster to be used. Either 'nearby' for
     *                 devices on the local network, or 'account' for other
     *                 devices that are using the same Developer ID Token,
     *                 or 'friends' for friend devices if using a mimik backend
     *                 with mFD enabled.
     * @param userToken A mimik backend access token for use with the 'friends'
     *                  cluster
     * @return A {@link Call} object that can be executed to perform the
     * request
     */
    @GET("drives")
    Call<Drives> getDrives(@Query("type") String clusters, @Query("userAccessToken") String userToken);

    /**
     * Establish a tunnel to a given drive device (edge node) if necessary (for
     * example, if the device is not accessible on the local network, and
     * return the drive device's data
     * @param id The id of the drive device
     * @param userToken A mimik backend access token for use with the 'friends'
     *                  cluster
     * @return A {@link Call} object that can be executed to perform the
     * request
     */
    @GET("nodes/{id}")
    Call<DriveData> establishTunnel(@Path("id") String id, @Query("userAccessToken") String userToken);
}
