/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies.tmdbapi;

import biz.smk.popularmovies.tmdbapi.responseobjects.Configuration;
import biz.smk.popularmovies.tmdbapi.responseobjects.MovieListingPage;
import biz.smk.popularmovies.tmdbapi.responseobjects.MovieReviews;
import biz.smk.popularmovies.tmdbapi.responseobjects.MovieVideos;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Single;

/**
 * TMDb API v3 interface for Retrofit 2.
 *
 * If a path contains an api_key query parameter, the API key will automatically be filled in by an
 * interceptor.
 */
public interface TmdbApiClient {

    @GET("configuration?api_key=")
    Single<Configuration> getConfiguration();

    @GET("movie/top_rated?api_key=")
    Single<MovieListingPage> getTopRatedMovies(@Query("page") int pageNr);

    @GET("movie/popular?api_key=")
    Single<MovieListingPage> getPopularMovies(@Query("page") int pageNr);

    @GET("movie/{id}/videos?api_key=")
    Single<MovieVideos> getMovieVideos(@Path("id") long movieId);

    @GET("movie/{id}/reviews?api_key=")
    Single<MovieReviews> getMovieReviews(@Path("id") long movieId);

}
