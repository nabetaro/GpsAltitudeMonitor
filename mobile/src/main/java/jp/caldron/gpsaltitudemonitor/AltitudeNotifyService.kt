package jp.caldron.gpsaltitudemonitor

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.IBinder
import android.support.v4.app.NotificationCompat.CarExtender
import android.support.v4.app.NotificationCompat.CarExtender.UnreadConversation
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.app.RemoteInput
import android.support.v4.app.NotificationCompat
import android.util.Log
import android.widget.Toast
import jp.caldron.gpsaltitudemonitor.event.NotifyAltitudeEvent
import jp.caldron.gpsaltitudemonitor.exception.GpsDisabledException
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class AltitudeNotifyService : Service() {
    private val TAG = "Service"
    private val TEST = true
    private var testCount = 4000.0

    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var altitudeReader: AltitudeReader

    private val NOTICE_CONV_ID = 0
    private val REQUEST_CODE_MAIN_ACTIVITY = 1001
    private val MY_VOICE_REPLY_KEY = "reply_key"

    override fun onCreate() {
        notificationManager = NotificationManagerCompat.from(applicationContext)
        notificationBuilder = NotificationCompat.Builder(applicationContext)
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

        if (TEST) {
            val handler = Handler()
            val runnable = object : Runnable {
                override fun run() {
                    EventBus.getDefault().post(NotifyAltitudeEvent(testCount))
                    testCount ++
                    handler.postDelayed(this, 10000)
                }
            }
            handler.post(runnable)
        }

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
        Log.d(TAG, "Received NotifyAltitudeEvent")
        updateAltitudeNotification(event.altitude)
        //notifyMessage(this,resources.getString(R.string.altitude), resources.getString(R.string.alt_value, event?.altitude))
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

        //notificationBuilder.priority = Notification.PRIORITY_MIN


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
        Log.d(TAG, "updateAltitudeNotification start")
        val timestamp = System.currentTimeMillis()

        val msgHeardIntent = Intent()
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .setAction(READ_ACTION)
                .putExtra(CONVERSATION_ID, NOTICE_CONV_ID)


        // A pending Intent for reads
        val readPendingIntent = PendingIntent.getBroadcast(applicationContext,
                NOTICE_CONV_ID,
                msgHeardIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        // Create the UnreadConversation and populate it with the participant name,
        // read and reply intents.
        val unreadConvBuilder = UnreadConversation.Builder("高度")
                .setReadPendingIntent(readPendingIntent)
                .addMessage(resources.getString(R.string.alt_value, altitude))
                .setLatestTimestamp(timestamp)

        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher)
        notificationBuilder.extend(CarExtender()
                .setUnreadConversation(unreadConvBuilder.build()))
        notificationBuilder.setContentText(resources.getString(R.string.alt_value, altitude))
        notificationBuilder.setWhen(timestamp)
        notificationManager.notify(NOTICE_CONV_ID, notificationBuilder.build())
    }

    fun notifyMessage(context: Context, name: String, message: String) {
        // 既読用インテント
        val msgHeardIntent = Intent()
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .setAction(READ_ACTION)
                .putExtra(CONVERSATION_ID, NOTICE_CONV_ID)

        val msgHeardPendingIntent = PendingIntent.getBroadcast(context.applicationContext,
                // ここでは固定で入れていますが、どの会話か同定できるようにしましょう。
                NOTICE_CONV_ID,
                msgHeardIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        // 返信用インテント
        val replyIntent = Intent()
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .putExtra(CONVERSATION_ID, NOTICE_CONV_ID)

        val replyPendingIntent = PendingIntent.getBroadcast(context.applicationContext,
                // これも固定で入れていますが、どの会話か同定できるようにしましょう。
                NOTICE_CONV_ID,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        // 入力を受け付けるためのクラス
        val remoteInput = RemoteInput.Builder(MY_VOICE_REPLY_KEY)
                .setLabel(context.getString(R.string.app_name))
                .build()

        // 一連の会話をグループ化するためのクラス
        val unreadConvBuilder = UnreadConversation.Builder(name)
                // 既読用のPendingIntent
                .setReadPendingIntent(msgHeardPendingIntent)
                // 返信用のPendingIntent
                .setReplyAction(replyPendingIntent, remoteInput)
                // 通知内容
                .addMessage(message)
                // タイムスタンプをつけましょう
                .setLatestTimestamp(System.currentTimeMillis())

        val notificationBuilder = android.support.v4.app.NotificationCompat.Builder(context.applicationContext)
                .setSmallIcon(R.mipmap.ic_launcher)
                .extend(android.support.v4.app.NotificationCompat.CarExtender()
                        .setUnreadConversation(unreadConvBuilder.build()))
                .setPriority(NotificationCompat.PRIORITY_MIN)

        val manager = NotificationManagerCompat.from(context.applicationContext)
        manager.notify(NOTICE_CONV_ID, notificationBuilder.build())
    }

    companion object {
        val READ_ACTION = "jp.caldron.gpsaltitudemonitor.ACTION_MESSAGE_READ"
        val CONVERSATION_ID = "conversation_id"
       }
}
