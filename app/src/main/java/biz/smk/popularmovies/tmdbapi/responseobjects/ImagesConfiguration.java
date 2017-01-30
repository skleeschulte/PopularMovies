/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies.tmdbapi.responseobjects;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Class for image configuration data received in the configuration data.
 */
@SuppressWarnings("unused")
public class ImagesConfiguration {

    @SerializedName("base_url")
    @Expose
    private String mBaseUrl;

    @SerializedName("secure_base_url")
    @Expose
    private String mSecureBaseUrl;

    @SerializedName("backdrop_sizes")
    @Expose
    private List<String> mBackdropSizes = null;

    @SerializedName("logo_sizes")
    @Expose
    private List<String> mLogoSizes = null;

    @SerializedName("poster_sizes")
    @Expose
    private List<String> mPosterSizes = null;

    @SerializedName("profile_sizes")
    @Expose
    private List<String> mProfileSizes = null;

    @SerializedName("still_sizes")
    @Expose
    private List<String> mStillSizes = null;

    public String getBaseUrl() {
        return mBaseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.mBaseUrl = baseUrl;
    }

    public String getSecureBaseUrl() {
        return mSecureBaseUrl;
    }

    public void setSecureBaseUrl(String secureBaseUrl) {
        this.mSecureBaseUrl = secureBaseUrl;
    }

    public List<String> getBackdropSizes() {
        return mBackdropSizes;
    }

    public void setBackdropSizes(List<String> backdropSizes) {
        this.mBackdropSizes = backdropSizes;
    }

    public List<String> getLogoSizes() {
        return mLogoSizes;
    }

    public void setLogoSizes(List<String> logoSizes) {
        this.mLogoSizes = logoSizes;
    }

    public List<String> getPosterSizes() {
        return mPosterSizes;
    }

    public void setPosterSizes(List<String> posterSizes) {
        this.mPosterSizes = posterSizes;
    }

    public List<String> getProfileSizes() {
        return mProfileSizes;
    }

    public void setProfileSizes(List<String> profileSizes) {
        this.mProfileSizes = profileSizes;
    }

    public List<String> getStillSizes() {
        return mStillSizes;
    }

    public void setStillSizes(List<String> stillSizes) {
        this.mStillSizes = stillSizes;
    }

}
