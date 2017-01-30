/**
 * Copyright (C) 2017 Stefan Kleeschulte
 */

package biz.smk.popularmovies.tmdbapi;

import android.content.Context;
import android.os.SystemClock;

import java.io.IOException;

import biz.smk.popularmovies.Application;
import biz.smk.popularmovies.BuildConfig;
import biz.smk.popularmovies.R;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.schedulers.Schedulers;

/**
 * Creates new TMDb API clients with Retrofit 2.
 */
public class TmdbApiClientFactory {

    private static final boolean ENABLE_DEBUG_OUTPUT = true;

    private static final String API_BASE_URL = "https://api.themoviedb.org/3/";
    private static final String API_KEY_PARAM = "api_key";

    /**
     * When the API response indicates that there were too many requests (status code 429), the
     * request will be retried up to MAX_RETRY_COUNT times, but only if the Retry-After header in
     * the response is <= MAX_RETRY_AFTER (seconds).
     */
    private static final int MAX_RETRY_COUNT = 2;
    private static final int MAX_RETRY_AFTER = 10;

    private static String sApiKey;

    private static String getApiKey() {
        if (sApiKey == null) {
            Context appContext = Application.getContext();
            sApiKey = appContext.getString(R.string.themoviedb_api_key);
        }
        return sApiKey;
    }

    /**
     * Returns an TMDb Retrofit API client that executes requests on RxJava Schedulers.io() threads
     * and handles 429 (Too Many Requests) API responses with retries.
     *
     * @return TMDb API client.
     */
    public static TmdbApiClient createApiClient() {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        clientBuilder.addInterceptor(getApiKeyInterceptor());
        clientBuilder.addInterceptor(getRetryOn429Interceptor());

        if (BuildConfig.DEBUG && ENABLE_DEBUG_OUTPUT) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            clientBuilder.addInterceptor(loggingInterceptor);
        }

        OkHttpClient client = clientBuilder.build();

        RxJavaCallAdapterFactory callAdapterFactory =
                RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io());

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(callAdapterFactory)
                .build();

        return retrofit.create(TmdbApiClient.class);
    }

    /**
     * Returns an OkHttp 3 interceptor that fills in the request with the API key if there is an
     * (possibly empty) api key query parameter.
     *
     * @return Api key injection intercepter.
     */
    private static Interceptor getApiKeyInterceptor() {
        return new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                HttpUrl url = request.url();

                // If there is at least one query parameter named API_KEY_PARAM, remove them all and
                // add one parameter with its value set to getApiKey's return value.
                if (url.queryParameter(API_KEY_PARAM) != null) {
                    url = url.newBuilder()
                            .removeAllQueryParameters(API_KEY_PARAM)
                            .addQueryParameter(API_KEY_PARAM, getApiKey())
                            .build();
                }

                request = request.newBuilder().url(url).build();
                return chain.proceed(request);
            }
        };
    }

    /**
     * Returns an OkHttp 3 interceptor that handles 429 (Too Many Requests) API responses with a
     * limited number of retries.
     *
     * @return Status code 429 handling interceptor.
     */
    private static Interceptor getRetryOn429Interceptor() {
        return new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                return process(chain, 0);
            }

            /**
             * Recursively requests until no 429 is returned or the retry count is reached or the
             * Retry-After value is missing or too high.
             *
             * @param chain Interceptor chain.
             * @param retryCount Number of retries so far (for recursion).
             * @return Response of the retry.
             * @throws IOException If chain.proceed fails.
             */
            private Response process(Chain chain, int retryCount) throws IOException {
                Response response = chain.proceed(chain.request());

                // No processing if the status code is != 429
                if (response.code() != 429) return response;

                // No processing if the maximum retry count is exceeded.
                if (retryCount > MAX_RETRY_COUNT) return response;

                // No processing if the Retry-After value is missing or too high.
                int retryAfter = getRetryAfter(response);
                if (retryAfter == -1 || retryAfter > MAX_RETRY_AFTER) return response;

                // Retrofit requests run in their own thread, so it is ok to sleep the thread here.
                // It is the only solution when using an interceptor, because OkHttp 3
                // interceptors work synchronously.
                SystemClock.sleep(retryAfter * 1000);

                return process(chain, retryCount + 1);
            }

            /**
             * Extracts the value of the Retry-After header.
             *
             * @param response The responsen from which to extract the value.
             * @return The Retry-After value or -1 on failure.
             */
            private int getRetryAfter(Response response) {
                int retryAfter;

                try {
                    retryAfter = Integer.parseInt(response.header("Retry-After"));
                } catch (NumberFormatException e) {
                    return -1;
                }

                if (retryAfter < 0) return -1;
                return retryAfter;
            }
        };
    }

}
