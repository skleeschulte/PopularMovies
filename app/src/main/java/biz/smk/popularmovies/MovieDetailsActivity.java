/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;

import biz.smk.popularmovies.data.MovieListing;
import biz.smk.popularmovies.data.MovieListingMovieDetailsStore;
import biz.smk.popularmovies.tmdbapi.responseobjects.MovieListingMovieDetails;
import biz.smk.popularmovies.utilities.PosterLoader;
import biz.smk.popularmovies.utilities.StringUtils;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Movie details activity.
 */
public class MovieDetailsActivity extends AppCompatActivity {

    private static final String TAG = "MovieDetailsActivity";

    public static final String EXTRAS_FIELD_MOVIE_ID = "movie_id";
    public static final String EXTRAS_FIELD_FROM_LISTING_TYPE = "from_listing_type";
    public static final String EXTRAS_FIELD_LISTING_POSITION = "listing_position";

    private long mMovieId;
    private MovieListing.Type mFromListingType;
    private int mListingPosition;
    private String mPosterPath;

    Runnable mPosterSizeRunnable;
    PosterLoader mPosterLoader;

    @BindView(R.id.tv_movie_details_title) TextView mTitleView;
    @BindView(R.id.rl_movie_details_poster_layout) RelativeLayout mPosterLayout;
    @BindView(R.id.iv_movie_details_poster) ImageView mPosterView;
    @BindView(R.id.pb_movie_details_poster_loading_indicator) ProgressBar mPosterLoadingIndicator;
    @BindView(R.id.tv_movie_details_poster_error) TextView mPosterErrorView;
    @BindView(R.id.tv_movie_details_release_date) TextView mReleaseDateView;
    @BindView(R.id.tv_movie_details_rating) TextView mRatingView;
    @BindView(R.id.tv_movie_details_listing_position) TextView mListingPositionView;
    @BindView(R.id.tv_movie_details_overview) TextView mOverviewView;

    /**
     * Extracts extras from the intent, gets the movie data (by movieId from the intent extras) and
     * sets the UI up.
     *
     * @param savedInstanceState {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_movie_details);
        ButterKnife.bind(this);

        // Get extras from intent
        Intent fromIntent = getIntent();
        if (fromIntent != null) {
            if (fromIntent.hasExtra(EXTRAS_FIELD_MOVIE_ID)) {
                mMovieId = fromIntent.getLongExtra(EXTRAS_FIELD_MOVIE_ID, -1);
            }
            if (fromIntent.hasExtra(EXTRAS_FIELD_FROM_LISTING_TYPE)) {
                String listingTypeStr = fromIntent.getStringExtra(EXTRAS_FIELD_FROM_LISTING_TYPE);
                mFromListingType = MovieListing.Type.valueOf(listingTypeStr);
            }
            if (fromIntent.hasExtra(EXTRAS_FIELD_LISTING_POSITION)) {
                mListingPosition = fromIntent.getIntExtra(EXTRAS_FIELD_LISTING_POSITION, -1);
            }
        }

        // Get movie details from details store
        MovieListingMovieDetails details;
        try {
            details = MovieListingMovieDetailsStore.getMovieDetails(mMovieId);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to get details for movie ID " + mMovieId + ": " + e.toString());
            String msg = getString(R.string.activity_movie_details_load_details_failure);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            onBackPressed();
            return;
        }

        // Display title (if available)
        final String title = details.getTitle();
        if (title == null) {
            mTitleView.setVisibility(View.GONE);
        } else {
            // Post runnable to title auto-resize text view so that it will be laid out (und thus
            // have its width) when the text is auto resized.
            mTitleView.post(new Runnable() {
                @Override
                public void run() {
                    mTitleView.setText(title);
                }
            });
        }

        // Store poster path (if available) - poster loading is started in onResume
        mPosterPath = details.getPosterPath();
        if (mPosterPath == null) {
            setNoPosterView();
        }

        // Display release date (if available)
        Date releaseDate = details.getParsedReleaseDate();
        if (releaseDate == null) {
            mReleaseDateView.setVisibility(View.GONE);
        } else {
            String formattedReleaseDate = DateFormat.getMediumDateFormat(this).format(releaseDate);
            String nonBreakingReleaseDate = StringUtils.makeStringUnbreakable(formattedReleaseDate);
            String releaseDateText = getString(R.string.activity_movie_details_release_date,
                    nonBreakingReleaseDate);
            mReleaseDateView.setText(releaseDateText);
        }

        // Display rating (if available)
        double rating = details.getVoteAverage();
        if (rating == -1) {
            mRatingView.setVisibility(View.GONE);
        } else {
            // Double is formatted as string so only as many digits as necessary are shown.
            @SuppressLint("StringFormatMatches")
            String ratingText = getString(R.string.activity_movie_details_rating, rating);
            mRatingView.setText(ratingText);
        }

        // Display position in movie listing from where this activity was called (if available)
        String listingName = "";
        switch(mFromListingType) {
            case POPULAR:
                listingName = getString(R.string.activity_movie_details_popular);
                break;
            case TOP_RATED:
                listingName = getString(R.string.activity_movie_details_top_rated);
                break;
        }
        if (listingName.equals("") || mListingPosition <= 0) {
            mListingPositionView.setVisibility(View.GONE);
        } else {
            String listingPositionText = getString(R.string.activity_movie_details_listing_position,
                    mListingPosition, listingName);
            mListingPositionView.setText(listingPositionText);
        }

        // Display overview (if available)
        String overview = details.getOverview();
        if (overview != null) {
            mOverviewView.setText(overview);
        }
    }

    /**
     * Triggers poster loading if a poster path is available and the poster is not already loaded.
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (mPosterPath != null && mPosterView.getDrawable() == null) {
            loadPoster();
        }
    }

    /**
     * Cancels all pending poster loading requests.
     */
    @Override
    protected void onPause() {
        // Cancel all pending requests
        if (mPosterSizeRunnable != null) mPosterView.removeCallbacks(mPosterSizeRunnable);
        if (mPosterLoader != null) mPosterLoader.cancel();

        super.onPause();
    }

    /**
     * Handles clicks on the poster error message (triggers loading the poster).
     *
     * @param view The clicked view.
     */
    @SuppressWarnings("unused")
    @OnClick(R.id.tv_movie_details_poster_error)
    void handlePosterErrorClick(View view) {
        loadPoster();
    }

    /**
     * Entrypoint for poster loading. Posts a new runnable to the poster view to get its
     * dimensions. Then triggers the download of the movie poster in the appropriate size.
     */
    private void loadPoster() {
        setPosterPendingView();

        final PosterLoader.Callback callback = new PosterLoader.Callback() {
            @Override
            public void onSuccess() {
                setPosterLoadedView();
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e(TAG, "Failed to load poster: " + throwable.toString());
                setPosterErrorView();
            }
        };

        if (mPosterSizeRunnable != null) {
            mPosterView.removeCallbacks(mPosterSizeRunnable);
        }

        mPosterSizeRunnable = new Runnable() {
            @Override
            public void run() {
                int posterViewWidth = mPosterView.getWidth();

                mPosterLoader = new PosterLoader(MovieDetailsActivity.this, mPosterPath,
                        posterViewWidth, mPosterView, callback);
                mPosterLoader.loadPoster();
            }
        };

        mPosterView.post(mPosterSizeRunnable);
    }

    private void setNoPosterView() {
        mPosterLayout.setVisibility(View.GONE);
    }

    private void setPosterPendingView() {
        mPosterLayout.setVisibility(View.VISIBLE);
        mPosterLoadingIndicator.setVisibility(View.VISIBLE);
        mPosterView.setVisibility(View.INVISIBLE);
        mPosterErrorView.setVisibility(View.GONE);
    }

    private void setPosterLoadedView() {
        mPosterLayout.setVisibility(View.VISIBLE);
        mPosterLoadingIndicator.setVisibility(View.GONE);
        mPosterView.setVisibility(View.VISIBLE);
        mPosterErrorView.setVisibility(View.GONE);
    }

    private void setPosterErrorView() {
        mPosterLayout.setVisibility(View.VISIBLE);
        mPosterLoadingIndicator.setVisibility(View.GONE);
        mPosterView.setVisibility(View.GONE);
        mPosterErrorView.setVisibility(View.VISIBLE);
    }

    /**
     * Explicitly handles presses on the back button in the AppCompat Toolbar, because otherwise the
     * parent activity gets an empty savedInstanceState when resumed.
     *
     * @param item {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
