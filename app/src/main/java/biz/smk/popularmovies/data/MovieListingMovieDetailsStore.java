/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies.data;

import android.annotation.SuppressLint;

import java.util.HashMap;

import biz.smk.popularmovies.tmdbapi.responseobjects.MovieListingMovieDetails;

/**
 * Provides static methods for storing and retrieving movie details from movie listings.
 */
public class MovieListingMovieDetailsStore {

    @SuppressLint("UseSparseArrays")
    private static HashMap<Long, MovieListingMovieDetails> mStore = new HashMap<>();

    /**
     * Add movie details to the store. Uses the id field of the given movie details. If an entry
     * with the same id already exists, it will be updated.
     *
     * @param movieDetails The movie details to add.
     */
    static synchronized void addMovieDetails(MovieListingMovieDetails movieDetails) {
        Long movieId = movieDetails.getId();
        mStore.put(movieId, movieDetails);
    }

    /**
     * Returns the movie details for the given movie id.
     *
     * @param movieId The movie id.
     * @return The movie details.
     * @throws IllegalArgumentException If no entry with the given id exists.
     */
    public static synchronized MovieListingMovieDetails getMovieDetails(long movieId)
            throws IllegalArgumentException {
        MovieListingMovieDetails movieDetails = mStore.get(movieId);

        if (movieDetails == null) {
            throw new IllegalArgumentException("Unknown movieId: " + movieId);
        }

        return movieDetails;
    }

}
