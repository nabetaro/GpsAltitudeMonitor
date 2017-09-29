package jp.caldron.gpsaltitudemonitor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import jp.caldron.gpsaltitudemonitor.event.NotifyAltitudeEvent
import jp.caldron.gpsaltitudemonitor.exception.GpsDisabledException
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe


class MainActivity : AppCompatActivity() {

    private val TAG = "Main"
    private val REQUEST_GPS_PERMISSION = 1000
    private lateinit var altitudeReader: AltitudeReader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val lat = findViewById(R.id.altitude) as TextView
        lat.text = ""

        val stopBtn = findViewById(R.id.stop_button)
        stopBtn.setOnClickListener {
            stopService(Intent(this, AltitudeNotifyService::class.java))
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_GPS_PERMISSION)
        } else {
            locationStart()
        }
    }

    override fun onResume() {
        EventBus.getDefault().register(this)
        super.onResume()
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    private fun locationStart() {
        Log.d(TAG, "locationStart()")

        altitudeReader = AltitudeReader(this)

        try {
            startService(Intent(this, AltitudeNotifyService::class.java))
        } catch (e: GpsDisabledException) {
            // GPSを設定するように促す
            val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(settingsIntent)
            Log.d(TAG, "not gpsEnable, startActivity")

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_GPS_PERMISSION)

                Log.d(TAG, "checkSelfPermission false")
                return
            }
        }

    }

    @Subscribe
    fun onMessageEvent(event: NotifyAltitudeEvent) {
        Log.d(TAG, "Received NotifyAltitudeEvent")
        val lat = findViewById(R.id.altitude) as TextView
        lat.text = getString(R.string.alt_value, event.altitude)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        if (requestCode == REQUEST_GPS_PERMISSION) {
            // 使用が許可された
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "checkSelfPermission true")

                locationStart()
                return

            } else {
                // それでも拒否された時の対応
                Toast.makeText(this, "cannot start", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
