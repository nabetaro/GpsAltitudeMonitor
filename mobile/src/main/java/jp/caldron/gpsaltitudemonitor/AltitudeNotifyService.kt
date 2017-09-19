package jp.caldron.gpsaltitudemonitor

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.IBinder
import android.support.v4.app.NotificationCompat.CarExtender
import android.support.v4.app.NotificationCompat.CarExtender.UnreadConversation
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import android.widget.Toast
import jp.caldron.gpsaltitudemonitor.event.NotifyAltitudeEvent
import jp.caldron.gpsaltitudemonitor.exception.GpsDisabledException
import org.greenrobot.eventbus.Subscribe

class AltitudeNotifyService : Service() {
    private val TAG = "Service"

    private var mNotificationManager: NotificationManagerCompat? = null
    private lateinit var altitudeReader: AltitudeReader


    private val NOTICE_CONV_ID = 1
    private val REQUEST_CODE_MAIN_ACTIVITY = 1001

    override fun onCreate() {
        mNotificationManager = NotificationManagerCompat.from(applicationContext)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "locationStart()")

        altitudeReader = AltitudeReader(this)
        try {
            altitudeReader.start()
            sendAltitudeNotification(null)
        } catch (e: GpsDisabledException) {
            Log.d(TAG, "not gpsEnable, startActivity")
            stopSelf()
            return Service.START_NOT_STICKY
        }
        Toast.makeText(this, "Service start...", Toast.LENGTH_LONG).show()
        return Service.START_STICKY
    }

    override fun onDestroy() {
        altitudeReader.stop()
        Toast.makeText(this, "Service stop", Toast.LENGTH_LONG).show()
        Log.d(TAG, "GPS Location Stop")
    }

    @Subscribe
    fun onMessageEvent(event: NotifyAltitudeEvent) {
        sendAltitudeNotification(event.altitude)
    }

    private fun createIntent(conversationId: Int, action: String): Intent {
        return Intent()
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .setAction(action)
                .putExtra(CONVERSATION_ID, conversationId)
    }

    /**
     * 通知
     */
    private fun sendAltitudeNotification(altitude: Double?) {
        val timestamp = System.currentTimeMillis()
        // A pending Intent for reads
        val readPendingIntent = PendingIntent.getBroadcast(applicationContext,
                NOTICE_CONV_ID,
                createIntent(NOTICE_CONV_ID, READ_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT)

        // Create the UnreadConversation and populate it with the participant name,
        // read and reply intents.
        val unreadConvBuilder = UnreadConversation.Builder("unread_string")
                .setLatestTimestamp(timestamp)
                .setReadPendingIntent(readPendingIntent)


        // Intent の作成
        val intent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
                this, REQUEST_CODE_MAIN_ACTIVITY, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        // LargeIcon の Bitmap を生成
        val largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

        // NotificationBuilderを作成
        val builder = android.support.v7.app.NotificationCompat.Builder(applicationContext)
        builder.setContentIntent(contentIntent)
        // アイコン
        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setContentTitle(resources.getString(R.string.altitude))
        if (altitude != null) {
            builder.setContentText(resources.getString(R.string.alt_value, altitude))
        } else {
            builder.setContentText("")
        }
        builder.setLargeIcon(largeIcon)
        builder.setWhen(timestamp)
        // タップするとキャンセル(消える)
        builder.setAutoCancel(true)

        builder.priority = Notification.PRIORITY_MIN


        builder.setContentIntent(readPendingIntent)
        builder.extend(CarExtender()
                .setUnreadConversation(unreadConvBuilder.build()))

        // NotificationManagerを取得
        val manager = NotificationManagerCompat.from(applicationContext)
        // Notificationを作成して通知
        manager.notify(NOTICE_CONV_ID, builder.build())
    }

    companion object {
        val READ_ACTION = "jp.caldron.gpsaltitudemonitor.ACTION_MESSAGE_READ"
        val CONVERSATION_ID = "conversation_id"
    }
}
