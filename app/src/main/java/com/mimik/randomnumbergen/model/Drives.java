package com.mimik.randomnumbergen.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * A list of drive devices, and the type of cluster they were discovered with
 */
public class Drives {
    /**
     * Cluster type
     */
    @SerializedName("type")
    public String type;

    /**
     * List of drive devices
     */
    @SerializedName("data")
    public List<DriveData> data;
}
