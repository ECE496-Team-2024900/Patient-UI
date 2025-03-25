import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.widget.Toast
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.example.remotepdt.BluetoothComm
import com.example.remotepdt.TreatmentSessionActivity
import org.json.JSONObject

class UpdatePolling private constructor(private val context: Context) {

    private val bluetoothComm = BluetoothComm.getInstance(context)
    private val handlerThread = HandlerThread("UpdatePollingThread").apply { start() }
    private val handler = Handler(handlerThread.looper)
    private val BeUrl = "http://hardware-comm.onrender.com"
    private val treatmentId = 1
    private val interval = 5000 // polling every 5 seconds

    private val sendRunnable = object : Runnable {
        override fun run() {
            val messageSent = bluetoothComm.sendMessageBytes("8".toByteArray()) // Send message
            if(messageSent) {
                val response = bluetoothComm.receiveMessage()
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "Recieved: $response",
                        Toast.LENGTH_LONG
                    ).show()
                }
                val jsonBody = JSONObject()
                jsonBody.put("data", response)
                AndroidNetworking.put("$BeUrl/hardware/set_sensor_data_updates")
                    .addQueryParameter("id", treatmentId.toString())
                    .addJSONObjectBody(jsonBody)
                    .build()
                    .getAsJSONObject(object : JSONObjectRequestListener {
                        override fun onResponse(response: JSONObject) {
                            // clinician got response
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    context,
                                    "Stored reply",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        override fun onError(anError: ANError) {
                            // handle error
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    context,
                                    "Error: ${anError.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    })
            }
            handler.postDelayed(this, interval.toLong())
        }
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
        private var instance: UpdatePolling? = null

        fun getInstance(context: Context): UpdatePolling {
            return instance ?: synchronized(this) {
                instance ?: UpdatePolling(context.applicationContext).also { instance = it }
            }
        }
    }
}
