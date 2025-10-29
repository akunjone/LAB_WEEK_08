package com.example.lab_week_08

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.lab_week_08.NotificationService.Companion.EXTRA_ID
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker

class MainActivity : AppCompatActivity() {
    //create an instance of a work manager
    //work manager manages all your requests and workers
    //it also sets up the sequenct for all your processes
    private val workManager = WorkManager.getInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
        //create a constraint of which your workers are bound to
        //here the workers cannot execute the given process if
        //there's no internet connection
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val Id = "001"

        //2 types of work request:
        //OneTimeWorkRequest and PeriodWorkRequest
        //OneTimeWorkRequest exec the request just once
        //PeriodicWorkRequest exec periodically

        //one time work request that includes all constraints and inputs needed for the worker
        //created request for FirstWorker class
        val firstRequest = OneTimeWorkRequest
            .Builder(FirstWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(FirstWorker
                .INPUT_DATA_ID, Id)
            ).build()

        //ini buat second worker
        val secondRequest = OneTimeWorkRequest
            .Builder(SecondWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(SecondWorker
                .INPUT_DATA_ID, Id)
            ).build()

        //sets up the process sequence from the work manager instance
        //here starts with firstworker, then secondworker
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .enqueue()

        //skrg getting outputnya
        //kita receive output, display result as toast
        //ada LiveData dan observe:
        //LiveData = data holder class in Android Jetpack supaya lebih reactive
        //reaktif itu datang dari observe keyword
        //observing data changes and immediately update app UI

        //kita observe returned LiveData and getting state result of worker (SUCCEEDED, FAILED, CANCELLED)
        //isFinished untuk cek apakah state SUCCEEDED / FAILED
        workManager.getWorkInfoByIdLiveData(firstRequest.id)
            .observe(this){ info ->
                if(info != null && info.state.isFinished){
                    showResult("First process is done")
                }
            }

        workManager.getWorkInfoByIdLiveData(secondRequest.id)
            .observe(this){ info ->
                if(info != null && info.state.isFinished){
                    showResult("Second process is done")
                    //dilaunch setelah second worker done:
                    launchNotificationService()
                }
            }
    }

    //FUNCTIONS
    //build data into the correct format before passing it to the worker as input
    private fun getIdInputData(IdKey: String, IdValue: String) =
        Data.Builder()
            .putString(IdKey, IdValue)
            .build()

    //show result as toast
    private fun showResult(message: String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    //Launch the NotificationService
    private fun launchNotificationService() {
        //Observe if the service process is done or not
        //If it is, show a toast with the channel ID in it
        NotificationService.trackingCompletion.observe(
            this) { Id ->
            showResult("Process for Notification Channel ID $Id is done!")
        }

        //Create an Intent to start the NotificationService
        //An ID of "001" is also passed as the notification channel ID
        val serviceIntent = Intent(
            this,
            NotificationService::class.java
        ).apply {
            putExtra(EXTRA_ID, "001")
        }

        //Start the foreground service through the Service Intent
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    companion object{
        const val EXTRA_ID = "Id"
    }
}