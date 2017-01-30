/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies.tmdbapi.responseobjects;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Class for configuration data received from the API.
 */
@SuppressWarnings("unused")
public class Configuration {

    @SerializedName("images")
    @Expose
    private ImagesConfiguration mImagesConfiguration;

    @SerializedName("change_keys")
    @Expose
    private List<String> changeKeys = null;

    public ImagesConfiguration getImagesConfiguration() {
        return mImagesConfiguration;
    }

    public void setImagesConfiguration(ImagesConfiguration imagesConfiguration) {
        this.mImagesConfiguration = imagesConfiguration;
    }

    public List<String> getChangeKeys() {
        return changeKeys;
    }

    public void setChangeKeys(List<String> changeKeys) {
        this.changeKeys = changeKeys;
    }

}
