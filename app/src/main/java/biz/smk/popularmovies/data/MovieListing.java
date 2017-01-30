/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies.data;

import android.annotation.SuppressLint;

import java.util.HashMap;
import java.util.Map;

import biz.smk.popularmovies.tmdbapi.TmdbApiClient;
import biz.smk.popularmovies.tmdbapi.TmdbApiClientFactory;
import biz.smk.popularmovies.tmdbapi.TmdbApiConfiguration;
import biz.smk.popularmovies.tmdbapi.responseobjects.MovieListingMovieDetails;
import biz.smk.popularmovies.tmdbapi.responseobjects.MovieListingPage;
import rx.Single;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Class for accessing TMDb movie listings.
 */
public class MovieListing {

    /**
     * Enumeration of the accessible listing types.
     */
    public enum Type { TOP_RATED, POPULAR }

    private static Map<Type, MovieListing> sInstances = new HashMap<>();

    private Type mType;
    private TmdbApiClient mApiClient;

    @SuppressLint("UseSparseArrays")
    private Map<Integer, Single<MovieListingPage>> mPageRequestsCache = new HashMap<>();

    /**
     * Returns the MovieListing instance for the given MovieListing.Type.
     *
     * @param type MovieListing.Type.
     * @return The MovieListing for the given type.
     */
    public static synchronized MovieListing getListing(Type type) {
        MovieListing movieListing = sInstances.get(type);
        if (movieListing == null) {
            movieListing = new MovieListing(type);
            sInstances.put(type, movieListing);
        }
        return movieListing;
    }

    private MovieListing(Type type) {
        mType = type;
        mApiClient = TmdbApiClientFactory.createApiClient();
    }

    public Type getType() {
        return mType;
    }

    /**
     * Returns a RxJava Single that resolves to the TMDb API result for the given page of the
     * current listing.
     *
     * @param pageNr The page number of the TMDb API movie listing.
     * @return The TMDb API response.
     */
    private Single<MovieListingPage> getPage(final int pageNr) {
        switch(mType) {
            case TOP_RATED:
                return mApiClient.getTopRatedMovies(pageNr);
            case POPULAR:
                return mApiClient.getPopularMovies(pageNr);
            default:
                throw new RuntimeException("Unhandled type " + mType.toString());
        }
    }

    /**
     * Get the movie listing page with the given pageNr. If there already is a pending or successful
     * request for that page, the corresponding RxJava Single will be returned. Otherwise a new
     * request is made.
     *
     * The movie details contained in the response are stored in the MovieListingMovieDetailsStore.
     *
     * @param pageNr The number of the page of this movie listing that shall be returned.
     * @return The MovieListingPage for the given pageNr.
     */
    private synchronized Single<MovieListingPage> getPageWithCaching(final int pageNr) {
        Single<MovieListingPage> pageSingle = mPageRequestsCache.get(pageNr);

        if (pageSingle == null) {
            Action1<MovieListingPage> storeMovieDetails = new Action1<MovieListingPage>() {
                @Override
                public void call(MovieListingPage movieListingPage) {
                    for (MovieListingMovieDetails details : movieListingPage.getResults()) {
                        MovieListingMovieDetailsStore.addMovieDetails(details);
                    }
                }
            };

            Action1<Throwable> removeFailedSingle = new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    synchronized (MovieListing.this) {
                        mPageRequestsCache.remove(pageNr);
                    }
                }
            };

            pageSingle = getPage(pageNr)
                    .doOnSuccess(storeMovieDetails)
                    .doOnError(removeFailedSingle)
                    .cache();

            mPageRequestsCache.put(pageNr, pageSingle);
        }
        return pageSingle;
    }

    /**
     * Returns a RxJava Single that resolves to the total count of movies in this listing.
     *
     * @return The total count of movies in this movie listing.
     */
    public Single<Integer> getTotalCount() {
        return getPageWithCaching(1).map(new Func1<MovieListingPage, Integer>() {
            @Override
            public Integer call(MovieListingPage movieListingPage) {
                return movieListingPage.getTotalResults();
            }
        });
    }

    /**
     * Returns the movie ID for the given (zero-based) index within this listing.
     *
     * @param index (Zero-based) movie index.
     * @return The movie ID.
     */
    public Single<Long> getMovieId(final int index) {
        int pageNr = calculatePageNr(index);

        return getPageWithCaching(pageNr).map(new Func1<MovieListingPage, Long>() {
            @Override
            public Long call(MovieListingPage movieListingPage) {
                int indexOnPage = calculateIndexOnPage(index);
                return movieListingPage.getResults().get(indexOnPage).getId();
            }
        });
    }

    /**
     * Returns the number of the page containing the entry with the given index.
     *
     * @param index Zero-based index of an entry in a listing.
     * @return Page number for querying the API.
     */
    private static int calculatePageNr(int index) {
        int entriesPerPage = TmdbApiConfiguration.NUM_LISTING_ENTRIES_PER_PAGE;
        return index / entriesPerPage + 1;
    }

    /**
     * Returns the (zero-based) index on the result page for a given index within this listing.
     *
     * @param indexWithinListing Zero-based index within this listing.
     * @return Zero-based index on the result page.
     */
    private static int calculateIndexOnPage(int indexWithinListing) {
        int entriesPerPage = TmdbApiConfiguration.NUM_LISTING_ENTRIES_PER_PAGE;
        return indexWithinListing % entriesPerPage;
    }

}
