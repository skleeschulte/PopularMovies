/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;

import biz.smk.popularmovies.data.MovieListing;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * RecyclerView adapter for the movie posters grid. The adapter is "ready" as soon as it received
 * the total count of the assigned movie listing. The item data (movieId and movie poster) is loaded
 * and cached on the fly when scrolling through the items.
 */
class PosterGridAdapter extends RecyclerView.Adapter<PosterGridViewHolder> {

    private static final String TAG = "PosterGridAdapter";

    private MovieListing mMovieListing;
    private Subscription mTotalCountSubscription;
    private int mTotalCount;
    private int mViewHolderCount = 0;

    /**
     * Sets the type of the movie listing.
     *
     * @param type Desired listing type.
     */
    void setMovieListingType(MovieListing.Type type) {
        mMovieListing = MovieListing.getListing(type);
        mTotalCount = 0;

        notifyDataSetChanged();
        getTotalCount();
    }

    /**
     * Requests the total count from the movie listing. This will emit sticky status events
     * (pending, ready, error).
     */
    private void getTotalCount() {
        if (mTotalCountSubscription != null) {
            mTotalCountSubscription.unsubscribe();
        }

        Log.d(TAG, "Requesting total count of movie listing " + mMovieListing.getType().toString());
        EventBus.getDefault().postSticky(new StatusChangedEvent(StatusChangedEvent.Status.PENDING));

        mTotalCountSubscription = mMovieListing.getTotalCount()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer totalCount) {
                        if (totalCount != mTotalCount) {
                            mTotalCount = totalCount;
                            Log.d(TAG, "Received total count of movie listing " +
                                    mMovieListing.getType().toString() + ": " + totalCount);
                            notifyDataSetChanged();
                            EventBus.getDefault().postSticky(
                                    new StatusChangedEvent(StatusChangedEvent.Status.READY));
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(TAG, "Could not get total count of movie listing " +
                                mMovieListing.getType().toString() + ": " + throwable.toString());
                        EventBus.getDefault().postSticky(
                                new StatusChangedEvent(StatusChangedEvent.Status.ERROR));
                    }
                });
    }

    /**
     * Refreshes the adapter by requesting a new total count.
     */
    void refresh() {
        getTotalCount();
    }

    @Override
    public PosterGridViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.poster_grid_item, parent, false);

        int number = ++mViewHolderCount;
        return new PosterGridViewHolder(view, number);
    }

    @Override
    public void onBindViewHolder(PosterGridViewHolder holder, int position) {
        holder.bind(mMovieListing, position);
    }

    @Override
    public void onViewRecycled(PosterGridViewHolder holder) {
        holder.unbind();
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return mTotalCount;
    }

    /**
     * Emitted when the adapter status changes.
     */
    static class StatusChangedEvent {

        /**
         * Enumeration of adapter status types.
         */
        enum Status { PENDING, READY, ERROR }

        /**
         * The new adapter status.
         */
        final Status newStatus;

        private StatusChangedEvent(Status newStatus) {
            this.newStatus = newStatus;
        }

    }

    /**
     * Emitted when a poster is clicked.
     */
    static class PosterClickedEvent {

        /**
         * The position of the clicked poster in the adapter.
         */
        final int adapterPosition;

        /**
         * The movie ID of the clicked poster.
         */
        final long movieId;

        PosterClickedEvent(int adapterPosition, long movieId) {
            this.adapterPosition = adapterPosition;
            this.movieId = movieId;
        }

    }

}
