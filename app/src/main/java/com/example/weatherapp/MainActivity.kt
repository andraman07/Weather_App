package com.example.weatherapp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.models.Weather
import com.example.weatherapp.models.WeatherResponse
import com.example.weatherapp.network.WeatherService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationClient:FusedLocationProviderClient
    private lateinit var customProgressDialog:Dialog
    private lateinit var binding: ActivityMainBinding
    private lateinit var mSharedPreferences :SharedPreferences

    companion object{
        private const val  LOCATION_PERMISSION_REQUEST_CODE=300
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mSharedPreferences= getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)
        setupUi()

        if(!isLocationEnabled()){
            Toast.makeText(this,
                "Your location provider  is turned off. Please turn it on",
                 Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }else{
              requestLocationData()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection.
        return when (item.itemId) {
            R.id.refresh -> {
                requestLocationData()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun requestLocationData(){

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mFusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        Log.d("Location", "Latitude: $latitude, Longitude: $longitude")

                        getLocationWeatherDetail(latitude,longitude)
                    } ?: run {
                        Log.e("Location", "No last known location found")
                    }

                }

        }else{

            locationPermission()

        }



    }

    private fun getLocationWeatherDetail(latitude:Double,longitude:Double){


             if(Constants.isNetworkAvailable(this)){
                 val retrofit:Retrofit=Retrofit.Builder()
                     .baseUrl(Constants.BASE_URL)
                     .addConverterFactory(GsonConverterFactory.create())
                     .build()

                 val service:WeatherService = retrofit.create(WeatherService::class.java)


                 showProgressDialog()

                     val listCall:Call<WeatherResponse> = service.getWeather(latitude,longitude,Constants.APP_ID,Constants.METRIC_UNIT)


                     listCall.enqueue(object : Callback<WeatherResponse>{
                     override fun onResponse(
                         call: Call<WeatherResponse>,
                         response: Response<WeatherResponse>
                     ) {
                         if(response.isSuccessful){
                             cancelProgressDialog()
                             val weatherList: WeatherResponse? = response.body()
                             val weatherJsonResponse = Gson().toJson(weatherList)
                             val editor = mSharedPreferences.edit()
                             editor.putString(Constants.WEATHER_RESPONSE_DATA, weatherJsonResponse)
                             editor.apply()
                           setupUi()
                         }else{

                             when(response.code()){
                                 400-> Log.e("Error 400","Bad Connection")
                                 404-> Log.e("Error 404","Not Found")
                             }

                         }
                     }

                     override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                         Log.e("Error:",t.message.toString())
                     }

                 })

             }else{

                 Toast.makeText(this,"No internet Connection Available",Toast.LENGTH_SHORT).show()

             }



    }


    private fun setupUi(){

          val weatherJsonResponse = mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA, null)

        if(!weatherJsonResponse.isNullOrEmpty()){

            val weatherList = Gson().fromJson(weatherJsonResponse, WeatherResponse::class.java)
            for(i in weatherList?.weather?.indices!!){
                binding.tvMainDescription.text = weatherList.weather[i].description
                binding.tvMain.text =weatherList.weather[i].main

                when(weatherList.weather[i].icon){

                    "01d" -> binding.ivMain.setImageResource(R.drawable.sunny)
                    "02d" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "03d" -> binding.ivMain.setImageResource(R.drawable.sunny)
                    "04d" -> binding.ivMain.setImageResource(R.drawable.sunny)
                    "09d"-> binding.ivMain.setImageResource(R.drawable.rain)
                    "10d" -> binding.ivMain.setImageResource(R.drawable.rain)
                    "11d" -> binding.ivMain.setImageResource(R.drawable.storm)
                    "13d" -> binding.ivMain.setImageResource(R.drawable.snowflake)
                    "01n" -> binding.ivMain.setImageResource(R.drawable.sunny)
                    "02n" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "03n" -> binding.ivMain.setImageResource(R.drawable.sunny)
                    "04n" -> binding.ivMain.setImageResource(R.drawable.sunny)
                    "09n"-> binding.ivMain.setImageResource(R.drawable.rain)
                    "10n" -> binding.ivMain.setImageResource(R.drawable.rain)
                    "11n" -> binding.ivMain.setImageResource(R.drawable.storm)
                    "13n" -> binding.ivMain.setImageResource(R.drawable.snowflake)


                }
            }

            "${weatherList.main.temp_min}${getUnit(application.resources.configuration.locales.toString())} min".also { binding.tvMin.text = it }
            "${weatherList.main.temp_max}${getUnit(application.resources.configuration.locales.toString())} max".also { binding.tvMax.text = it }
            "${weatherList.main.temp}${getUnit(application.resources.configuration.locales.toString())}".also { binding.tvTemp.text = it }

            "${weatherList.main.humidity} percent".also { binding.tvHumidity.text = it }
            binding.tvCountry.text = weatherList.sys.country
            binding.tvName.text = weatherList.name
            binding.tvSunriseTime.text = unixTime(weatherList.sys.sunrise)
            binding.tvSunsetTime.text = unixTime(weatherList.sys.sunset)
            binding.tvSpeed.text = String.format("%.2f", windSpeedConversion(weatherList.wind.speed))

        }







    }

    private fun windSpeedConversion(value:Double):Double{
             return (value/1000.0)*3600.0
    }
    private fun getUnit(value:String): String {
            var degreeValue = "°C"
           if("US" == value || "LR" == value || "MM" == value){
               degreeValue = "°F"
           }
        return degreeValue
    }

    private fun unixTime(timex:Long):String{
        val date = Date(timex * 1000L )
        val sdf = SimpleDateFormat("HH:mm", Locale.UK)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }

    private fun showProgressDialog(){
        customProgressDialog= Dialog(this@MainActivity)
        customProgressDialog.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog.show()
    }


    private fun cancelProgressDialog(){
        customProgressDialog.dismiss()
    }

    private fun locationPermission(){

        val permissionsToRequest = arrayListOf<String>()

        if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            showRationaleDialogForPermissions()
            return
        }

        if(this.let { ContextCompat.checkSelfPermission(it, Manifest.permission.ACCESS_COARSE_LOCATION) } != PackageManager.PERMISSION_GRANTED){
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if(this.let { ContextCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION) } != PackageManager.PERMISSION_GRANTED){
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }


        if(permissionsToRequest.isNotEmpty()){
            ActivityCompat.requestPermissions(
                this as Activity,
                permissionsToRequest.toTypedArray(),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }


    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

           if(requestCode== LOCATION_PERMISSION_REQUEST_CODE){

               if ((grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED })) {
                    locationPermission()
               }

           }


    }

    private fun showRationaleDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It looks like you have turned off permission request for this feature It can be enabled under the Applications settings ")
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", this.packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Log.d("",e.message.toString())
                }
            }
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }
            .show()

    }

    private fun isLocationEnabled():Boolean{

        val locationManager:LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }


}