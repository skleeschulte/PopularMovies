/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies.tmdbapi.responseobjects;

import android.annotation.SuppressLint;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Class for movie details received in movie listings.
 */
@SuppressWarnings("unused")
public class MovieListingMovieDetails {

    /**
     * The format of the release date string.
     */
    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat RELEASE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @SerializedName("poster_path")
    @Expose
    private String mPosterPath;

    @SerializedName("adult")
    @Expose
    private Boolean mAdult;

    @SerializedName("overview")
    @Expose
    private String mOverview;

    @SerializedName("release_date")
    @Expose
    private String mReleaseDate;

    @SerializedName("genre_ids")
    @Expose
    private List<Integer> mGenreIds = null;

    @SerializedName("id")
    @Expose
    private Long mId;

    @SerializedName("original_title")
    @Expose
    private String mOriginalTitle;

    @SerializedName("original_language")
    @Expose
    private String mOriginalLanguage;

    @SerializedName("title")
    @Expose
    private String mTitle;

    @SerializedName("backdrop_path")
    @Expose
    private String mBackdropPath;

    @SerializedName("popularity")
    @Expose
    private Double mPopularity;

    @SerializedName("vote_count")
    @Expose
    private Integer mVoteCount;

    @SerializedName("video")
    @Expose
    private Boolean mVideo;

    @SerializedName("vote_average")
    @Expose
    private Double mVoteAverage = -1d;

    public String getPosterPath() {
        return mPosterPath;
    }

    public void setPosterPath(String posterPath) {
        this.mPosterPath = posterPath;
    }

    public Boolean getAdult() {
        return mAdult;
    }

    public void setAdult(Boolean adult) {
        this.mAdult = adult;
    }

    public String getOverview() {
        return mOverview;
    }

    public void setOverview(String overview) {
        this.mOverview = overview;
    }

    public String getReleaseDate() {
        return mReleaseDate;
    }

    /**
     * Returns the release date as java Date object.
     *
     * @return Date object from parsed release date String or null.
     */
    public Date getParsedReleaseDate() {
        try {
            return RELEASE_DATE_FORMAT.parse(mReleaseDate);
        } catch (ParseException e) {
            return null;
        }
    }

    public void setReleaseDate(String releaseDate) {
        this.mReleaseDate = releaseDate;
    }

    public List<Integer> getGenreIds() {
        return mGenreIds;
    }

    public void setGenreIds(List<Integer> genreIds) {
        this.mGenreIds = genreIds;
    }

    public Long getId() {
        return mId;
    }

    public void setId(Long id) {
        this.mId = id;
    }

    public String getOriginalTitle() {
        return mOriginalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.mOriginalTitle = originalTitle;
    }

    public String getOriginalLanguage() {
        return mOriginalLanguage;
    }

    public void setOriginalLanguage(String originalLanguage) {
        this.mOriginalLanguage = originalLanguage;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public String getBackdropPath() {
        return mBackdropPath;
    }

    public void setBackdropPath(String backdropPath) {
        this.mBackdropPath = backdropPath;
    }

    public Double getPopularity() {
        return mPopularity;
    }

    public void setPopularity(Double popularity) {
        this.mPopularity = popularity;
    }

    public Integer getVoteCount() {
        return mVoteCount;
    }

    public void setVoteCount(Integer voteCount) {
        this.mVoteCount = voteCount;
    }

    public Boolean getVideo() {
        return mVideo;
    }

    public void setVideo(Boolean video) {
        this.mVideo = video;
    }

    public Double getVoteAverage() {
        return mVoteAverage;
    }

    public void setVoteAverage(Double voteAverage) {
        this.mVoteAverage = voteAverage;
    }

}
