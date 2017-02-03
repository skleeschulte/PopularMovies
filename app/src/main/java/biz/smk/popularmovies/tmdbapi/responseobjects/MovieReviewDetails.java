/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies.tmdbapi.responseobjects;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Class for movie review details received from the API.
 */
@SuppressWarnings("unused")
public class MovieReviewDetails {

    @SerializedName("id")
    @Expose
    private String mId;

    @SerializedName("author")
    @Expose
    private String mAuthor;

    @SerializedName("content")
    @Expose
    private String mContent;

    @SerializedName("url")
    @Expose
    private String mUrl;

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        this.mId = id;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public void setAuthor(String author) {
        this.mAuthor = author;
    }

    public String getContent() {
        return mContent;
    }

    public void setContent(String content) {
        this.mContent = content;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }

}
