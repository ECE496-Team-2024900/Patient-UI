import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

// Data model for the response
data class TreatmentStatusResponse(
    val packet: String? = null
)

interface TreatmentStatusService {
    @GET("approval")
    fun getTreatmentStatus(
        @Query("treatment_id") treatmentId: String,
    ): Call<TreatmentStatusResponse>
}
