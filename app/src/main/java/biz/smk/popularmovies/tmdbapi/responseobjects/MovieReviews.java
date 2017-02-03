/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies.tmdbapi.responseobjects;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Class for movie reviews list received from the API.
 */
@SuppressWarnings("unused")
public class MovieReviews {

    @SerializedName("id")
    @Expose
    private Integer mId;

    @SerializedName("page")
    @Expose
    private Integer mPage;

    @SerializedName("results")
    @Expose
    private List<MovieReviewDetails> mMovieReviewDetails = null;

    @SerializedName("total_pages")
    @Expose
    private Integer mTotalPages;

    @SerializedName("total_results")
    @Expose
    private Integer mTotalResults;

    public Integer getId() {
        return mId;
    }

    public void setId(Integer id) {
        this.mId = id;
    }

    public Integer getPage() {
        return mPage;
    }

    public void setPage(Integer page) {
        this.mPage = page;
    }

    public List<MovieReviewDetails> getMovieReviewDetails() {
        return mMovieReviewDetails;
    }

    public void setMovieReviewDetails(List<MovieReviewDetails> movieReviewDetails) {
        this.mMovieReviewDetails = movieReviewDetails;
    }

    public Integer getTotalPages() {
        return mTotalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.mTotalPages = totalPages;
    }

    public Integer getTotalResults() {
        return mTotalResults;
    }

    public void setTotalResults(Integer totalResults) {
        this.mTotalResults = totalResults;
    }

}
