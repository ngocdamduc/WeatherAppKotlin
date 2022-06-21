package com.example.weatherappkotlin.dataClass

import com.google.gson.annotations.SerializedName

data class Weather(
    @SerializedName("icon") val icon: String,
    @SerializedName ("description") val description: String,
    @SerializedName("main") val main: String,
    @SerializedName("id") val id: Int
    )