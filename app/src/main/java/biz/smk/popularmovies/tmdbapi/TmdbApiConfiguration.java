/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies.tmdbapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Date;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import biz.smk.popularmovies.Application;
import biz.smk.popularmovies.tmdbapi.responseobjects.Configuration;
import biz.smk.popularmovies.tmdbapi.responseobjects.ImagesConfiguration;
import biz.smk.popularmovies.utilities.StringUtils;
import rx.Single;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Class for accessing the TMDb API configuration. Relevant configuration data is cached "for a few
 * days" as proposed by the API documentation:
 * https://developers.themoviedb.org/3/configuration
 */
public class TmdbApiConfiguration {

    private static final String TAG = "TmdbApiConfiguration";

    /**
     * (Maximum) number of results per movie listing page. Result sets have a fixed number of 20
     * results per page (except last page) - see:
     * https://www.themoviedb.org/talk/587bea71c3a36846c300ff73#587ce9879251413eec019ac2
     */
    public static final int NUM_LISTING_ENTRIES_PER_PAGE = 20;

    private static final String SHARED_PREFS_NAME = "tmdb_api_configuration_cache";
    private static final String SHARED_PREFS_IMAGES_BASE_URL_FIELD = "images_base_url";
    private static final String SHARED_PREFS_POSTER_SIZES_FIELD = "poster_sizes";
    private static final String SHARED_PREFS_POSTER_SIZES_DIVIDER = ",";
    private static final String SHARED_PREFS_LAST_UPDATE_FIELD = "last_update";
    private static final long MAX_CONFIG_CACHE_AGE = 3 * 24 * 60 * 60 * 1000; // 3 days in ms

    /**
     * RxJava Single that resolves to the generated poster base URIs. The Single is initialized once
     * with .cache() so that it can be returned to all future observers.
     */
    private static Single<TreeSet<PosterBaseUri>> sPosterBaseUrisSingle;

    private static SharedPreferences getSharedPreferences() {
        Context appContext = Application.getContext();
        return appContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Stores relevant configuration data received from the TMDb API in shared preferences.
     *
     * @param configuration Configuration data as received from TMDb API.
     */
    private static void storeConfigInSharedPrefs(Configuration configuration) {
        ImagesConfiguration imgConfig = configuration.getImagesConfiguration();

        String joinedPosterSizes =
                StringUtils.join(SHARED_PREFS_POSTER_SIZES_DIVIDER, imgConfig.getPosterSizes());

        getSharedPreferences().edit()
                .putString(SHARED_PREFS_IMAGES_BASE_URL_FIELD, imgConfig.getBaseUrl())
                .putString(SHARED_PREFS_POSTER_SIZES_FIELD, joinedPosterSizes)
                .putLong(SHARED_PREFS_LAST_UPDATE_FIELD, new Date().getTime())
                .apply();
    }

    /**
     * Gets the cached configuration data from shared preferences. Returns null if no configuration
     * data has been cached so far or if the cache is outdated or the data invalid.
     *
     * @return Cached configuration data or null.
     */
    private static CachedConfiguration getConfigFromSharedPrefs() {
        SharedPreferences preferences = getSharedPreferences();

        Long lastUpdate = preferences.getLong(SHARED_PREFS_LAST_UPDATE_FIELD, 0);
        Long now = new Date().getTime();

        if (now - lastUpdate <= MAX_CONFIG_CACHE_AGE) {
            String joinedPosterSizes = preferences.getString(SHARED_PREFS_POSTER_SIZES_FIELD, "");
            String splitPattern = Pattern.quote(SHARED_PREFS_POSTER_SIZES_DIVIDER);

            CachedConfiguration cachedConfig = new CachedConfiguration();
            cachedConfig.imagesBaseUrl =
                    preferences.getString(SHARED_PREFS_IMAGES_BASE_URL_FIELD, "");
            cachedConfig.posterSizes = joinedPosterSizes.split(splitPattern);

            // Return null if invalid configuration data was cached.
            if (cachedConfig.imagesBaseUrl.equals("") || cachedConfig.posterSizes.length == 0) {
                return null;
            }

            return cachedConfig;
        }

        return null;
    }

    /**
     * Returns a RxJava Single which resolves to the cached configuration data. If no valid cached
     * data is available, an API request is made to initialize/refresh the cache.
     *
     * @return Single that resolves to configuration data.
     */
    private static Single<CachedConfiguration> getCachedConfiguration() {
        CachedConfiguration cachedConfiguration = getConfigFromSharedPrefs();

        if (cachedConfiguration != null) {
            return Single.just(cachedConfiguration);
        } else {
            TmdbApiClient api = TmdbApiClientFactory.createApiClient();

            return api.getConfiguration().map(new Func1<Configuration, CachedConfiguration>() {
                @Override
                public CachedConfiguration call(Configuration configuration) {
                    storeConfigInSharedPrefs(configuration);
                    CachedConfiguration cachedConfig = getConfigFromSharedPrefs();

                    if (cachedConfig == null) {
                        throw new RuntimeException("Could not get configuration from cache after " +
                                "the cache was updated. Probably the configuration data received " +
                                "from the API is invalid.");
                    }

                    return cachedConfig;
                }
            });
        }
    }

    /**
     * Returns a RxJava Single which resolves to a TreeSet containing the available poster base
     * URIs.
     *
     * @return Single that resolves to a TreeSet containing PosterBaseUri objects.
     */
    private static synchronized Single<TreeSet<PosterBaseUri>> getPosterBaseUris() {
        if (sPosterBaseUrisSingle == null) {
            final Func1<CachedConfiguration, TreeSet<PosterBaseUri>>
                    cachedConfigurationToPosterBaseUris =
                    new Func1<CachedConfiguration, TreeSet<PosterBaseUri>>() {
                @Override
                public TreeSet<PosterBaseUri> call(CachedConfiguration cachedConfiguration) {
                    return generatePosterBaseUris(cachedConfiguration);
                }
            };

            final Action1<Throwable> removeFailedSingle = new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    synchronized (TmdbApiConfiguration.class) {
                        sPosterBaseUrisSingle = null;
                    }
                }
            };

            sPosterBaseUrisSingle = getCachedConfiguration()
                    .map(cachedConfigurationToPosterBaseUris)
                    .doOnError(removeFailedSingle)
                    .cache();
        }

        return sPosterBaseUrisSingle;
    }

    /**
     * Generates the available poster base URIs based on the TMDb API configuration data.
     *
     * @param cachedConfiguration The TMDb API configuration data.
     * @return TreeSet containing PosterBaseUri objects.
     */
    private static TreeSet<PosterBaseUri> generatePosterBaseUris(
            CachedConfiguration cachedConfiguration) {
        TreeSet<PosterBaseUri> posterBaseUris = new TreeSet<>();

        Pattern widthPattern = Pattern.compile("^w([0-9]+)$", Pattern.CASE_INSENSITIVE);
        Pattern heightPattern = Pattern.compile("^h([0-9]+)$", Pattern.CASE_INSENSITIVE);
        Matcher matcher;

        for (String sizeString : cachedConfiguration.posterSizes) {
            int width;

            if (sizeString.equalsIgnoreCase("original")) {
                width = Integer.MAX_VALUE;
            } else if ((matcher = widthPattern.matcher(sizeString)).matches()) {
                width = Integer.valueOf(matcher.group(1));
            } else if (heightPattern.matcher(sizeString).matches()) {
                Log.d(TAG, "generatePosterBaseUris: Ignoring height string: " + sizeString);
                continue;
            } else {
                Log.w(TAG, "generatePosterBaseUris: Found unknown size string: " + sizeString);
                continue;
            }

            PosterBaseUri posterBaseUri = new PosterBaseUri(cachedConfiguration.imagesBaseUrl,
                    sizeString, width);

            posterBaseUris.add(posterBaseUri);
        }

        return posterBaseUris;
    }

    /**
     * Returns a RxJava Single that resolves to the appropriate poster base URI for the given
     * minWidth.
     *
     * @param minWidth The desired minimum width of the poster.
     * @return A Single that resolves to the PosterBaseUri for the desired minimum width.
     */
    public static Single<PosterBaseUri> getPosterBaseUri(final int minWidth) {
        Func1<TreeSet<PosterBaseUri>, PosterBaseUri> selectPosterBaseUri =
                new Func1<TreeSet<PosterBaseUri>, PosterBaseUri>() {
            @Override
            public PosterBaseUri call(TreeSet<PosterBaseUri> posterBaseUris) {
                for (PosterBaseUri posterBaseUri : posterBaseUris) {
                    if (posterBaseUri.width >= minWidth) {
                        return posterBaseUri;
                    }
                }

                return posterBaseUris.last();
            }
        };

        return getPosterBaseUris()
                .map(selectPosterBaseUri);
    }

    /**
     * Class that holds a poster base URI together with the corresponding poster width.
     */
    public static class PosterBaseUri implements Comparable<PosterBaseUri> {

        /**
         * Base URI for posters of a specific width.
         */
        public final Uri uri;

        /**
         * The width of the posters returned from this base URI.
         */
        public final int width;

        private PosterBaseUri(String imagesBaseUrl, String sizePathElement, int width) {
            uri = Uri.parse(imagesBaseUrl).buildUpon()
                    .appendPath(sizePathElement)
                    .build();

            this.width = width;
        }

        /**
         * Compares two PosterBaseUri objects; used for natural sorting in the TreeSets. This only
         * compares the widths of the PosterBaseUri objects, not the uri itself.
         *
         * @param o {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        public int compareTo(@NonNull PosterBaseUri o) {
            return width == o.width ? 0 : (width < o.width ? -1 : 1);
        }

    }

    /**
     * Class for objects that hold image configuration data.
     */
    private static class CachedConfiguration {

        /**
         * Base URL for images (without image size).
         */
        private String imagesBaseUrl;

        /**
         * Available poster size strings (for image URLs).
         */
        private String[] posterSizes;

    }

}
