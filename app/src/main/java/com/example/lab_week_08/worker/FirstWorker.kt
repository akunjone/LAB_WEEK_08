package com.example.lab_week_08.worker

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters

class FirstWorker(
    context: Context, workerParams: WorkerParameters
) : Worker(context, workerParams){
    override fun doWork(): Result{
        //get the parameter input
        val id = inputData.getString(INPUT_DATA_ID)
        //sleep the process for 3s
        Thread.sleep(3000L)
        //build output based on process result
        val outputData = Data.Builder()
            .putString(OUTPUT_DATA_ID, id)
            .build()

        //return output
        return Result.success(outputData)
    }
    companion object{
        const val INPUT_DATA_ID = "inId"
        const val OUTPUT_DATA_ID = "outId"
    }
}