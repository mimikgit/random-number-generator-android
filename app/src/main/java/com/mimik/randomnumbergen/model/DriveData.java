package com.mimik.randomnumbergen.model;

import com.google.gson.annotations.SerializedName;

/**
 * Drive data class
 */
public class DriveData {
    /**
     * Drive device ID
     */
    @SerializedName("id")
    public String id;

    /**
     * Account ID associated with the drive
     */
    @SerializedName("accountId")
    public String accountId;

    /**
     * Drive device name
     */
    @SerializedName("name")
    public String name;

    /**
     * URL at which the drive is accessible
     */
    @SerializedName("url")
    public String url = "";

    /**
     * Operating system of the drive device
     */
    @SerializedName("os")
    public String os;
}