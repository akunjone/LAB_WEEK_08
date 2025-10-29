package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SecondNotificationService : Service() {
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var serviceHandler: Handler

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate(){
        super.onCreate()
        notificationBuilder = startForegroundService()

        val handlerThread = HandlerThread("ThirdThread")
            .apply{ start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    private fun startForegroundService(): NotificationCompat.Builder{
        val pendingIntent = getPendingIntent()
        val channelId2 = createNotificationChannel()
        val notificationBuilder = getNotificationBuilder(
            pendingIntent, channelId2
        )

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        return notificationBuilder
    }

    private fun getPendingIntent(): PendingIntent{
        val flag = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            FLAG_IMMUTABLE else 0

        return PendingIntent.getActivity(
            this, 0, Intent(
                this,
                MainActivity::class.java
            ), flag
        )
    }

    private fun createNotificationChannel(): String  =
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channelId2 = "002"
            val channelName = "002 Channel Assignment"

            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(
                channelId2,
                channelName,
                channelPriority
            )

            val service = requireNotNull(
                ContextCompat.getSystemService(this,
                    NotificationManager::class.java)
            )
            service.createNotificationChannel(channel)
            channelId2
        } else{""}

    private fun getNotificationBuilder(pendingIntent: PendingIntent, channelId2: String)=
        NotificationCompat.Builder(this, channelId2)
            .setContentTitle("Assignment worker process is done")
            .setContentText("Check it out the assignment")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("AssignmentWorkerProcess is done, check it out!")
            .setOngoing(true)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int{
        val returnValue = super.onStartCommand(intent, flags, startId)
        val Id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")

        serviceHandler.post{
            countDownTimer(notificationBuilder)
            notifyCompletion(Id)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return returnValue
    }

    private fun countDownTimer(notificationBuilder: NotificationCompat.Builder){
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        for(i in 5 downTo 0){
            notificationBuilder.setContentText("$i seconds left")
                .setSilent(true)
            notificationManager.notify(
                NOTIFICATION_ID,
                notificationBuilder.build()
            )
        }
    }

    private fun notifyCompletion(Id: String){
        Handler(Looper.getMainLooper()).post{
            mutableID.value = Id
        }
    }

    companion object{
        const val NOTIFICATION_ID = 0xCA7
        const val EXTRA_ID = "Id"

        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}