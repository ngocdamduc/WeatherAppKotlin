package com.example.weatherappkotlin

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.weatherappkotlin.dataClass.ModelClass
import com.example.weatherappkotlin.databinding.ActivityMainBinding
import com.example.weatherappkotlin.utilities.ApiUtilities
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    companion object{
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
        const val API_KEY = "91ce709e59d05cd31a19f48575fc1a77"
    }
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var activityMainBinding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        supportActionBar?.hide()
        activityMainBinding.rlMainLayout.visibility = View.GONE
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        activityMainBinding.etGetCityName.setOnEditorActionListener { v, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                getCityWeather(activityMainBinding.etGetCityName.text.toString())
                val view = this.currentFocus
                if (view != null) {
                    val imm: InputMethodManager =
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    activityMainBinding.etGetCityName.clearFocus()
                }
                true
            } else false
        }
        getCurrentLocation()
    }
    private fun getCityWeather(cityName: String) {
        activityMainBinding.pbLoading.visibility = View.VISIBLE
        ApiUtilities.getApiInterface()?.getCityWeatherData(cityName, API_KEY)?.enqueue(object: Callback<ModelClass>{
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
                setDataOnViews(response.body())
            }
            override fun onFailure(call: Call<ModelClass>, t: Throwable) {
               Toast.makeText(applicationContext, "Not a Valid City Name", Toast.LENGTH_SHORT).show()
            }

        })
    }
    private fun getCurrentLocation() {
        if(checkPermissions())
        {
            if(isLocationEnabled()){
                if (ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions()
                    return
                }
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this){ task ->
                    val location: Location?= task.result
                    if(location == null)
                    {
                        Toast.makeText(this, "Null Received", Toast.LENGTH_SHORT).show()
                    }
                    else
                    {
                        //fetch weather here
                        fetchCurrentLocationWeather(location.latitude.toString(), location.longitude.toString())
                    }
                }
            }
            else{
                Toast.makeText(this, "Turn on Location", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }else{
            requestPermissions()
        }
    }

    private fun fetchCurrentLocationWeather(latitude: String, longitude: String) {
        activityMainBinding.pbLoading.visibility = View.VISIBLE
        ApiUtilities.getApiInterface()?.getCurrentWeatherData(latitude, longitude, API_KEY)?.enqueue(
            object : Callback<ModelClass> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
                    if(response.isSuccessful)
                    {
                        setDataOnViews(response.body())
                    }
                }

                override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                    Toast.makeText(applicationContext, "ERROR", Toast.LENGTH_SHORT).show()
                }
            })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setDataOnViews(body: ModelClass?) {
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm")
        val currentDate = sdf.format(Date())
        if(body == null){
            Toast.makeText(applicationContext, "Not a Valid City Name", Toast.LENGTH_SHORT).show()
            return
        }
        activityMainBinding.tvDateAndTime.text = currentDate
        activityMainBinding.tvDayMaxTemp.text = "Day:" + kelvinToCelsius(body.main.tempMax) + "°"
        activityMainBinding.tvDayMinTemp.text = "Night:" + kelvinToCelsius(body.main.tempMin) + "°"
        activityMainBinding.tvTemp.text = "" + kelvinToCelsius(body.main.temp) + "°"
        activityMainBinding.tvFeelsLike.text = "Feels Like:" + kelvinToCelsius(body.main.feelsLike) + "°"
        activityMainBinding.tvWeatherType.text = body.weather[0].main
        activityMainBinding.tvSunrise.text = timeStampToLocalDate(body.sys.sunrise.toLong())
        activityMainBinding.tvSunset.text = timeStampToLocalDate(body.sys.sunset.toLong())
        activityMainBinding.tvPressure.text = body.main.pressure.toString()
        activityMainBinding.tvHumidity.text = body.main.humidity.toString() + "%"
        activityMainBinding.tvWindSpeed.text = body.wind.speed.toString() + "m/s"

        activityMainBinding.tvTempFahrenheit.text ="" +((kelvinToCelsius(body.main.temp)).times(1.8).plus(32).roundToInt()) +"°"
        activityMainBinding.etGetCityName.setText(body.name)

        updateUI(body.weather[0].id)
    }

    private fun updateUI(id: Int) {
        //thunderstorm
        when (id) {
            in 200..232 -> {
                //thunderstorm
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.thunderstorm)
                activityMainBinding.rlToolbar.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.thunderstorm))
                activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.thunderstorm_bg
                )
                activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.thunderstorm_bg
                )
                activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.thunderstorm_bg
                )
                activityMainBinding.ivWeatherBg.setImageResource(R.drawable.thunderstorm_bg)
                activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.thunderstrom)
            }
            in 300..321 -> {
                //drizzle
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.drizzle)
                activityMainBinding.rlToolbar.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.drizzle))
                activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.drizzle_bg
                )
                activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.drizzle_bg
                )
                activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.drizzle_bg
                )
                activityMainBinding.ivWeatherBg.setImageResource(R.drawable.drizzle_bg)
                activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.drizzle)
            }
            in 500..531 -> {
                //rain
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.rain)
                activityMainBinding.rlToolbar.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.rain))
                activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.rainy_bg
                )
                activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.rainy_bg
                )
                activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.rainy_bg
                )
                activityMainBinding.ivWeatherBg.setImageResource(R.drawable.rainy_bg)
                activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.rain)
            }
            in 600..620 -> {
                //snow
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.snow)
                activityMainBinding.rlToolbar.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.snow))
                activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.snow_bg
                )
                activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.snow_bg
                )
                activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.snow_bg
                )
                activityMainBinding.ivWeatherBg.setImageResource(R.drawable.snow_bg)
                activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.snow)
            }
            in 701..781 -> {
                //mist
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.mist)
                activityMainBinding.rlToolbar.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.mist))
                activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.mist_bg
                )
                activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.mist_bg
                )
                activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.mist_bg
                )
                activityMainBinding.ivWeatherBg.setImageResource(R.drawable.mist_bg)
                activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.mist)
            }
            800 -> {
                //clear
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.clear)
                activityMainBinding.rlToolbar.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.clear))
                activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.clear_bg
                )
                activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.clear_bg
                )
                activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.clear_bg
                )
                activityMainBinding.ivWeatherBg.setImageResource(R.drawable.clear_bg)
                activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.clear)
            }
            else -> {
                //cloud
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.cloud)
                activityMainBinding.rlToolbar.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.cloud))
                activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.cloud_bg
                )
                activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.cloud_bg
                )
                activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.cloud_bg
                )
                activityMainBinding.ivWeatherBg.setImageResource(R.drawable.cloud_bg)
                activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.clouds)
            }
        }
        activityMainBinding.pbLoading.visibility = View.GONE
        activityMainBinding.rlMainLayout.visibility = View.VISIBLE
    }
    private fun isLocationEnabled() : Boolean{
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }
    private fun requestPermissions(){
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_ACCESS_LOCATION)
    }
    private fun checkPermissions() : Boolean{
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED)
        {
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSION_REQUEST_ACCESS_LOCATION){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(applicationContext, "Granted", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            }
            else{
                Toast.makeText(applicationContext, "Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun timeStampToLocalDate(timestamp: Long): String {
        val localTime = timestamp.let {
            Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).toLocalTime()
        }
        return localTime.toString()
    }

    private fun kelvinToCelsius(temp: Double): Double {
        var intTemp = temp
        intTemp = intTemp.minus(273)
        return intTemp.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
    }
}