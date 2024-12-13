package com.baldae.finalproject

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface BackgroundApi {
    @GET("photos/random")
    fun getRandomPhoto(
        @Query("client_id") clientId: String,
        @Query("orientation") orientation: String = "portrait"
    ): Call<UnsplashPhotoResponse>
}

data class UnsplashPhotoResponse(
    val urls: Urls
)

data class Urls(
    val regular: String
)
