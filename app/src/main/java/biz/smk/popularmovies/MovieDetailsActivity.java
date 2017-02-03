/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;

import biz.smk.popularmovies.data.MovieListing;
import biz.smk.popularmovies.data.MovieListingMovieDetailsStore;
import biz.smk.popularmovies.tmdbapi.TmdbApiClient;
import biz.smk.popularmovies.tmdbapi.TmdbApiClientFactory;
import biz.smk.popularmovies.tmdbapi.responseobjects.MovieListingMovieDetails;
import biz.smk.popularmovies.tmdbapi.responseobjects.MovieReviewDetails;
import biz.smk.popularmovies.tmdbapi.responseobjects.MovieReviews;
import biz.smk.popularmovies.tmdbapi.responseobjects.MovieVideoDetails;
import biz.smk.popularmovies.tmdbapi.responseobjects.MovieVideos;
import biz.smk.popularmovies.utilities.PosterLoader;
import biz.smk.popularmovies.utilities.SingleLoader;
import biz.smk.popularmovies.utilities.StringUtils;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Single;

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

    private static final int VIDEO_LIST_LOADER_ID = 0;
    private static final int REVIEW_LIST_LOADER_ID = 1;

    @BindView(R.id.tv_movie_details_title) TextView mTitleView;
    @BindView(R.id.rl_movie_details_poster_layout) RelativeLayout mPosterLayout;
    @BindView(R.id.iv_movie_details_poster) ImageView mPosterView;
    @BindView(R.id.pb_movie_details_poster_loading_indicator) ProgressBar mPosterLoadingIndicator;
    @BindView(R.id.tv_movie_details_poster_error) TextView mPosterErrorView;
    @BindView(R.id.tv_movie_details_release_date) TextView mReleaseDateView;
    @BindView(R.id.tv_movie_details_rating) TextView mRatingView;
    @BindView(R.id.tv_movie_details_listing_position) TextView mListingPositionView;
    @BindView(R.id.tv_movie_details_overview) TextView mOverviewView;

    @BindView(R.id.pb_movie_details_video_list_loading_indicator)
    ProgressBar mVideoListLoadingIndicator;
    @BindView(R.id.tv_movie_details_video_list_empty_msg) TextView mVideoListEmptyMsg;
    @BindView(R.id.tv_movie_details_video_list_error_msg) TextView mVideoListErrorMsg;
    @BindView(R.id.ll_movie_details_video_list) LinearLayout mVideoList;

    @BindView(R.id.pb_review_list_loading_indicator) ProgressBar mReviewListLoadingIndicator;
    @BindView(R.id.tv_review_list_empty_msg) TextView mReviewListEmptyMsg;
    @BindView(R.id.tv_review_list_error_msg) TextView mReviewListErrorMsg;
    @BindView(R.id.ll_review_list) LinearLayout mReviewList;

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

        // Load video list
        loadVideoList(false);

        // Load review list
        loadReviewList(false);
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
     * Handles clicks on the video list error message (triggers loading the video list).
     *
     * @param view The clicked view.
     */
    @SuppressWarnings("unused")
    @OnClick(R.id.tv_movie_details_video_list_error_msg)
    void handleVideoListErrorClick(View view) {
        loadVideoList(true);
    }

    private void loadVideoList(boolean forceReload) {
        setVideoListPendingView();

        if (forceReload) {
            getSupportLoaderManager()
                    .restartLoader(VIDEO_LIST_LOADER_ID, null, new VideoListLoaderCallbacks());
        } else {
            getSupportLoaderManager()
                    .initLoader(VIDEO_LIST_LOADER_ID, null, new VideoListLoaderCallbacks());
        }
    }

    private void setVideoListPendingView() {
        mVideoListLoadingIndicator.setVisibility(View.VISIBLE);
        mVideoListErrorMsg.setVisibility(View.GONE);
        mVideoListEmptyMsg.setVisibility(View.GONE);
        mVideoList.setVisibility(View.GONE);
    }

    /**
     * Update the UI after the video list was loaded. Displays a text message if there are no
     * videos. Otherwise populates a LinearLayout with clickable video entries. Only videos of type
     * trailer and hosted on YouTube are displayed.
     *
     * @param videos MovieVideos result from TMDb API.
     */
    private void setVideoListLoadedView(MovieVideos videos) {
        mVideoListLoadingIndicator.setVisibility(View.GONE);
        mVideoListErrorMsg.setVisibility(View.GONE);
        mVideoListEmptyMsg.setVisibility(View.GONE);
        mVideoList.setVisibility(View.GONE);

        int videoCount = 0;
        mVideoList.removeAllViews();

        for (MovieVideoDetails details : videos.getMovieVideoDetails()) {
            if (!details.isTrailer() || !details.isHostedOnYouTube()) continue;

            String name = details.getName();
            String key = details.getKey();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(key)) continue;

            videoCount++;

            View listItem =
                    getLayoutInflater().inflate(R.layout.video_list_item, mVideoList, false);

            TextView title = (TextView) listItem.findViewById(R.id.tv_video_list_item_title);
            title.setText(name);

            listItem.setTag(key);

            listItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String videoId = (String) v.getTag();
                    openYouTubeVideo(videoId);
                }
            });

            mVideoList.addView(listItem);
        }

        if (videoCount == 0) {
            mVideoListEmptyMsg.setVisibility(View.VISIBLE);
        } else {
            mVideoList.setVisibility(View.VISIBLE);
        }
    }

    private void setVideoListErrorView() {
        mVideoListLoadingIndicator.setVisibility(View.GONE);
        mVideoListErrorMsg.setVisibility(View.VISIBLE);
        mVideoListEmptyMsg.setVisibility(View.GONE);
        mVideoList.setVisibility(View.GONE);
    }

    /**
     * Opens the YouTube app if it is available, otherwise opens a browser with the YouTube URL.
     *
     * @param videoId The ID of the YouTube video to show.
     */
    private void openYouTubeVideo(String videoId) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://" + videoId));
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://www.youtube.com/watch?v=" + videoId));
            startActivity(intent);
        }
    }

    /**
     * Handles clicks on the review list error message (triggers loading the review list).
     *
     * @param view The clicked view.
     */
    @SuppressWarnings("unused")
    @OnClick(R.id.tv_review_list_error_msg)
    void handleReviewListErrorClick(View view) {
        loadReviewList(true);
    }

    private void loadReviewList(boolean forceReload) {
        setReviewListPendingView();

        if (forceReload) {
            getSupportLoaderManager()
                    .restartLoader(REVIEW_LIST_LOADER_ID, null, new ReviewListLoaderCallbacks());
        } else {
            getSupportLoaderManager()
                    .initLoader(REVIEW_LIST_LOADER_ID, null, new ReviewListLoaderCallbacks());
        }
    }

    private void setReviewListPendingView() {
        mReviewListLoadingIndicator.setVisibility(View.VISIBLE);
        mReviewListErrorMsg.setVisibility(View.GONE);
        mReviewListEmptyMsg.setVisibility(View.GONE);
        mReviewList.setVisibility(View.GONE);
    }

    /**
     * Update the UI after the review list was loaded. Displays a text message if there are no
     * reviews. Otherwise populates a LinearLayout with review entries.
     *
     * @param reviews MovieReviews result from TMDb API.
     */
    private void setReviewListLoadedView(MovieReviews reviews) {
        mReviewListLoadingIndicator.setVisibility(View.GONE);
        mReviewListErrorMsg.setVisibility(View.GONE);
        mReviewListEmptyMsg.setVisibility(View.GONE);
        mReviewList.setVisibility(View.GONE);

        int reviewCount = 0;
        mReviewList.removeAllViews();

        for (MovieReviewDetails details : reviews.getMovieReviewDetails()) {
            String author = details.getAuthor();
            String content = details.getContent();

            if (TextUtils.isEmpty(author) || TextUtils.isEmpty(content)) continue;

            reviewCount++;

            View listItem =
                    getLayoutInflater().inflate(R.layout.review_list_item, mReviewList, false);

            TextView authorTv = (TextView) listItem.findViewById(R.id.tv_review_list_item_author);
            authorTv.setText(author);

            TextView contentTv = (TextView) listItem.findViewById(R.id.tv_review_list_item_content);
            contentTv.setText(StringUtils.removeCarriageReturns(content));

            mReviewList.addView(listItem);
        }

        if (reviewCount == 0) {
            mReviewListEmptyMsg.setVisibility(View.VISIBLE);
        } else {
            mReviewList.setVisibility(View.VISIBLE);
        }
    }

    private void setReviewListErrorView() {
        mReviewListLoadingIndicator.setVisibility(View.GONE);
        mReviewListErrorMsg.setVisibility(View.VISIBLE);
        mReviewListEmptyMsg.setVisibility(View.GONE);
        mReviewList.setVisibility(View.GONE);
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

    /**
     * Class for handling video list loading.
     */
    private class VideoListLoaderCallbacks implements
            LoaderManager.LoaderCallbacks<SingleLoader.Result<MovieVideos>> {

        @Override
        public Loader<SingleLoader.Result<MovieVideos>> onCreateLoader(int id, Bundle args) {
            TmdbApiClient apiClient = TmdbApiClientFactory.createApiClient();
            Single<MovieVideos> request = apiClient.getMovieVideos(mMovieId);

            return new SingleLoader<>(MovieDetailsActivity.this, request);
        }

        @Override
        public void onLoadFinished(Loader<SingleLoader.Result<MovieVideos>> loader,
                                   SingleLoader.Result<MovieVideos> data) {
            if (data.isError()) {
                Log.e(TAG, "failed to load video list: " + data.getError());
                setVideoListErrorView();
            } else {
                setVideoListLoadedView(data.getResult());
            }
        }

        @Override
        public void onLoaderReset(Loader<SingleLoader.Result<MovieVideos>> loader) {}

    }

    /**
     * Class for handling review loading.
     */
    private class ReviewListLoaderCallbacks implements
            LoaderManager.LoaderCallbacks<SingleLoader.Result<MovieReviews>> {

        @Override
        public Loader<SingleLoader.Result<MovieReviews>> onCreateLoader(int id, Bundle args) {
            TmdbApiClient apiClient = TmdbApiClientFactory.createApiClient();
            Single<MovieReviews> request = apiClient.getMovieReviews(mMovieId);

            return new SingleLoader<>(MovieDetailsActivity.this, request);
        }

        @Override
        public void onLoadFinished(Loader<SingleLoader.Result<MovieReviews>> loader,
                                   SingleLoader.Result<MovieReviews> data) {
            if (data.isError()) {
                Log.e(TAG, "failed to load reviews: " + data.getError());
                setReviewListErrorView();
            } else {
                setReviewListLoadedView(data.getResult());
            }
        }

        @Override
        public void onLoaderReset(Loader<SingleLoader.Result<MovieReviews>> loader) {}

    }

}
