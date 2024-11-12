// TreatmentManager.kt
import android.os.Handler
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TreatmentManager(
    private val treatmentId: String,        // Patient ID to track treatment approval
    private val treatmentApiService: TreatmentStatusService,  // Retrofit API service
    private val context: android.content.Context,           // Context for Toast messages
    private val handler: Handler          // Handler for polling
) {

    private val pollingInterval: Long = 5000  // Poll every 5 seconds

    // Start polling for treatment status
    fun startPolling() {
        val runnable = object : Runnable {
            override fun run() {
                checkTreatmentStatus()
                handler.postDelayed(this, pollingInterval)
            }
        }
        handler.post(runnable)
    }

    // Stop polling (optional, to be called from the calling class when needed)
    fun stopPolling() {
        handler.removeCallbacksAndMessages(null)
    }

    // Polling method to check treatment status
    private fun checkTreatmentStatus() {
        treatmentApiService.getTreatmentStatus(treatmentId).enqueue(object : Callback<TreatmentStatusResponse> {
            override fun onResponse(
                call: Call<TreatmentStatusResponse>,
                response: Response<TreatmentStatusResponse>
            ) {
                when (response.code()) {
                    200 -> {
                        // Approved
                        val treatmentStatus = response.body()
                        treatmentStatus?.let {
                            it.packet_data?.let { packetData ->
                                sendBTDevicePacket(packetData)  // Placeholder for Bluetooth function
                                Toast.makeText(context, "Treatment Approved! Sending packet...", Toast.LENGTH_SHORT).show()
                                stopPolling()
                            }
                        }
                    }
                }
            }

            override fun onFailure(call: Call<TreatmentStatusResponse>, t: Throwable) {
                // Handle failure (e.g., network issues)
                Toast.makeText(context, "Error checking treatment status", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Placeholder function for Bluetooth communication (replace with actual Bluetooth code)
    private fun sendBTDevicePacket(packetData: String) {
        // Your Bluetooth logic to send the packet data to the Bluetooth device
    }
}
