/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies.favoritemovies;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Open helper for favorite movies DB.
 */
class FavoriteMoviesOpenHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "favorite-movies.db";
    private static final int DB_VERSION = 1;

    FavoriteMoviesOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + FavoriteMoviesContract.FavoriteMovieEntry.TABLE_NAME + " (" +
                FavoriteMoviesContract.FavoriteMovieEntry._ID + " INTEGER PRIMARY KEY, " +
                FavoriteMoviesContract.FavoriteMovieEntry.COLUMN_MOVIE_ID +
                " INTEGER UNIQUE NOT NULL, " +
                FavoriteMoviesContract.FavoriteMovieEntry.COLUMN_MOVIE_DETAILS + " TEXT NOT NULL);";

        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > 1) throw new IllegalStateException("database upgrade must be implemented");
    }

}
