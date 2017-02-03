/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies.tmdbapi.responseobjects;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Class for movie video details received from the API.
 */
@SuppressWarnings("unused")
public class MovieVideoDetails {

    /**
     * Type string returned from the API for trailers.
     */
    private static final String TYPE_TRAILER = "Trailer";

    /**
     * Site string returned from the API for videos hosted on YouTube.
     */
    private static final String SITE_YOUTUBE = "YouTube";

    @SerializedName("id")
    @Expose
    private String mId;

    @SerializedName("iso_639_1")
    @Expose
    private String mIso6391;

    @SerializedName("iso_3166_1")
    @Expose
    private String mIso31661;

    @SerializedName("key")
    @Expose
    private String mKey;

    @SerializedName("name")
    @Expose
    private String mName;

    @SerializedName("site")
    @Expose
    private String mSite;

    @SerializedName("size")
    @Expose
    private Integer mSize;

    @SerializedName("type")
    @Expose
    private String mType;

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        this.mId = id;
    }

    public String getIso6391() {
        return mIso6391;
    }

    public void setIso6391(String iso6391) {
        this.mIso6391 = iso6391;
    }

    public String getIso31661() {
        return mIso31661;
    }

    public void setIso31661(String iso31661) {
        this.mIso31661 = iso31661;
    }

    public String getKey() {
        return mKey;
    }

    public void setKey(String key) {
        this.mKey = key;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getSite() {
        return mSite;
    }

    public void setSite(String site) {
        this.mSite = site;
    }

    public Integer getSize() {
        return mSize;
    }

    public void setSize(Integer size) {
        this.mSize = size;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        this.mType = type;
    }

    public boolean isTrailer() {
        return TYPE_TRAILER.equalsIgnoreCase(mType);
    }

    public boolean isHostedOnYouTube() {
        return SITE_YOUTUBE.equalsIgnoreCase(mSite);
    }

}
