/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies.favoritemovies;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Content provider for favorite movies.
 */
public class FavoriteMoviesContentProvider extends ContentProvider {

    private static final int MATCHED_FAVORITE_MOVIES_LISTING = 1;
    private static final int MATCHED_FAVORITE_MOVIE_ENTRY = 2;

    private SQLiteOpenHelper mOpenHelper;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static UriMatcher buildUriMatcher() {
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        uriMatcher.addURI(
                FavoriteMoviesContract.AUTHORITY,
                FavoriteMoviesContract.PATH_FAVORITE_MOVIES,
                MATCHED_FAVORITE_MOVIES_LISTING);

        uriMatcher.addURI(
                FavoriteMoviesContract.AUTHORITY,
                FavoriteMoviesContract.PATH_FAVORITE_MOVIES + "/#",
                MATCHED_FAVORITE_MOVIE_ENTRY);

        return uriMatcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new FavoriteMoviesOpenHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    @SuppressWarnings("ConstantConditions")
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        if (sUriMatcher.match(uri) != MATCHED_FAVORITE_MOVIES_LISTING) {
            throw new UnsupportedOperationException("Invalid insert URI: " + uri);
        }

        long id = mOpenHelper.getWritableDatabase().insert(
                FavoriteMoviesContract.FavoriteMovieEntry.TABLE_NAME,
                null,
                values);
        Uri uriOfNewEntry = ContentUris.withAppendedId(uri, id);

        getContext().getContentResolver().notifyChange(uriOfNewEntry, null);
        return uriOfNewEntry;
    }

    @Nullable
    @Override
    @SuppressWarnings("ConstantConditions")
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        switch(sUriMatcher.match(uri)) {
            case MATCHED_FAVORITE_MOVIE_ENTRY:
                long id = ContentUris.parseId(uri);
                selection = FavoriteMoviesContract.FavoriteMovieEntry._ID + " = ?";
                selectionArgs = new String[] { String.valueOf(id) };
                break;
            case MATCHED_FAVORITE_MOVIES_LISTING:
                break;
            default:
                throw new UnsupportedOperationException("Invalid query URI: " + uri);
        }

        Cursor result = mOpenHelper.getReadableDatabase().query(
                FavoriteMoviesContract.FavoriteMovieEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);

        result.setNotificationUri(getContext().getContentResolver(), uri);
        return result;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        return 0;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        switch(sUriMatcher.match(uri)) {
            case MATCHED_FAVORITE_MOVIE_ENTRY:
                long id = ContentUris.parseId(uri);
                selection = FavoriteMoviesContract.FavoriteMovieEntry._ID + " = ?";
                selectionArgs = new String[] { String.valueOf(id) };
                break;
            case MATCHED_FAVORITE_MOVIES_LISTING:
                break;
            default:
                throw new UnsupportedOperationException("Invalid delete URI: " + uri);
        }

        int numberOfDeletedEntries = mOpenHelper.getWritableDatabase().delete(
                FavoriteMoviesContract.FavoriteMovieEntry.TABLE_NAME,
                selection,
                selectionArgs);

        getContext().getContentResolver().notifyChange(uri, null);
        return numberOfDeletedEntries;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

}
