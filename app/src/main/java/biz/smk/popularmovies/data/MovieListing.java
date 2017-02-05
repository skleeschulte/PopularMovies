/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import biz.smk.popularmovies.Application;
import biz.smk.popularmovies.favoritemovies.FavoriteMoviesContract;
import biz.smk.popularmovies.favoritemovies.FavoriteMoviesHelper;
import biz.smk.popularmovies.tmdbapi.TmdbApiClient;
import biz.smk.popularmovies.tmdbapi.TmdbApiClientFactory;
import biz.smk.popularmovies.tmdbapi.TmdbApiConfiguration;
import biz.smk.popularmovies.tmdbapi.responseobjects.MovieListingMovieDetails;
import biz.smk.popularmovies.tmdbapi.responseobjects.MovieListingPage;
import rx.Single;
import rx.SingleEmitter;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Class for accessing TMDb movie listings.
 */
public class MovieListing {

    /**
     * Enumeration of the accessible listing types.
     */
    public enum Type { TOP_RATED, POPULAR, FAVORITES }

    private static Map<Type, MovieListing> sInstances = new HashMap<>();

    private Type mType;
    private TmdbApiClient mApiClient;

    private Single<List<MovieListingMovieDetails>> mFavoriteMoviesSingle;
    private boolean MFavoriteMoviesContentObserverRegistered = false;

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
        if (mType == Type.FAVORITES) {
            return getFavoriteMovies().map(new Func1<List<MovieListingMovieDetails>, Integer>() {
                @Override
                public Integer call(List<MovieListingMovieDetails> movieList) {
                    return movieList.size();
                }
            });
        }

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
        if (mType == Type.FAVORITES) {
            return getFavoriteMovies().map(new Func1<List<MovieListingMovieDetails>, Long>() {
                @Override
                public Long call(List<MovieListingMovieDetails> movieList) {
                    return movieList.get(index).getId();
                }
            });
        }

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
     * Returns a Single that resolves to a list of the favorite movies.
     *
     * @return Single that resolves to a list of the favorite movies.
     */
    private synchronized Single<List<MovieListingMovieDetails>> getFavoriteMovies() {
        if (mFavoriteMoviesSingle == null) {
            mFavoriteMoviesSingle = Single.fromEmitter(
                    new Action1<SingleEmitter<List<MovieListingMovieDetails>>>() {
                @Override
                public void call(
                        final SingleEmitter<List<MovieListingMovieDetails>> singleEmitter) {
                    FavoriteMoviesHelper.GetMoviesCallback callback =
                            new FavoriteMoviesHelper.GetMoviesCallback() {
                                public void onMoviesReceived(
                                        List<MovieListingMovieDetails> movieList) {
                                    if (movieList != null) {
                                        for (MovieListingMovieDetails details : movieList) {
                                            MovieListingMovieDetailsStore.addMovieDetails(details);
                                        }

                                        singleEmitter.onSuccess(movieList);
                                    } else {
                                        singleEmitter.onError(new RuntimeException("could not " +
                                                "get favorite movies"));
                                    }
                                }
                            };

                    Context context = Application.getContext();
                    FavoriteMoviesHelper.getMovies(context.getContentResolver(), callback);
                }
            }).cache();
        }

        registerFavoriteMoviesContentObserver();

        return mFavoriteMoviesSingle;
    }

    /**
     * Registers a content observer for the favorite movies database and invalidates the cache when
     * changes occur.
     */
    private synchronized void registerFavoriteMoviesContentObserver() {
        if (!MFavoriteMoviesContentObserverRegistered) {
            MFavoriteMoviesContentObserverRegistered = true;
            Context context = Application.getContext();
            context.getContentResolver().registerContentObserver(FavoriteMoviesContract.FavoriteMovieEntry.CONTENT_URI, true, new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    synchronized (MovieListing.this) {
                        mFavoriteMoviesSingle = null;
                        EventBus.getDefault().post(new FavoriteMoviesChangedEvent());
                    }
                }
            });
        }
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

    /**
     * Emitted when the favorite movies listing changes.
     */
    public static class FavoriteMoviesChangedEvent {}

}
