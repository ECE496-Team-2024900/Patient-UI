package com.example.remotepdt

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("treatment/timer/{treatment_id}")
    fun getTreatmentTimer(@Path("treatment_id") treatmentId: Int): Call<TreatmentSession>
}
