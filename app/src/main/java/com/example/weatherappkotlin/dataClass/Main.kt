package com.example.weatherappkotlin.dataClass

import com.google.gson.annotations.SerializedName

data class Main(
    @SerializedName("temp") val temp: Double,
    @SerializedName("temp_min") val tempMin: Double,
    @SerializedName("humidity")val humidity: Int,
    @SerializedName("pressure")val pressure: Int,
    @SerializedName("feels_like")val feelsLike: Double,
    @SerializedName("temp_max") val tempMax: Double
    )