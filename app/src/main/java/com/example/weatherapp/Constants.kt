package com.example.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities


object Constants {

    const val APP_ID:String="4143d7390a9ed39891f2e6f56ac91bcc"
    const val BASE_URL:String="https://api.openweathermap.org/data/"
    const val METRIC_UNIT="metric"
    const val PREFERENCE_NAME ="WeatherAppPreference"
    const val WEATHER_RESPONSE_DATA = "weather_response_data"

      fun isNetworkAvailable(context:Context):Boolean{

          val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

          val network = connectivityManager.activeNetwork ?: return false
          val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

          return when {
              activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
              activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->true
              activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
              else -> false

          }

      }




}