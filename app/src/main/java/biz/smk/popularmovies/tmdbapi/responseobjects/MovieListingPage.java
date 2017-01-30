/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies.tmdbapi.responseobjects;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Class for movie listing pages received from the API.
 */
@SuppressWarnings("unused")
public class MovieListingPage {

    @SerializedName("page")
    @Expose
    private Integer mPage;

    @SerializedName("results")
    @Expose
    private List<MovieListingMovieDetails> mMovieListingMovieDetails = null;

    @SerializedName("total_results")
    @Expose
    private Integer mTotalResults;

    @SerializedName("total_pages")
    @Expose
    private Integer mTotalPages;

    public Integer getPage() {
        return mPage;
    }

    public void setPage(Integer page) {
        this.mPage = page;
    }

    public List<MovieListingMovieDetails> getResults() {
        return mMovieListingMovieDetails;
    }

    public void setResults(List<MovieListingMovieDetails> movieListingMovieDetails) {
        this.mMovieListingMovieDetails = movieListingMovieDetails;
    }

    public Integer getTotalResults() {
        return mTotalResults;
    }

    public void setTotalResults(Integer totalResults) {
        this.mTotalResults = totalResults;
    }

    public Integer getTotalPages() {
        return mTotalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.mTotalPages = totalPages;
    }

}
