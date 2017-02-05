/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import biz.smk.popularmovies.data.MovieListing;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Main activity with poster grid view.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final MovieListing.Type DEFAULT_MOVIE_LISTING_TYPE = MovieListing.Type.POPULAR;
    private static final String PREFS_FIELD_SELECTED_MOVIE_LISTING = "selected_movie_listing";
    private MovieListing.Type mMovieListingType;
    private MovieListing.Type mPreviousMovieListingType;

    private static final String STATE_FIELD_LAYOUT_MANAGER_STATE = "layout_manager_state";
    private Parcelable mLayoutManagerState;

    private PosterGridAdapter mPosterGridAdapter;
    private boolean mFavoriteMoviesChanged = false;

    @BindView(R.id.rv_main_poster_grid) RecyclerView mPosterGridView;
    @BindView(R.id.pb_main_poster_grid_loading_indicator) ProgressBar mLoadingIndicatorView;
    @BindView(R.id.tv_main_poster_grid_error_msg) TextView mPosterGridErrorMsgView;

    @BindString(R.string.activity_main_movie_listing_selection_title) String mMovieListingSelectionTitle;
    @BindString(R.string.activity_main_option_show_most_popular_first) String mOptionMostPopularFirstString;
    @BindString(R.string.activity_main_option_show_top_rated_first) String mOptionTopRatedFirstString;

    /**
     * Initializes the UI.
     *
     * @param savedInstanceState {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Context context = this;

        // Setup recycler view for poster grid.
        mPosterGridView.setHasFixedSize(true);

        // Setup layout manager for poster grid.
        PosterGridLayoutManager posterGridLayoutManager = new PosterGridLayoutManager(context);
        mPosterGridView.setLayoutManager(posterGridLayoutManager);

        // Setup adapter for poster grid.
        mPosterGridAdapter = new PosterGridAdapter();
        mPosterGridView.setAdapter(mPosterGridAdapter);

        // Add item decoration to poster grid.
        mPosterGridView.addItemDecoration(new PosterGridItemDecoration(context));

        // Load movie listing type from shared preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String listingType = preferences.getString(PREFS_FIELD_SELECTED_MOVIE_LISTING,
                DEFAULT_MOVIE_LISTING_TYPE.toString());
        setMovieListingType(MovieListing.Type.valueOf(listingType));

        // Register to the EventBus.
        EventBus.getDefault().register(this);
    }

    /**
     * Unregisters from the EventBus.
     */
    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    /**
     * Inflates the options menu.
     *
     * @param menu {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);
        return true;
    }

    /**
     * Updates the options menu to reflect the current state.
     *
     * @param menu {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem selectListing = menu.findItem(R.id.action_select_movie_listing);
        MenuItem showFavorites = menu.findItem(R.id.action_show_favorite_movies);

        if (mMovieListingType == MovieListing.Type.FAVORITES) {
            selectListing.setVisible(false);
            showFavorites.setIcon(R.drawable.ic_star_white_24dp);
        } else {
            selectListing.setVisible(true);
            showFavorites.setIcon(R.drawable.ic_star_border_white_24dp);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Handles options menu item selections.
     *
     * @param item {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_select_movie_listing:
                showMovieListingSelectionDialog();
                return true;
            case R.id.action_show_favorite_movies:
                toggleShowFavoriteMovies();
                return true;
            case R.id.action_about:
                Intent aboutActivityIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutActivityIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Shows an alert dialog which allows to choose the movie listing type.
     */
    private void showMovieListingSelectionDialog() {
        String[] options = {
                mOptionMostPopularFirstString,
                mOptionTopRatedFirstString
        };

        int selectedOption = 0;
        switch(mMovieListingType) {
            case POPULAR:
                selectedOption = 0;
                break;
            case TOP_RATED:
                selectedOption = 1;
                break;
            default:
                Log.e(TAG, "Unknown movie listing type: " + mMovieListingType.toString());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(mMovieListingSelectionTitle);
        builder.setSingleChoiceItems(options, selectedOption,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedOption) {
                        dialog.dismiss();
                        switch(selectedOption){
                            case 0:
                                setMovieListingType(MovieListing.Type.POPULAR);
                                break;
                            case 1:
                                setMovieListingType(MovieListing.Type.TOP_RATED);
                                break;
                        }
                    }
                });
        builder.show();
    }

    /**
     * Toggles displaying the users favorite movies.
     */
    private void toggleShowFavoriteMovies() {
        if (mMovieListingType == MovieListing.Type.FAVORITES) {
            if (mPreviousMovieListingType != null) {
                setMovieListingType(mPreviousMovieListingType);
            } else {
                setMovieListingType(DEFAULT_MOVIE_LISTING_TYPE);
            }
        } else {
            setMovieListingType(MovieListing.Type.FAVORITES);
        }
    }

    /**
     * Sets the movie listing type. This changes the toolbar title and updates the poster grid
     * adapter. The type is persisted in the shared preferences.
     *
     * @param type The listing type.
     */
    private void setMovieListingType(MovieListing.Type type) {
        ActionBar toolbar = getSupportActionBar();
        if (toolbar != null) {
            switch (type) {
                case POPULAR:
                    toolbar.setTitle(R.string.activity_main_title_popular);
                    break;
                case TOP_RATED:
                    toolbar.setTitle(R.string.activity_main_title_top_rated);
                    break;
                case FAVORITES:
                    toolbar.setTitle(R.string.activity_main_title_favorites);
                    break;
            }
        }

        mPreviousMovieListingType = mMovieListingType;
        mMovieListingType = type;

        invalidateOptionsMenu();
        mPosterGridAdapter.setMovieListingType(mMovieListingType);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putString(PREFS_FIELD_SELECTED_MOVIE_LISTING, type.toString()).apply();
    }

    /**
     * Handles clicks on the error message that is shown when the adapter could not load data.
     *
     * @param view The clicked text view.
     */
    @SuppressWarnings("unused")
    @OnClick(R.id.tv_main_poster_grid_error_msg)
    void handleClick(View view) {
        mPosterGridAdapter.refresh();
    }

    /**
     * Handles favorite movies changed events.
     *
     * @param event The event.
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPosterGridAdapterStatusChanged(MovieListing.FavoriteMoviesChangedEvent event) {
        mFavoriteMoviesChanged = true;
    }

    /**
     * Update poster grid adapter when favorite movies listing changed.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mFavoriteMoviesChanged && mMovieListingType == MovieListing.Type.FAVORITES) {
            mPosterGridAdapter.setMovieListingType(MovieListing.Type.FAVORITES);
            mFavoriteMoviesChanged = false;
        }
    }

    /**
     * Save poster grid layout manager state so it can be restored if the adapter needs to be
     * refreshed on resume.
     */
    @Override
    protected void onPause() {
        mLayoutManagerState = mPosterGridView.getLayoutManager().onSaveInstanceState();
        super.onPause();
    }

    /**
     * Handles poster grid adapter status change events.
     *
     * @param event The event.
     */
    @SuppressWarnings("unused")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onPosterGridAdapterStatusChanged(PosterGridAdapter.StatusChangedEvent event) {
        switch(event.newStatus) {
            case PENDING:
                setPosterGridPendingView();
                break;
            case READY:
                setPosterGridReadyView();
                break;
            default:
                setPosterGridErrorView();
        }
    }

    /**
     * Handles poster click events.
     *
     * @param event The event.
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMoviePosterClicked(PosterGridAdapter.PosterClickedEvent event) {
        Intent movieDetailsActivityIntent = new Intent(this, MovieDetailsActivity.class);

        movieDetailsActivityIntent.putExtra(MovieDetailsActivity.EXTRAS_FIELD_MOVIE_ID,
                event.movieId);
        movieDetailsActivityIntent.putExtra(MovieDetailsActivity.EXTRAS_FIELD_FROM_LISTING_TYPE,
                mMovieListingType.toString());
        movieDetailsActivityIntent.putExtra(MovieDetailsActivity.EXTRAS_FIELD_LISTING_POSITION,
                event.adapterPosition + 1); // adapter position is zero-based

        startActivity(movieDetailsActivityIntent);
    }

    private void setPosterGridPendingView() {
        mLoadingIndicatorView.setVisibility(View.VISIBLE);
        mPosterGridErrorMsgView.setVisibility(View.GONE);
    }

    /**
     * Sets the UI to ready state. If a layout manager state was saved, it is restored here (e.g.
     * scroll offset), because at this point the adapter will have its total count set.
     */
    private void setPosterGridReadyView() {
        mLoadingIndicatorView.setVisibility(View.GONE);
        mPosterGridErrorMsgView.setVisibility(View.GONE);

        if (mLayoutManagerState != null) {
            mPosterGridView.getLayoutManager().onRestoreInstanceState(mLayoutManagerState);
            mLayoutManagerState = null;
        }
    }

    private void setPosterGridErrorView() {
        mLoadingIndicatorView.setVisibility(View.GONE);
        mPosterGridErrorMsgView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Parcelable layoutManagerState = mPosterGridView.getLayoutManager().onSaveInstanceState();
        outState.putParcelable(STATE_FIELD_LAYOUT_MANAGER_STATE, layoutManagerState);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mLayoutManagerState = savedInstanceState.getParcelable(STATE_FIELD_LAYOUT_MANAGER_STATE);
    }

}
