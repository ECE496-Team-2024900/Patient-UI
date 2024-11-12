import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Body

// Data model for the response
data class TreatmentStatusResponse(
    val status: String,
    val packet_data: String? = null
)

interface TreatmentStatusService {
    @GET("api/treatment-status/{patient_id}/")
    fun getTreatmentStatus(@Path("patient_id") patientId: String): Call<TreatmentStatusResponse>
}
