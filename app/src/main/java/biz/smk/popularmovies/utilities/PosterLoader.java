/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies.utilities;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import biz.smk.popularmovies.tmdbapi.TmdbApiConfiguration;
import rx.Single;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Class for loading TMDb movie posters into ImageViews.
 */
public class PosterLoader {

    private static final String TAG = "PosterLoader";

    /**
     * Holds, for each poster path, all full (width dependent) URIs of poster images which already
     * have been successfully downloaded by Picasso (and thus cached).
     * The key is the poster path, the value is a TreeSet of PosterUri objects. A TreeSet is
     * naturally sorted. PosterUri implements natural sort order (with compareTo) so that the URLs
     * are sorted by increasing width.
     */
    private static final Map<String, TreeSet<PosterUri>> sCachedPostersUris = new HashMap<>();

    private final Context mContext;
    private final String mPosterPath;
    private final int mPosterWidthPx;
    private final ImageView mPosterView;
    private final Callback mCallback;

    private Subscription mPosterUriSubscription;
    private Date mStartDate;

    /**
     * Initializes a new PosterLoader for loading a poster into an ImageView with Picasso.
     *
     * @param context Android context for Picasso.with(context).
     * @param posterPath Poster path, as returned from TMDb API in the movie details (without base
     *                   URL).
     * @param posterWidthPx The desired poster width. The loaded poster image will have a width
     *                      equal to or bigger than this (if a sufficiently large image is
     *                      available, otherwise the largest available image will be downloaded).
     * @param posterView The image view into which the poster shall be loaded by Picasso.
     * @param callback Callback with success and error methods.
     */
    public PosterLoader(Context context, String posterPath, int posterWidthPx, ImageView posterView,
                        Callback callback) {
        mContext = context;
        mPosterPath = posterPath;
        mPosterWidthPx = posterWidthPx;
        mPosterView = posterView;
        mCallback = callback;
    }

    /**
     * Cancels all pending operations of this PosterLoader.
     */
    public void cancel() {
        if (mPosterUriSubscription != null) {
            mPosterUriSubscription.unsubscribe();
        }

        Picasso.with(mContext).cancelTag(this);
    }

    /**
     * Loads the poster URI and then with Picasso the poster into the ImageView. If a poster with a
     * width >= posterWidthPx has already been successfully downloaded (and thus cached) by Picasso,
     * it will be used. Otherwise the smallest available poster image with a width >= posterWidthPx
     * will be (down)loaded.
     */
    public void loadPoster() {
        // Callback for successful retrieval of the PosterUri.
        final Action1<PosterUri> onGetPosterUriSuccess = new Action1<PosterUri>() {
            @Override
            public void call(final PosterUri posterUri) {
                Log.v(TAG, prepLogMsg("loadPoster: got poster URI: " + posterUri.uri + "(width=" +
                        posterUri.width + ")"));

                Picasso.with(mContext)
                        .load(posterUri.uri)
                        .fit()
                        .tag(PosterLoader.this)
                        .into(mPosterView, new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {
                                Log.v(TAG, prepLogMsg("loadPoster: picasso successfully loaded " +
                                        "poster into view"));

                                addPosterUriToCache(mPosterPath, posterUri, PosterLoader.this);
                                mCallback.onSuccess();
                            }

                            @Override
                            public void onError() {
                                Log.v(TAG, prepLogMsg("loadPoster: picasso failed to load poster " +
                                        "into view"));

                                mCallback.onError(new RuntimeException("Picasso failed to load " +
                                        "the poster (poster path: \"" + mPosterPath + "\")."));
                            }
                        });
            }
        };

        // Callback for errors during PosterUri retrieval.
        final Action1<Throwable> onGetPosterUriError = new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                mCallback.onError(throwable);
            }
        };

        mStartDate = new Date();
        Log.d(TAG, prepLogMsg("loading poster"));

        mPosterUriSubscription = getPosterUri()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onGetPosterUriSuccess, onGetPosterUriError);
    }

    /**
     * Returns a full poster URI for mPosterPath and mPosterWidthPx (either a cached one or a new
     * one).
     *
     * @return A Single which resolves to a poster URI for mPosterPath and mPosterWidthPx.
     */
    private Single<PosterUri> getPosterUri() {
        PosterUri posterUri = getPosterUriFromCache(mPosterPath, mPosterWidthPx, this);

        if (posterUri != null) {
            return Single.just(posterUri);
        } else {
            Func1<TmdbApiConfiguration.PosterBaseUri, PosterUri> posterBaseUriToPosterUri =
                    new Func1<TmdbApiConfiguration.PosterBaseUri, PosterUri>() {
                @Override
                public PosterUri call(TmdbApiConfiguration.PosterBaseUri posterBaseUri) {
                    Log.v(TAG, prepLogMsg("getPosterUri: received poster base URI from TMDb API " +
                            "configuration: " + posterBaseUri.uri + "(width=" +
                            posterBaseUri.width + ")"));

                    return new PosterUri(posterBaseUri, mPosterPath);
                }
            };

            return TmdbApiConfiguration.getPosterBaseUri(mPosterWidthPx)
                    .map(posterBaseUriToPosterUri);
        }
    }

    private String prepLogMsg(String msg) {
        long elapsedTime = new Date().getTime() - mStartDate.getTime();
        return mPosterPath + " (mPosterWidthPx=" + mPosterWidthPx + "): " + elapsedTime + " ms: " +
                msg;
    }

    /**
     * Adds the given PosterUri to the cache (using mPosterPath as the key to find the right Set).
     *
     * @param posterUri The PosterUri.
     */
    private static synchronized void addPosterUriToCache(String posterPath, PosterUri posterUri,
                                                         PosterLoader instance) {
        TreeSet<PosterUri> cachedPostersUris = sCachedPostersUris.get(posterPath);
        if (cachedPostersUris == null) {
            cachedPostersUris = new TreeSet<>();
            sCachedPostersUris.put(posterPath, cachedPostersUris);
        }

        Log.v(TAG, instance.prepLogMsg("addPosterUriToCache: adding poster URI to cache (width=" +
                posterUri.width + ")"));

        cachedPostersUris.add(posterUri);
    }

    /**
     * Retrieves a poster URI from the cache. As only URIs of successfully downloaded images are
     * cached, an URI returned from this function will be an URI of an image that is already cached
     * by Picasso.
     *
     * The returned URI will be for the smallest already downloaded image that has a width equal to
     * or grater than mPosterWidthPx.
     *
     * @return A PosterUri instance, or null if no adequate image has been downloaded so far.
     */
    private static synchronized PosterUri getPosterUriFromCache(String posterPath,
                                                                int posterWidthPx,
                                                                PosterLoader instance) {
        if (!sCachedPostersUris.containsKey(posterPath)) {
            Log.v(TAG, instance.prepLogMsg("getPosterUriFromCache: no cached poster URIs found"));

            return null;
        }

        TreeSet<PosterUri> cachedPostersUris = sCachedPostersUris.get(posterPath);
        for (PosterUri posterUri : cachedPostersUris) {
            if (posterUri.width >= posterWidthPx) {
                Log.v(TAG, instance.prepLogMsg("getPosterUriFromCache: found cached poster URI " +
                        "(width=" + posterUri.width + ")"));

                return posterUri;
            }
        }

        Log.v(TAG, instance.prepLogMsg("getPosterUriFromCache: no cached poster URI found for " +
                "the requested width"));

        return null;
    }

    /**
     * Interface for PosterLoader callbacks.
     */
    public interface Callback {

        /**
         * Called after Picasso successfully loaded the poster into the ImageView.
         */
        void onSuccess();

        /**
         * Called on errors.
         *
         * @param throwable The error cause.
         */
        void onError(Throwable throwable);

    }

    /**
     * Class that holds a poster URI together with the corresponding poster width.
     */
    private static class PosterUri implements Comparable<PosterUri> {

        private final Uri uri;
        private final int width;

        private PosterUri(TmdbApiConfiguration.PosterBaseUri posterBaseUri, String posterPath) {
            uri = posterBaseUri.uri.buildUpon()
                    .appendEncodedPath(posterPath)
                    .build();

            this.width = posterBaseUri.width;
        }

        /**
         * Compares two PosterUri objects; used for natural sorting in the TreeSets. For efficiency
         * reasons this only compares the width. This works as expected as long as only PosterUri
         * objects with the same uri are stored in the same TreeSet.
         *
         * @param o {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        public int compareTo(@NonNull PosterUri o) {
            return width == o.width ? 0 : (width < o.width ? -1 : 1);
        }

    }

}
