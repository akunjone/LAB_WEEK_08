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

class NotificationService : Service() {
    //in  order to make the required notification, service is required
    //to do the job for us in the foreground process
    //create the notification builder that will be called later on
    private lateinit var notificationBuilder: NotificationCompat.Builder
    //create system handler which controls what thread the process is being exectution on
    private lateinit var serviceHandler: Handler

    //this is used to bind a two way communication
    //kita pakai one-way, lalu return set ke null
    override fun onBind(intent: Intent): IBinder?=null

    //this is a callback and part of the life cycle
    //the onCreate callback will be called when this service is created for the #1 time
    override fun onCreate() {
        super.onCreate()
        //create notif with all of its contents and config
        //in the startForegrooundService() custom func
        notificationBuilder = startForegroundService()

        //create handler to control which thread the notif will be exec on
        //'HandlerThread' menyediakan different thread untuk prosesnya diexec,
        //'Handler' enqueues the process to HandlerThread to be exec
        //kita instansiasi HandlerThread "SecondThread"
        //lalu pass handlerThread ke main Handler, serviceHandler
        val handlerThread = HandlerThread("SecondThread")
            .apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    //create the notiffication with all of its contents and config all set up
    private fun startForegroundService(): NotificationCompat.Builder{
        val pendingIntent = getPendingIntent()
        val channelId = createNotificationChannel()
        val notificationBuilder = getNotificationBuilder(
            pendingIntent, channelId
        )

        //setelah semua sudah diset dan notif builder is ready, start the foreground service and notif will appear
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        return notificationBuilder
    }

    //pending inttent = intent used to be exec saat user klik notif
    private fun getPendingIntent(): PendingIntent{
        val flag = if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S)
            FLAG_IMMUTABLE else 0

        //setting mainactivity to pending intent
        //saat user click notif, mereka keredirect ke main activity app
        return PendingIntent.getActivity(
            this, 0, Intent(
                this,
                MainActivity::class.java
            ), flag
        )
    }

    //untuk bikin notif, notification channel diperlukan untuk set up required config
    //channel include:
    //channel id, name, priority
    private fun createNotificationChannel(): String =
        //notif channel ada di API 26 keatas
        //kita hrs cek SDK
        //"Build.VERSION_CODES.O" Oreo artinya, untuk API 26 keatas
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            //create channel id
            val channelId = "001"
            //channel name
            val channelName = "001 Channel"
            //priority:
            //IMPORTANCE_HIGH=sound, vibrate, heads up notif
            //IMPORTANCE_DEFAULT=sound, tapi ga heads up
            //IMPORTANCE_LOW=silent dan ga heads up
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT

            //build channel
            val channel = NotificationChannel(
                channelId,
                channelName,
                channelPriority
            )

            //get notificationmanager class
            val service = requireNotNull(
                ContextCompat.getSystemService(this,
                    NotificationManager::class.java)
            )
            //bind channel ke NotificationManager
            //NotificationManager will trigger the notification later on
            service.createNotificationChannel(channel)

            //return channel id
            channelId
        } else{""}

    //build notif with all of its contents and config
    private fun getNotificationBuilder(pendingIntent: PendingIntent, channelId: String)=
        NotificationCompat.Builder(this, channelId)
    //set title
            .setContentTitle("Second worker process is done")
    //sets the content
            .setContentText("Check it out!")
    //sets the notif icon
            .setSmallIcon(R.drawable.ic_launcher_foreground)
    //set action/intent to be exec when user clicks the notif
            .setContentIntent(pendingIntent)
    //sets the ticker message brief message on top of your device
            .setTicker("Second worker process is done, check it out!")
    //setONGoing() controls whether the notif id dismissible or not by user
    //klo true, notif dismissible and can only be closed by app
            .setOngoing(true)

    //This is a callback and part of a life cycle
//This callback will be called when the service is started
//in this case, after the startForeground() method is called
//in your startForegroundService() custom function
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        val returnValue = super.onStartCommand(intent, flags, startId)

        //Gets the channel id passed from the MainActivity through the Intent
        val Id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")
        //Posts the notification task to the handler,
        //which will be executed on a different thread
        serviceHandler.post {
            //Sets up what happens after the notification is posted
            //Here, we're counting down from 10 to 0 in the notification
            countDownFromTenToZero(notificationBuilder)
            //Here we're notifying the MainActivity that the service process is done
            //by returning the channel ID through LiveData
            notifyCompletion(Id)
            //Stops the foreground service, which closes the notification
            //but the service still goes on
            stopForeground(STOP_FOREGROUND_REMOVE)
            //Stop and destroy the service
            stopSelf()
        }
        return returnValue
    }

    //A function to update the notification to display a count down from 10 to 0
    private fun countDownFromTenToZero(notificationBuilder:
                                       NotificationCompat.Builder) {
        //Gets the notification manager
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as
                NotificationManager
        //Count down from 10 to 0
        for (i in 10 downTo 0) {
            Thread.sleep(1000L)
            //Updates the notification content text
            notificationBuilder.setContentText("$i seconds until last warning")
                .setSilent(true)
            //Notify the notification manager about the content update
            notificationManager.notify(
                NOTIFICATION_ID,
                notificationBuilder.build()
            )
        }
    }

    //Update the LiveData with the returned channel id through the Main Thread
//the Main Thread is identified by calling the "getMainLooper()" method
//This function is called after the count down has completed
    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = Id
        }
    }


    companion object{
        const val NOTIFICATION_ID = 0xCA7
        const val EXTRA_ID = "Id"

        //ini LiveDara yg adalah dataholder automatically update UI based on yg diobserved
        //dia akan return channel ID ke LiveData setelah countdown reach 0, ngasitau klo service process selesai
        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}