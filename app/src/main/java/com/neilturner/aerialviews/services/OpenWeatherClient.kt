package com.neilturner.aerialviews.services

import android.content.Context
import com.neilturner.aerialviews.models.weather.OpenWeatherAPI
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create

class OpenWeatherClient(context: Context) : WeatherClient(context) {

    val client by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(okHttpClient())
            .build()
            .create<OpenWeatherAPI>()
    }

    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/"
        private const val TAG = "OpenWeatherClient"
    }
}
