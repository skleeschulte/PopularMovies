/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies;

import android.content.Context;

/**
 * Application class - useful to access the application context from anywhere.
 */
public class Application extends android.app.Application {

    private static Application sInstance;

    /**
     * Initialize the Application class.
     *
     * Initializing static variables here is ok because: "The Application class, or your subclass of
     * the Application class, is instantiated before any other class when the process for your
     * application/package is created."
     * (https://developer.android.com/reference/android/app/Application.html)
     */
    public Application() {
        sInstance = this;
    }

    /**
     * Returns the application context.
     *
     * @return The application context.
     */
    public static Context getContext() {
        return sInstance.getApplicationContext();
    }

}
