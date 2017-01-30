/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Decorator for poster grid items (views). Used for adding spacing between the items.
 */
class PosterGridItemDecoration extends RecyclerView.ItemDecoration {

    private int mSpacing;

    /**
     * Initializes a new decorator.
     *
     * @param context Context.
     */
    PosterGridItemDecoration(Context context) {
        super();

        mSpacing = (int) context.getResources().getDimension(R.dimen.poster_grid_spacing);
    }

    /**
     * Add equally distributed spacing around all items.
     *
     * @param outRect {@inheritDoc}
     * @param view {@inheritDoc}
     * @param parent {@inheritDoc}
     * @param state {@inheritDoc}
     */
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        PosterGridLayoutManager layoutManager = (PosterGridLayoutManager) parent.getLayoutManager();
        int spanCount = layoutManager.getSpanCount();

        // Top spacing only for first row
        if (position < spanCount) {
            outRect.top = mSpacing;
        } else {
            outRect.top = 0;
        }

        // Adjust the left and right spacing of each item so that the combined spacing of all items
        // in a row is equal in each 'spacing-column'.
        int positionInRow = position % spanCount; // first element: 0
        outRect.left = Math.round(mSpacing * (1 - (float) positionInRow / spanCount));
        outRect.right = Math.round(mSpacing * ((float) (positionInRow + 1) / spanCount));

        outRect.bottom = mSpacing;
    }

}
