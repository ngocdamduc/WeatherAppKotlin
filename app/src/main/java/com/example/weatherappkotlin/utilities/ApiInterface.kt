package com.example.weatherappkotlin.utilities

import com.example.weatherappkotlin.dataClass.ModelClass
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {

    @GET("weather")
    fun getCurrentWeatherData(
        @Query("lat") latitude: String,
        @Query("lon") longitude: String,
        @Query("appid") api_key:String
    ):retrofit2.Call<ModelClass>

    @GET("weather")
    fun getCityWeatherData(
        @Query("q") cityName: String,
        @Query("appid") api_key:String
    ):retrofit2.Call<ModelClass>
}