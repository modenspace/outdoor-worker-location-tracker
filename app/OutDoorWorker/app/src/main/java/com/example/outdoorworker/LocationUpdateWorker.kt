package com.example.outdoorworker


import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import android.Manifest
import android.util.Log

class LocationUpdateWorker(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams) {


        // use suppression to ignore the missing permission warning
    //@SuppressLint("MissingPermission")
        override suspend fun doWork(): Result {
            if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return Result.failure()
            }

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
            val locationTask = fusedLocationClient.lastLocation
            val location = Tasks.await(locationTask, 10, TimeUnit.SECONDS)

            if (location != null) {
                Log.d("LocationUpdateWorker", "Location retrieved: ${location.latitude}, ${location.longitude}")
                val user = FirebaseAuth.getInstance().currentUser
                user?.let {
                    val db = FirebaseFirestore.getInstance()
                    val docRef = db.collection("users").document(it.uid)

                    db.runTransaction { transaction ->
                        val snapshot = transaction.get(docRef)
                        val locations = snapshot.get("locations") as? MutableList<String> ?: mutableListOf()
                        locations.add(0, "${location.latitude},${location.longitude}")
                        if (locations.size > 5) {
                            locations.removeAt(5)
                        }
                        transaction.update(docRef, "locations", locations)
                    }.addOnSuccessListener {
                        Log.d("LocationUpdateWorker", "Firestore document updated")
                    }.addOnFailureListener { e ->
                        Log.d("LocationUpdateWorker", "Error updating Firestore document: ${e.message}")
                    }
                }
            } else {
                Log.d("LocationUpdateWorker", "Location is null")
            }
            return Result.success()
        }
}