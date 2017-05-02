package com.srmgmail.anand.kr.sunshine;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.srmgmail.anand.kr.sunshine.data.SunshinePreferences;
import com.srmgmail.anand.kr.sunshine.utilities.NetworkUtils;
import com.srmgmail.anand.kr.sunshine.utilities.OpenWeatherJsonUtils;

import java.net.URL;

public class MainActivity extends AppCompatActivity implements ForecastAdapter.ForecastAdapterOnClickHandler,
        LoaderCallbacks<String[]>, SharedPreferences.OnSharedPreferenceChangeListener {

    protected ForecastAdapter mForecastAdapter;

    protected RecyclerView mRecyclerView;

    protected TextView mErrorMessageDisplay;

    protected ProgressBar mLoadingIndicator;

    private String TAG = MainActivity.class.getSimpleName();

    private static final int FORECAST_LOADER_ID = 0;

    private static boolean PREFERENCES_HAVE_BEEN_UPDATED = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_forecast);

        mErrorMessageDisplay = (TextView) findViewById(R.id.tv_error_message_display);

        int recyclerViewOrientation = LinearLayout.VERTICAL;

        boolean shouldReversLayout = false;

        LinearLayoutManager layoutManager = new LinearLayoutManager
                (this, recyclerViewOrientation, shouldReversLayout);

        mRecyclerView.setLayoutManager(layoutManager);

        /** LinearLayoutManager layoutManager = new LinearLayoutManager(this , LinearLayoutManager.VERTICAL , false);
         mRecyclerView.setLayoutManager(layoutManager);
         **/

        mRecyclerView.setHasFixedSize(true);

        mForecastAdapter = new ForecastAdapter(this);

        mRecyclerView.setAdapter(mForecastAdapter);

        mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_loading_indicator);

        int loaderId = FORECAST_LOADER_ID;

        LoaderCallbacks<String[]> callbacks = MainActivity.this;

        Bundle bundleForLoader = null;

        getSupportLoaderManager().initLoader(loaderId, bundleForLoader, callbacks);

        Log.d(TAG, "onCreate : registring preference changed listener");

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

    }


    private void showErrorMessage() {
        mRecyclerView.setVisibility(View.INVISIBLE);
        mErrorMessageDisplay.setVisibility(View.VISIBLE);
    }

    @Override
    public Loader<String[]> onCreateLoader(int id, final Bundle LoaderArgs) {
        return new AsyncTaskLoader<String[]>(this) {

            String[] mWeatherData = null;

            @Override
            public void onStartLoading() {
                if (mWeatherData != null) {
                    deliverResult(mWeatherData);
                } else {
                    mLoadingIndicator.setVisibility(View.VISIBLE);
                    forceLoad();
                }
            }

            @Override
            public String[] loadInBackground() {

                String locationQuery = SunshinePreferences
                        .getPreferredWeatherLocation(MainActivity.this);

                URL weatherRequestUrl = NetworkUtils.buildUrl(locationQuery);

                try {
                    String jsonWeatherResponse = NetworkUtils
                            .getResponseFromHttpUrl(weatherRequestUrl);

                    String[] simpleJsonWeatherData = OpenWeatherJsonUtils
                            .getSimpleWeatherStringsFromJson(MainActivity.this, jsonWeatherResponse);

                    return simpleJsonWeatherData;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }

            }

            public void deliverResult(String[] data) {
                mWeatherData = data;
                super.deliverResult(data);
            }

        };
    }

    @Override
    public void onLoadFinished(Loader<String[]> loader, String[] data) {
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        mForecastAdapter.setWeatherData(data);
        if (null == data) {
            showErrorMessage();
        } else {
            showWeatherDataView();
        }
    }

    @Override
    public void onLoaderReset(Loader<String[]> loader) {
        // Not Using this one
    }

    private void invalidateData() {
        mForecastAdapter.setWeatherData(null);
    }


    private void openLoactionInMap() {
        String addressString = "1600 Ampitheatre Parkway, CA";
        Uri geoLocation = Uri.parse("geo:0,0?q=" + addressString);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(TAG, "Couldn't call" + geoLocation.toString() + ",no recieving" +
                    "app Installed");
        }
    }

    @Override
    public void onClick(String weatherForDay) {
        Context context = this;
        Class destinationClass = DetailActivity.class;
        Intent intentToStartDetailActivity = new Intent(context, destinationClass);
        intentToStartDetailActivity.putExtra(Intent.EXTRA_TEXT, weatherForDay);
        startActivity(intentToStartDetailActivity);
    }

    private void showWeatherDataView() {
        /* First, make sure the error is invisible */
        mErrorMessageDisplay.setVisibility(View.INVISIBLE);
        /* Then, make sure the weather data is visible */
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (PREFERENCES_HAVE_BEEN_UPDATED) {
            Log.d(TAG, "onStart: preferences were updated");
            getSupportLoaderManager().restartLoader(FORECAST_LOADER_ID, null, this);
            PREFERENCES_HAVE_BEEN_UPDATED = false;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        PREFERENCES_HAVE_BEEN_UPDATED = true;

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.forecast, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            invalidateData();
            getSupportLoaderManager().restartLoader(FORECAST_LOADER_ID, null, this);
            return true;
        }

        if (id == R.id.action_map) {
            openLoactionInMap();
            return true;
        }

        if (id == R.id.action_settings) {
            Intent startSettingActivity = new Intent(this, SettingsActivity.class);
            startActivity(startSettingActivity);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
