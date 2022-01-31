package com.xamk.weatherapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.RemoteViews
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.AppWidgetTarget
import com.google.android.gms.ads.*
import com.xamk.weatherapp.ui.main.SectionsPagerAdapter
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class Forecast(
    val city: String,
    val condition: String,
    val temperature: String,
    val time: String,
    val icon: String
)

class WeatherAppWidgetProvider : AppWidgetProvider() {

    // example call is : https://api.openweathermap.org/data/2.5/weather?q=Jyväskylä&APPID=YOUR_API_KEY&units=metric
    val API_LINK: String = "https://api.openweathermap.org/data/2.5/weather?q="
    val API_ICON: String = "https://openweathermap.org/img/w/"
    val API_KEY: String = "861c1edd0efc550a9e57ef8be7faf8f6"

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        // Perform this loop procedure for each App Widget that belongs to this provider
        appWidgetIds.forEach { appWidgetId ->
            ;
            // continue coding here...
            // Create an Intent to launch MainActivity
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

            // Get the layout for the App Widget and attach an on-click listener
            val views = RemoteViews(context.packageName, R.layout.weather_appwidget)
            views.setOnClickPendingIntent(R.id.cityTextView, pendingIntent)

            // create intent
            val refreshIntent = Intent(context, WeatherAppWidgetProvider::class.java)
            refreshIntent.action = "com.example.weatherapp.REFRESH"
            refreshIntent.putExtra("appWidgetId", appWidgetId)
            // create pending intent
            val refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            // set pending intent to refresh image view
            views.setOnClickPendingIntent(R.id.refreshImageView, refreshPendingIntent)

            // Load weather forecast
            loadWeatherForecast("Jyväskylä", context, views, appWidgetId, appWidgetManager)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // got a new action, check if it is refresh action
        if (intent.action == "com.example.weatherapp.REFRESH") {
            // get manager
            val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)
            // get views
            val views = RemoteViews(context.packageName, R.layout.weather_appwidget)
            // get appWidgetId
            val appWidgetId = intent.extras!!.getInt("appWidgetId")
            // load data again
            loadWeatherForecast("Jyväskylä", context, views, appWidgetId, appWidgetManager)
        }

    }

    private fun loadWeatherForecast(
        city: String,
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        appWidgetManager: AppWidgetManager
    ) {
        // URL to load forecast
        val url = "$API_LINK$city&APPID=$API_KEY&units=metric"

        // continue coding here...
        // JSON object request with Volley
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null, Response.Listener<JSONObject> { response ->
                try {
                    // load OK - parse data from the loaded JSON
                    // **add parse codes here... described later**
                    val mainJSONObject = response.getJSONObject("main")
                    val weatherArray = response.getJSONArray("weather")
                    val firstWeatherObject = weatherArray.getJSONObject(0)

                    // city, condition, temperature
                    val city = response.getString("name")
                    val condition = firstWeatherObject.getString("main")
                    val temperature = mainJSONObject.getString("temp") + " °C"
                    // time
                    val weatherTime: String = response.getString("dt")
                    val weatherLong: Long = weatherTime.toLong()
                    val formatter: DateTimeFormatter =
                        DateTimeFormatter.ofPattern("dd.MM.YYYY HH:mm:ss")
                    val dt = Instant.ofEpochSecond(weatherLong).atZone(ZoneId.systemDefault())
                        .toLocalDateTime().format(formatter).toString()

                    views.setTextViewText(R.id.cityTextView, city)
                    views.setTextViewText(R.id.condTextView, condition)
                    views.setTextViewText(R.id.tempTextView, temperature)
                    views.setTextViewText(R.id.timeTextView, dt)

                    // AppWidgetTarget will be used with Glide - image target view
                    val awt: AppWidgetTarget = object : AppWidgetTarget(
                        context.applicationContext,
                        R.id.iconImageView,
                        views,
                        appWidgetId
                    ) {}
                    val weatherIcon = firstWeatherObject.getString("icon")
                    val url = "$API_ICON$weatherIcon.png"

                    Glide
                        .with(context)
                        .asBitmap()
                        .load(url)
                        .into(awt)

                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.d("WEATRHER", "***** error: $e")
                }
            },
            Response.ErrorListener { error -> Log.d("ERROR", "Error: $error") })
        // start loading data with Volley
        val queue = Volley.newRequestQueue(context)
        queue.add(jsonObjectRequest)
    }
}


class MainActivity : AppCompatActivity() {

    // example call is :
    // https://api.openweathermap.org/data/2.5/weather?q=Jyväskylä&APPID=YOUR_API_KEY&units=metric&lang=fi
    val API_LINK: String = "https://api.openweathermap.org/data/2.5/weather?q="
    val API_ICON: String = "https://openweathermap.org/img/w/"
    val API_KEY: String = "861c1edd0efc550a9e57ef8be7faf8f6"

    // add a few test cities
    val cities: MutableList<String> = mutableListOf("Jyväskylä", "Helsinki", "Oulu", "New York", "Tokyo")
    // city index, used when data will be loaded
    var index: Int = 0

    companion object {
        var forecasts: MutableList<Forecast> = mutableListOf()
    }

    lateinit var mAdView : AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Load ads
        loadBannerad()

        // Load weather forecasts
        loadWeatherForecast(cities[index])
        // ...

        // ..
    }

    private fun loadBannerad() {

        MobileAds.initialize(this) {}

        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        mAdView.adListener = object: AdListener() {
            override fun onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                Toast.makeText(this@MainActivity,"Ad Loaded",Toast.LENGTH_SHORT).show()
            }

            override fun onAdFailedToLoad(adError : LoadAdError) {
                // Code to be executed when an ad request fails.
            }

            override fun onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }

            override fun onAdClicked() {
                // Code to be executed when the user clicks on an ad.
            }

            override fun onAdClosed() {
                Toast.makeText(this@MainActivity, "Ad Done", Toast.LENGTH_SHORT).show()
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }
        }

    }



    private fun setUI() {
        // hide progress bar
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.INVISIBLE
        // add adapber
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
    }
    // load forecast
    private fun loadWeatherForecast(city:String) {
        // url for loading
        val url = "$API_LINK$city&APPID=$API_KEY&units=metric&lang=fi"

        // JSON object request with Volley
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null, { response ->
                try {
                    val mainJSONObject = response.getJSONObject("main")
                    val weatherArray = response.getJSONArray("weather")
                    val firstWeatherObject = weatherArray.getJSONObject(0)
                    // load OK - parse data from the loaded JSON

                    // city, condition, temperature
                    val city = response.getString("name")
                    val condition = firstWeatherObject.getString("main")
                    val temperature = mainJSONObject.getString("temp")+" °C"
                    // time
                    val weatherTime: String = response.getString("dt")
                    val weatherLong: Long = weatherTime.toLong()
                    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.YYYY HH:mm:ss")
                    val dt = Instant.ofEpochSecond(weatherLong).atZone(ZoneId.systemDefault()).toLocalDateTime().format(formatter).toString()
                    // icon
                    val weatherIcon = firstWeatherObject.getString("icon")
                    val url = "$API_ICON$weatherIcon.png"

                    // ADD parse codes here... described later

                    // add forecast object to the list
                    forecasts.add(Forecast(city,condition,temperature,dt,url))
                    // use Logcat window to check that loading really works
                    Log.d("WEATHER", "**** weatherCity = " + forecasts[index].city)
                    // load another city if not loaded yet
                    if ((++index) < cities.size) loadWeatherForecast(cities[index])
                    else {
                        Log.d("WEATHER", "*** ALL LOADED!")
                        setUI()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.d("WEATHER", "***** error: $e")
                    // hide progress bar
                    val progressBar = findViewById<ProgressBar>(R.id.progressBar)
                    progressBar.visibility = View.INVISIBLE
                    // show Toast -> should be done better!!!
                    Toast.makeText(this, "Error loading weather forecast!", Toast.LENGTH_LONG)
                        .show()
                }
            },
            { error -> Log.d("PTM", "Error: $error") }
        )
        // start loading data with Volley
        val queue = Volley.newRequestQueue(applicationContext)
        queue.add(jsonObjectRequest)

    }
}

