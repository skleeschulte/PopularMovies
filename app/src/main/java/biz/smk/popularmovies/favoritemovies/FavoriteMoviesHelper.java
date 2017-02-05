/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies.favoritemovies;

import android.annotation.SuppressLint;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import biz.smk.popularmovies.tmdbapi.responseobjects.MovieListingMovieDetails;

/**
 * Helper functions for favorite movies.
 */
public class FavoriteMoviesHelper {

    /**
     * Add a movie to the favorite movies database.
     *
     * @param details The movie details of the movie to add.
     * @param queryHandler The query handler for the insert (onInsertCompleted will be called).
     */
    public static void addFavoriteMovie(final MovieListingMovieDetails details,
                                                 final AsyncQueryHandler queryHandler) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    return new Gson().toJson(details, MovieListingMovieDetails.class);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                if (s != null) {
                    long movieId = details.getId();

                    ContentValues values = new ContentValues();
                    values.put(FavoriteMoviesContract.FavoriteMovieEntry.COLUMN_MOVIE_ID, movieId);
                    values.put(FavoriteMoviesContract.FavoriteMovieEntry.COLUMN_MOVIE_DETAILS, s);

                    queryHandler.startInsert(0, null,
                            FavoriteMoviesContract.FavoriteMovieEntry.CONTENT_URI,
                            values);
                }
            }
        }.execute();
    }

    /**
     * Gets all favorite movies from the database.
     *
     * @param contentResolver A ContentResolver.
     * @param callback A Callback that will receive the favorite movies.
     */
    @SuppressLint("HandlerLeak")
    public static void getMovies(ContentResolver contentResolver,
                                 final GetMoviesCallback callback) {
        new AsyncQueryHandler(contentResolver) {
            @Override
            protected void onQueryComplete(int token, Object cookie, final Cursor cursor) {
                if (cursor == null) return;

                new AsyncTask<Void, Void, List<MovieListingMovieDetails>>() {
                    @Override
                    protected List<MovieListingMovieDetails> doInBackground(Void... params) {
                        List<MovieListingMovieDetails> movieList = new ArrayList<>();

                        while (cursor.moveToNext()) {
                            try {
                                String json = cursor.getString(0);
                                movieList.add(
                                        new Gson().fromJson(json, MovieListingMovieDetails.class));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        return movieList;
                    }

                    @Override
                    protected void onPostExecute(
                            List<MovieListingMovieDetails> movieListingMovieDetails) {
                        super.onPostExecute(movieListingMovieDetails);
                        callback.onMoviesReceived(movieListingMovieDetails);
                    }
                }.execute();
            }
        }.startQuery(0, null,
                FavoriteMoviesContract.FavoriteMovieEntry.CONTENT_URI,
                new String[] { FavoriteMoviesContract.FavoriteMovieEntry.COLUMN_MOVIE_DETAILS },
                null,
                null,
                null);
    }

    /**
     * Removes a movie from the favorite movies database.
     *
     * @param movieId The movie ID of the movie which shall be removed.
     * @param queryHandler The query handler for the delete (onDeleteComplete will be called).
     */
    public static void removeFavoriteMovie(long movieId, AsyncQueryHandler queryHandler) {
        queryHandler.startDelete(0, null,
                FavoriteMoviesContract.FavoriteMovieEntry.CONTENT_URI,
                FavoriteMoviesContract.FavoriteMovieEntry.COLUMN_MOVIE_ID + " = ?",
                new String[]{String.valueOf(movieId)});
    }

    /**
     * Interface for callbacks to receive the favorite movies from the database.
     */
    public interface GetMoviesCallback {
        void onMoviesReceived(List<MovieListingMovieDetails> movieListingMovieDetails);
    }

}
