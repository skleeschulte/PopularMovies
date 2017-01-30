/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Timer;
import java.util.TimerTask;

import biz.smk.popularmovies.data.MovieListing;
import biz.smk.popularmovies.data.MovieListingMovieDetailsStore;
import biz.smk.popularmovies.tmdbapi.responseobjects.MovieListingMovieDetails;
import biz.smk.popularmovies.utilities.PosterLoader;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * View holder for the poster grid adapter / RecyclerView. The view holder is responsible for
 * requesting the movie ID that belongs to its position in the listing (= in the adapter). It also
 * triggers downloading the corresponding movie poster in the appropriate size.
 */
class PosterGridViewHolder extends RecyclerView.ViewHolder {

    private static final String TAG = "PosterGridViewHolder";

    private static final int GLOBAL_LAYOUT_WATCHDOG_TIMEOUT = 1000; // ms

    private int mViewHolderNumber;
    private Context mContext;
    private MovieListing mMovieListing;
    private int mPosition;
    private long mMovieId;

    private Subscription mMovieIdSubscription;
    private ViewTreeObserver mViewTreeObserver;
    private ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener;
    private TimerTask mOnGlobalLayoutListenerWatchdog;
    private PosterLoader mPosterLoader;

    private enum Status { PENDING, SUCCESS, ERROR }
    private Status mStatus;

    @BindView(R.id.pb_poster_grid_item_loading_indicator) ProgressBar mLoadingIndicatorView;
    @BindView(R.id.iv_poster_grid_item_poster) ImageView mPosterView;
    @BindView(R.id.tv_poster_grid_item_listing_position) TextView mPositionView;
    @BindView(R.id.tv_poster_grid_item_error) TextView mErrorView;
    @BindView(R.id.tv_poster_grid_item_no_poster) TextView mNoPictureView;

    @BindString(R.string.poster_grid_item_no_picture_msg) String mNoPictureMsg;

    /**
     * Initializes a new view holder.
     *
     * @param itemView The item view.
     */
    PosterGridViewHolder(View itemView, int viewHolderNumber) {
        super(itemView);

        ButterKnife.bind(this, itemView);

        mViewHolderNumber = viewHolderNumber;
        mContext = itemView.getContext();
    }

    /**
     * Binds the view. This triggers the request of the movieId for the adapter position and
     * subsequently the movie poster.
     *
     * @param movieListing The current movie listing of the adapter.
     * @param position The position within the adapter.
     */
    void bind(MovieListing movieListing, int position) {
        mPosition = position;
        mMovieId = -1;
        Log.v(TAG, prepLogMsg("binding"));

        mMovieListing = movieListing;

        String positionString = (position + 1) + "."; // adapter position is zero-based
        mPositionView.setText(positionString);

        loadMovieId();

        EventBus.getDefault().register(this);
    }

    /**
     * Unbinds the view. All pending requests are cancelled and the image is removed.
     */
    void unbind() {
        Log.v(TAG,prepLogMsg("unbinding"));

        EventBus.getDefault().unregister(this);

        if (mMovieIdSubscription != null) {
            mMovieIdSubscription.unsubscribe();
        }

        removeOnGlobalLayoutListener();

        if (mOnGlobalLayoutListenerWatchdog != null) {
            mOnGlobalLayoutListenerWatchdog.cancel();
        }

        if (mPosterLoader != null) {
            mPosterLoader.cancel();
        }

        mPosterView.setImageDrawable(null);
        mNoPictureView.setText(null);
    }

    @SuppressWarnings("deprecation")
    private void removeOnGlobalLayoutListener() {
        if (mViewTreeObserver != null && mOnGlobalLayoutListener != null &&
                mViewTreeObserver.isAlive()) {
            if (android.os.Build.VERSION.SDK_INT >= 16) {
                mViewTreeObserver.removeOnGlobalLayoutListener(mOnGlobalLayoutListener);
            } else {
                mViewTreeObserver.removeGlobalOnLayoutListener(mOnGlobalLayoutListener);
            }
        }

        mOnGlobalLayoutListener = null;
    }

    /**
     * Handles a click on a poster. If there was an error, this triggers a retry. Otherwise a poster
     * clicked event is emitted.
     *
     * @param view The view that was clicked.
     */
    @SuppressWarnings("unused")
    @OnClick(R.id.rl_poster_grid_item)
    void handleClick(View view) {
        Log.d(TAG, prepLogMsg("click (mStatus=" + mStatus.toString() + ")"));

        if (mStatus == Status.ERROR) {
            EventBus.getDefault().post(new RetryRequestedEvent());
        } else if (mStatus == Status.SUCCESS) {
            EventBus.getDefault().post(new PosterGridAdapter.PosterClickedEvent(mPosition,
                    mMovieId));
        }
    }

    /**
     * Handles retry events.
     *
     * @param event The event.
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRetryRequested(RetryRequestedEvent event) {
        if (mStatus == Status.ERROR) {
            if (mMovieId == -1) {
                loadMovieId();
            } else {
                loadPoster();
            }
        }
    }

    /**
     * Requests the movieId for the current position in the listing. Triggers loading the poster on
     * success.
     */
    private void loadMovieId() {
        mStatus = Status.PENDING;
        setPendingView();

        Action1<Long> setMovieIdAndLoadPoster = new Action1<Long>() {
            @Override
            public void call(Long movieId) {
                mMovieId = movieId;
                Log.v(TAG, prepLogMsg("received movie ID"));
                loadPoster();
            }
        };

        Action1<Throwable> handleMovieIdFailure = new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Log.e(TAG, prepLogMsg("error loading movie ID: ") + throwable.toString());
                mMovieIdSubscription.unsubscribe();
                mStatus = Status.ERROR;
                setErrorView();
            }
        };

        Log.v(TAG, prepLogMsg("loading movie ID"));

        mMovieIdSubscription = mMovieListing.getMovieId(mPosition)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(setMovieIdAndLoadPoster, handleMovieIdFailure);
    }

    /**
     * Gets the width of the poster view (listens for GlobalLayout events if necessary) and loads
     * the poster. If the poster path is empty, shows a text message with the title.
     */
    private void loadPoster() {
        if (getPosterPath() == null) {
            Log.d(TAG, prepLogMsg("no poster path"));

            mStatus = Status.SUCCESS;

            String title = MovieListingMovieDetailsStore.getMovieDetails(mMovieId).getTitle();
            setNoPictureView(title);

            return;
        }

        mStatus = Status.PENDING;
        setPendingView();

        final PosterLoader.Callback callback = new PosterLoader.Callback() {
            @Override
            public void onSuccess() {
                Log.v(TAG, prepLogMsg("poster loaded"));
                mStatus = Status.SUCCESS;
                setReadyView();
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e(TAG, prepLogMsg("failed to load poster: " + throwable.toString()));
                mStatus = Status.ERROR;
                setErrorView();
            }
        };

        if (mPosterView.getWidth() > 0 && mPosterView.getHeight() > 0) {
            Log.v(TAG, prepLogMsg("poster view already has size - loading poster"));

            mPosterLoader = new PosterLoader(mContext, getPosterPath(), mPosterView.getWidth(),
                    mPosterView, callback);
            mPosterLoader.loadPoster();
        } else {
            Log.v(TAG, prepLogMsg("poster view does not have size yet - listening for " +
                    "GlobalLayout events"));

            mOnGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    Log.v(TAG, prepLogMsg("received GlobalLayout event - loading poster"));

                    removeOnGlobalLayoutListener();
                    mOnGlobalLayoutListenerWatchdog.cancel();

                    mPosterLoader = new PosterLoader(mContext, getPosterPath(),
                            mPosterView.getWidth(), mPosterView, callback);
                    mPosterLoader.loadPoster();
                }
            };

            mViewTreeObserver = mPosterView.getViewTreeObserver();
            mViewTreeObserver.addOnGlobalLayoutListener(mOnGlobalLayoutListener);

            // Rarely onGlobalLayout() will never be called. Thus adding a watchdog here with one
            // last try.
            mOnGlobalLayoutListenerWatchdog = new TimerTask() {
                @Override
                public void run() {
                    if (mOnGlobalLayoutListener != null) {
                        Log.v(TAG, prepLogMsg("GlobalLayout listener has not been called after " +
                                GLOBAL_LAYOUT_WATCHDOG_TIMEOUT + " ms - last try to get size"));

                        removeOnGlobalLayoutListener();

                        if (mPosterView.getWidth() > 0 && mPosterView.getHeight() > 0) {
                            Log.v(TAG, prepLogMsg("poster view has size - loading poster"));

                            mPosterLoader = new PosterLoader(mContext, getPosterPath(),
                                    mPosterView.getWidth(), mPosterView, callback);
                            mPosterLoader.loadPoster();
                        } else {
                            Log.e(TAG, prepLogMsg("poster view still has no size - cannot load " +
                                    "poster"));

                            mStatus = Status.ERROR;
                            setErrorView();
                        }
                    }
                }
            };
            new Timer().schedule(mOnGlobalLayoutListenerWatchdog, GLOBAL_LAYOUT_WATCHDOG_TIMEOUT);
        }
    }

    /**
     * Returns the poster path for the current movie (the path must be appended to the poster
     * base URI).
     *
     * @return Poster path or null if no poster path is available or mMovieId is invalid.
     */
    private String getPosterPath() {
        try {
            MovieListingMovieDetails details =
                    MovieListingMovieDetailsStore.getMovieDetails(mMovieId);
            return details.getPosterPath();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, prepLogMsg("invalid movie ID, could not get poster path from movie " +
                    "listing details store"));
            return null;
        }
    }

    private String prepLogMsg(String msg) {
        return "#" + mViewHolderNumber + " at position " + mPosition + " (movie ID " + mMovieId + "): " + msg;
    }

    private void setPendingView() {
        mLoadingIndicatorView.setVisibility(View.VISIBLE);
        mPosterView.setVisibility(View.VISIBLE);
        mErrorView.setVisibility(View.GONE);
        mNoPictureView.setVisibility(View.GONE);
    }

    private void setReadyView() {
        mLoadingIndicatorView.setVisibility(View.GONE);
        mPosterView.setVisibility(View.VISIBLE);
        mErrorView.setVisibility(View.GONE);
        mNoPictureView.setVisibility(View.GONE);
    }

    private void setErrorView() {
        mLoadingIndicatorView.setVisibility(View.GONE);
        mPosterView.setVisibility(View.GONE);
        mErrorView.setVisibility(View.VISIBLE);
        mNoPictureView.setVisibility(View.GONE);
    }

    private void setNoPictureView(String movieName) {
        mLoadingIndicatorView.setVisibility(View.GONE);
        mPosterView.setVisibility(View.INVISIBLE);
        mErrorView.setVisibility(View.GONE);
        mNoPictureView.setVisibility(View.VISIBLE);

        Integer fontColor = ContextCompat.getColor(mContext, R.color.colorPrimary);
        Integer fontColorWithoutAlpha = fontColor & 16777215; // 2^24 - 1 = 16777215

        Spanned text = fromHtml("<b><font color=\"#" + Integer.toHexString(fontColorWithoutAlpha) +
                "\">" + movieName + "</font></b><br><br>(" + mNoPictureMsg + ")");
        mNoPictureView.setText(text);
    }

    @SuppressWarnings("deprecation")
    private Spanned fromHtml(String source) {
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(source);
        }
    }

    /**
     * Emitted when retrying failed requests ist requested.
     */
    private static class RetryRequestedEvent {}

}
