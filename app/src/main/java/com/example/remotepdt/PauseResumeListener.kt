import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.example.remotepdt.BluetoothComm
import org.json.JSONObject

class PauseResumeListener private constructor(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private val bluetoothComm = BluetoothComm.getInstance(context)
    private var ongoingTreatment = true
    private var onPauseResumeListener: ((Boolean) -> Unit)? = null

    private val runnable = object : Runnable {
        override fun run() {
            try {
                AndroidNetworking.get("http://hardware-comm.onrender.com/hardware/get_treatment_pause?id=1")
                    .build()
                    .getAsJSONObject(object : JSONObjectRequestListener {
                        override fun onResponse(response: JSONObject) {
                            val message = response.optString("message", "")
                            if(ongoingTreatment && message == "Pause") {
                                handler.post {
                                    Toast.makeText(context, "Got request to pause", Toast.LENGTH_LONG).show()
                                }
                                // send BT command to pause
                                val command = "2".toByteArray()
                                val endMessage = bluetoothComm.sendAndReceiveMessage(command)
                                if (endMessage != "") {
                                    // Successfully paused treatment
                                    ongoingTreatment = false
                                    handler.post {
                                        Toast.makeText(context, "Paused treatment", Toast.LENGTH_LONG).show()
                                    }
                                    onPauseResumeListener?.invoke(ongoingTreatment)
                                } else {
                                    // Display error message
                                    handler.post {
                                        Toast.makeText(context, "An error occurred while trying to pause", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else if (!ongoingTreatment && message == "Resume") {
                                handler.post {
                                    Toast.makeText(context, "Got request to resume", Toast.LENGTH_LONG).show()
                                }
                                // send BT command to resume
                                val command = "1".toByteArray()
                                val endMessage = bluetoothComm.sendAndReceiveMessage(command)
                                if (endMessage != "") {
                                    // Successfully resumed treatment
                                    ongoingTreatment = true
                                    handler.post {
                                        Toast.makeText(context, "Resumed treatment", Toast.LENGTH_LONG).show()
                                    }
                                    onPauseResumeListener?.invoke(ongoingTreatment)
                                } else {
                                    // Display error message
                                    handler.post {
                                        Toast.makeText(context, "An error occurred while trying to resume", Toast.LENGTH_LONG).show()
                                    }
                                }

                            }
                        }

                        override fun onError(anError: com.androidnetworking.error.ANError?) {
                            Log.e("PauseListener", "Error: ${anError?.message}")
                        }
                    })

                handler.postDelayed(this, 5000)
            } catch (e: Exception) {
                Log.e("PauseResumeListener", "Error in request: ${e.message}")
            }
        }
    }

    fun start() {
        handler.post(runnable)
    }

    fun stop() {
        handler.removeCallbacks(runnable)
    }

    fun setOnPauseResumeListener(listener: ((Boolean) -> Unit)?) {
        this.onPauseResumeListener = listener
    }

    companion object {
        @Volatile
        private var instance: PauseResumeListener? = null

        fun getInstance(context: Context): PauseResumeListener {
            return instance ?: synchronized(this) {
                instance ?: PauseResumeListener(context.applicationContext).also { instance = it }
            }
        }
    }
}
