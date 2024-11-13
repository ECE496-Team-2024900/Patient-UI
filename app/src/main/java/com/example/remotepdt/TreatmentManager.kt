// TreatmentManager.kt
import android.os.Handler
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TreatmentManager(
    private val treatmentId: String,
    private val treatmentStatusService: TreatmentStatusService,
    private val context: android.content.Context,
    private val handler: Handler
) {

    private val pollingInterval: Long = 5000  // Poll every 5 seconds

    fun startPolling() {
        val runnable = object : Runnable {
            override fun run() {
                checkTreatmentStatus()
                handler.postDelayed(this, pollingInterval)
            }
        }
        handler.post(runnable)
    }

    fun stopPolling() {
        handler.removeCallbacksAndMessages(null)
    }

    private fun checkTreatmentStatus() {
        treatmentStatusService.getTreatmentStatus(treatmentId).enqueue(object : Callback<TreatmentStatusResponse> {
            override fun onResponse(
                call: Call<TreatmentStatusResponse>,
                response: Response<TreatmentStatusResponse>
            ) {
                when (response.code()) {
                    200 -> {
                        // Approved
                        val treatmentStatus = response.body()
                        treatmentStatus?.let {
                            it.packet?.let { p ->
                                sendBTDevicePacket(p)
                                stopPolling()
                            }
                        }
                    }
                }
            }

            override fun onFailure(call: Call<TreatmentStatusResponse>, t: Throwable) {
                // Telling user that there was an error
                Toast.makeText(context, "Error with retrieving clinician approval", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Placeholder function for Bluetooth communication (replace with actual Bluetooth code)
    private fun sendBTDevicePacket(packetData: String) {
        // Your Bluetooth logic to send the packet data to the Bluetooth device
    }
}
