/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies.utilities;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;

import rx.Observer;
import rx.Single;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Loader for loading RxJava Singles. The Single will be explicitly observed on the main thread, but
 * it is not changed where the Single is subscribed. This needs to be handled externally.
 */
public class SingleLoader<T> extends Loader<SingleLoader.Result<T>> implements Observer<T> {

    private Single<T> mSingle;
    private Subscription mSubscription;
    private Result<T> mResult;

    /**
     * Stores away the application context associated with context.
     * Since Loaders can be used across multiple activities it's dangerous to
     * store the context directly; always use {@link #getContext()} to retrieve
     * the Loader's Context, don't use the constructor argument directly.
     * The Context returned by {@link #getContext} is safe to use across
     * Activity instances.
     *
     * @param context used to retrieve the application context.
     * @param single The Single to load. The Single will be explicitly observed on the main thread,
     *               but it is not changed where the Single is subscribed. This needs to be handled
     *               externally.
     */
    public SingleLoader(Context context, Single<T> single) {
        super(context);
        mSingle = single;
    }

    /**
     * Subscribe to the Single and observe on Main Thread.
     */
    @Override
    protected void onStartLoading() {
        if (mResult != null) {
            deliverResult(mResult);
        } else if (mSubscription == null) {
            mSubscription = mSingle
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this);
        }
    }

    /**
     * Cancels the Single subscription.
     */
    @Override
    protected void onStopLoading() {
        if (mSubscription != null) {
            mSubscription.unsubscribe();
            mSubscription = null;
        }
    }

    /**
     * Cancels the Single subscription, deletes cached data and re-subscribed to the Single.
     */
    @Override
    protected void onForceLoad() {
        onReset();
        onStartLoading();
    }

    /**
     * Cancels the Single subscription and deletes cached data.
     */
    @Override
    protected void onReset() {
        onStopLoading();
        mResult = null;
    }

    /**
     * Cancels the subscription if there is one.
     *
     * @return {@inheritDoc}
     */
    @Override
    protected boolean onCancelLoad() {
        if (mSubscription != null) {
            onStopLoading();
            return true;
        }
        return false;
    }

    /**
     * Called when the Single completes.
     */
    @Override
    public void onCompleted() {
        onStopLoading();
        deliverResult(mResult);
    }

    /**
     * Called when the Single fails.
     *
     * @param e {@inheritDoc}
     */
    @Override
    public void onError(Throwable e) {
        onStopLoading();
        mResult = new Result<>(null, e);
        deliverResult(mResult);
    }

    /**
     * This should be called only once as this class is working with Singles.
     *
     * @param r {@inheritDoc}
     */
    @Override
    public void onNext(T r) {
        mResult = new Result<>(r, null);
    }

    /**
     * Class for holding results of the Single subscription - either the result of the Single or a
     * Throwable.
     *
     * @param <T> Type of the Single result.
     */
    public static class Result<T> {

        private final T mResult;
        private final Throwable mError;

        private Result(@Nullable T result, @Nullable Throwable error) {
            if (result == null && error == null) {
                throw new IllegalArgumentException("either result or error must be not null");
            }

            mResult = result;
            mError = error;
        }

        /**
         * Returns the result of the Single or null if this is an error result.
         *
         * @return Single result or null.
         */
        public T getResult() {
            return mResult;
        }

        /**
         * Returns the error the Single resulted in or null if the Single was successfully resoled.
         *
         * @return Throwable or null.
         */
        public Throwable getError() {
            return mError;
        }

        /**
         * Returns true if this is an error result, otherwise false.
         *
         * @return True if is error, otherwise false.
         */
        public boolean isError() {
            return mError != null;
        }

    }

}
