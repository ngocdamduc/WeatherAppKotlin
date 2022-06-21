package com.example.weatherappkotlin.dataClass

import com.google.gson.annotations.SerializedName

data class Sys(
    @SerializedName("country")val country: String,
    @SerializedName("sunrise")val sunrise: Int,
    @SerializedName("sunset")val sunset: Int,
    @SerializedName("id")val id: Int,
    @SerializedName("type")val type: Int,
    @SerializedName("message")val message: Double
    )