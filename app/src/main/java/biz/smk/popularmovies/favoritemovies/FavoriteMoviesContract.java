/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies.favoritemovies;

import android.net.Uri;
import android.provider.BaseColumns;

import biz.smk.popularmovies.Application;
import biz.smk.popularmovies.R;

/**
 * Database / content provider contract for favorite movies.
 */
@SuppressWarnings("WeakerAccess")
public class FavoriteMoviesContract {

    public static final String AUTHORITY =
            Application.getContext().getString(R.string.favorite_movies_content_provider_authority);

    public static final Uri CONTENT_BASE_URI = Uri.parse("content://" + AUTHORITY);

    public static final String PATH_FAVORITE_MOVIES = "favorite-movies";

    public static final class FavoriteMovieEntry implements BaseColumns {

        public static final Uri CONTENT_URI = CONTENT_BASE_URI.buildUpon()
                .appendPath(PATH_FAVORITE_MOVIES).build();

        public static final String TABLE_NAME = "favoriteMovies";

        public static final String COLUMN_MOVIE_ID = "movieId";
        public static final String COLUMN_MOVIE_DETAILS = "details";

    }

}
