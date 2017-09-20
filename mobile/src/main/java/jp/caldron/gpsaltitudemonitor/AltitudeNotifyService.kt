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
import android.support.v7.app.NotificationCompat
import android.util.Log
import android.widget.Toast
import jp.caldron.gpsaltitudemonitor.event.NotifyAltitudeEvent
import jp.caldron.gpsaltitudemonitor.exception.GpsDisabledException
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class AltitudeNotifyService : Service() {
    private val TAG = "Service"

    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var altitudeReader: AltitudeReader


    private val NOTICE_CONV_ID = 0
    private val REQUEST_CODE_MAIN_ACTIVITY = 1001

    override fun onCreate() {
        notificationManager = NotificationManagerCompat.from(applicationContext)
        notificationBuilder = android.support.v7.app.NotificationCompat.Builder(applicationContext)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "locationStart()")
        EventBus.getDefault().register(this)

        altitudeReader = AltitudeReader(this)
        try {
            altitudeReader.start()
            createAltitudeNotification()
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
        EventBus.getDefault().unregister(this)
        notificationManager.cancel(NOTICE_CONV_ID)
        Toast.makeText(this, "Service stop", Toast.LENGTH_LONG).show()
        Log.d(TAG, "GPS Location Stop")
    }

    @Subscribe
    fun onMessageEvent(event: NotifyAltitudeEvent) {
        updateAltitudeNotification(event.altitude)
    }

    private fun createIntent(conversationId: Int, action: String): Intent {
        return Intent()
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .setAction(action)
                .putExtra(CONVERSATION_ID, conversationId)
    }

    /**
     * 通知作成
     */
    private fun createAltitudeNotification() {
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
                .addMessage("")


        // Intent の作成
        val intent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
                this, REQUEST_CODE_MAIN_ACTIVITY, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        // LargeIcon の Bitmap を生成
        val largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

        notificationBuilder.setContentIntent(contentIntent)
        // アイコン
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher)
        notificationBuilder.setContentTitle(resources.getString(R.string.altitude))
        notificationBuilder.setContentText("")
        notificationBuilder.setLargeIcon(largeIcon)
        notificationBuilder.setWhen(timestamp)

        notificationBuilder.priority = Notification.PRIORITY_MIN


        notificationBuilder.setContentIntent(readPendingIntent)
        notificationBuilder.extend(CarExtender()
                .setUnreadConversation(unreadConvBuilder.build()))

        // Notificationを作成して通知
        notificationManager.notify(NOTICE_CONV_ID, notificationBuilder.build())
    }

    /**
     * 通知更新
     */
    private fun updateAltitudeNotification(altitude: Double?) {
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
                .addMessage("")


        notificationBuilder.setContentText(resources.getString(R.string.alt_value, altitude))
        notificationBuilder.setWhen(timestamp)
        // Notificationを作成して通知
        notificationManager.notify(NOTICE_CONV_ID, notificationBuilder.build())
    }

    private fun addAutoNotification(nManager: NotificationManagerCompat, content: String) {

    }

    companion object {
        val READ_ACTION = "jp.caldron.gpsaltitudemonitor.ACTION_MESSAGE_READ"
        val CONVERSATION_ID = "conversation_id"
    }
}
