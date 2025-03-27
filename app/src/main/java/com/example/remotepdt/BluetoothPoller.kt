import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.example.remotepdt.BluetoothComm
import org.json.JSONObject

class BluetoothPoller private constructor(private val context: Context, private var treatmentId: Int) {

    private val bluetoothComm = BluetoothComm.getInstance(context)
    private val handlerThread = HandlerThread("BluetoothPoller").apply { start() }
    private val handler = Handler(handlerThread.looper)
    private val BeUrl = "http://hardware-comm.onrender.com"
    private val interval = 1000L // polling every 5 seconds

    private val sendRunnable = object : Runnable {
        override fun run() {
            pollStatus()
            pollSensorData()
            handler.postDelayed(this, interval)
        }
    }

    private fun getPhaseAndPercentage(input: String): String? {
        val regex = """(.*?)(\d+%)""".toRegex()
        val matchResult = regex.find(input)

        return matchResult?.let {
            val (treatmentPhase, percentage) = it.destructured
            "${treatmentPhase.trim()}-${percentage.trim()}"
        }
    }

    /**
     * Sends the status request command and processes the response.
     */
    private fun pollStatus() {
        val response = bluetoothComm.sendAndReceiveMessage("9\r\n".toByteArray())
        Log.d("BT LOGGING:", "Command 9 response - $response")
        val formattedResponse = getPhaseAndPercentage(response).toString()
        sendProgressToBackend(formattedResponse)
    }

    private fun pollSensorData() {
        val response = bluetoothComm.sendAndReceiveMessage("8\r\n".toByteArray())
        Log.d("BT LOGGING: : ", response)
        val jsonBody = JSONObject()
        jsonBody.put("data", response)

        Log.d("BT LOGGING:", "Command 8 about to send $jsonBody to BE for $treatmentId")

        // Was commented out during testing!
        AndroidNetworking.put("${BeUrl}/hardware/set_sensor_data_updates?id=${treatmentId}")
            .addJSONObjectBody(jsonBody)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    Log.d("BT LOGGING:", "Command 8 response - $response")
                    // clinician got response
//                    handler.post {
//                        Toast.makeText(
//                            context,
//                            "Stored reply",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
                }
                override fun onError(anError: ANError) {
                    // handle error
                    handler.post {
                        Toast.makeText(
                            context,
                            "Error: ${anError.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    Log.e("BT LOGGING:", "Error: ${anError.message}")
                }
            })
    }

    /**
     * Sends the received progress to the backend microservice.
     */
    private fun sendProgressToBackend(progress: String) {
        val jsonObject = JSONObject().apply {
            put("data", progress)
        }

        val url = "$BeUrl/hardware/set_treatment_progress?id=$treatmentId"

        Log.d("BT LOGGING:", "Command 9 about to send $jsonObject to BE $treatmentId")

        // Was commented out during testing!
        AndroidNetworking.put(url)
            .addJSONObjectBody(jsonObject)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject?) {
//                    handler.post {
//                        Toast.makeText(
//                            context,
//                            "Recieved: $response",
//                            Toast.LENGTH_LONG
//                        ).show()
//                    }
                    Log.d("BT LOGGING:", "Command 9 sent $response to BE")
                }

                override fun onError(anError: ANError?) {
                    handler.post {
                        Toast.makeText(
                            context,
                            "Failed to update progress",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })
    }

    fun start() {
        handler.post(sendRunnable)
    }

    fun stop() {
        handler.removeCallbacks(sendRunnable)
        handlerThread.quitSafely()
    }

    companion object {
        @Volatile
        private var instance: BluetoothPoller? = null

        fun getInstance(context: Context, treatmentId: Int): BluetoothPoller {
            return instance ?: synchronized(this) {
                instance ?: BluetoothPoller(context.applicationContext, treatmentId).also { instance = it }
            }
        }
    }
}
