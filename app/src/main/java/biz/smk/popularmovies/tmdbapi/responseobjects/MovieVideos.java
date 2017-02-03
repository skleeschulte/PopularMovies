/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies.tmdbapi.responseobjects;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Class for movie videos list received from the API.
 */
@SuppressWarnings("unused")
public class MovieVideos {

    @SerializedName("id")
    @Expose
    private Integer mId;

    @SerializedName("results")
    @Expose
    private List<MovieVideoDetails> mMovieVideoDetails = null;

    public Integer getId() {
        return mId;
    }

    public void setId(Integer id) {
        this.mId = id;
    }

    public List<MovieVideoDetails> getMovieVideoDetails() {
        return mMovieVideoDetails;
    }

    public void setResults(List<MovieVideoDetails> movieVideoDetails) {
        this.mMovieVideoDetails = movieVideoDetails;
    }

}
