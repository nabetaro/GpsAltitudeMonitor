package jp.caldron.gpsaltitudemonitor

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationProvider
import android.os.Bundle
import android.util.Log
import jp.caldron.gpsaltitudemonitor.event.NotifyAltitudeEvent
import jp.caldron.gpsaltitudemonitor.exception.GpsDisabledException
import org.greenrobot.eventbus.EventBus

/**
 * 高度取得クラス
 */
class AltitudeReader(cont: Context) : LocationListener {
    private val context = cont

    private val TAG = "AltitudeReader"
    private lateinit var locationManager: LocationManager

    fun start() {
        Log.d(TAG, "AltitudeReader start()")

        // LocationManager インスタンス生成
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!gpsEnabled) {
            throw GpsDisabledException("GPS Disabled")
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 50F, this)
        Log.d(TAG, "GPS Location Start")
    }

    fun stop() {
        Log.d(TAG, "AltitudeReader stop()")
        locationManager.removeUpdates(this)
        Log.d(TAG, "GPS Location Stop")
    }

    override fun onStatusChanged(p0: String?, status: Int, p2: Bundle?) {
        when (status) {
            LocationProvider.AVAILABLE ->
                Log.d(TAG, "LocationProvider.AVAILABLE")
            LocationProvider.OUT_OF_SERVICE ->
                Log.d(TAG, "LocationProvider.OUT_OF_SERVICE")
            LocationProvider.TEMPORARILY_UNAVAILABLE ->
                Log.d(TAG, "LocationProvider.TEMPORARILY_UNAVAILABLE")
        }
    }

    override fun onLocationChanged(location: Location?) {
        // 高度を通知
        val event = NotifyAltitudeEvent(location?.altitude)
        EventBus.getDefault().post(event)
    }

    override fun onProviderEnabled(p0: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onProviderDisabled(p0: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}