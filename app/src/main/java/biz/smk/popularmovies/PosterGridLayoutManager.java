/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * Layout manager for the poster grid. Automatically adjusts the column count depending on its
 * width.
 */
class PosterGridLayoutManager extends GridLayoutManager {

    private static final int INITIAL_SPAN_COUNT = 2;

    private float mMinColumnWidth;
    private int mCurrentSpanCount;

    /**
     * Initializes a new layout manager.
     *
     * @param context Context.
     */
    PosterGridLayoutManager(Context context) {
        super(context, INITIAL_SPAN_COUNT);

        mMinColumnWidth = context.getResources().getDimension(R.dimen.poster_grid_min_column_width);
        mCurrentSpanCount = INITIAL_SPAN_COUNT;
    }

    /**
     * Calculate span count when view has dimensions.
     *
     * @param state {@inheritDoc}
     */
    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);

        int width = getWidth();
        int spanCount = Math.max((int) (width / mMinColumnWidth), 1);

        if (spanCount != mCurrentSpanCount) {
            mCurrentSpanCount = spanCount;
            setSpanCount(spanCount);
        }
    }
}
