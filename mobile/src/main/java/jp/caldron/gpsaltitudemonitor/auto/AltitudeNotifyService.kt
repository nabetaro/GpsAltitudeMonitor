/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.caldron.gpsaltitudemonitor.auto

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationCompat.CarExtender
import android.support.v4.app.NotificationCompat.CarExtender.UnreadConversation
import android.support.v4.app.NotificationManagerCompat

class AltitudeNotifyService : Service() {
    private val mMessenger = Messenger(IncomingHandler())
    private var mNotificationManager: NotificationManagerCompat? = null

    override fun onCreate() {
        mNotificationManager = NotificationManagerCompat.from(applicationContext)
    }

    override fun onBind(intent: Intent): IBinder? {
        return mMessenger.binder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return Service.START_STICKY
    }

    private fun createIntent(conversationId: Int, action: String): Intent {
        return Intent()
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .setAction(action)
                .putExtra(CONVERSATION_ID, conversationId)
    }

    private fun sendNotification(conversationId: Int, message: String,
                                 participant: String, timestamp: Long) {
        // A pending Intent for reads
        val readPendingIntent = PendingIntent.getBroadcast(applicationContext,
                conversationId,
                createIntent(conversationId, READ_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT)

        // Create the UnreadConversation and populate it with the participant name,
        // read and reply intents.
        val unreadConvBuilder = UnreadConversation.Builder(participant)
                .setLatestTimestamp(timestamp)
                .setReadPendingIntent(readPendingIntent)

        val builder = NotificationCompat.Builder(applicationContext)
                // Set the application notification icon:
                //.setSmallIcon(R.drawable.notification_icon)

                // Set the large icon, for example a picture of the other recipient of the message
                //.setLargeIcon(personBitmap)

                .setContentText(message)
                .setWhen(timestamp)
                .setContentTitle(participant)
                .setContentIntent(readPendingIntent)
                .extend(CarExtender()
                        .setUnreadConversation(unreadConvBuilder.build()))

        mNotificationManager!!.notify(conversationId, builder.build())
    }

    /**
     * Handler of incoming messages from clients.
     */
    internal inner class IncomingHandler : Handler() {
        override fun handleMessage(msg: Message) {
            sendNotification(1, "This is a sample message", "John Doe",
                    System.currentTimeMillis())
        }
    }

    companion object {
        val READ_ACTION = "jp.caldron.gpsaltitudemonitor.ACTION_MESSAGE_READ"
        val REPLY_ACTION = "jp.caldron.gpsaltitudemonitor.ACTION_MESSAGE_REPLY"
        val CONVERSATION_ID = "conversation_id"
        val EXTRA_VOICE_REPLY = "extra_voice_reply"
        private val TAG = AltitudeNotifyService::class.java.simpleName
    }
}
