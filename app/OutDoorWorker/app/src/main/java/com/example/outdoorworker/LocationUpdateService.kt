package com.example.outdoorworker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationCallback

import com.google.android.gms.location.LocationResult

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import android.Manifest
import android.os.Binder


class LocationUpdateService : Service() {

    private val channelId = "LocationUpdateServiceChannel"
    private val notificationId = 1

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    class LocationUpdateService : Service() {



        inner class LocalBinder : Binder() {
            fun getService(): LocationUpdateService = this@LocationUpdateService
        }

        override fun onBind(intent: Intent): IBinder {
            return LocalBinder()
        }


    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Update Service")
            .setContentText("Updating location...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        // Start the service in the foreground to prevent it from being killed by the system when the app is in the background
        startForeground(notificationId, notification)

        val locationRequest = LocationRequest.create().apply {
            interval = 60000 // Update location every 10 seconds
            fastestInterval = 5000 // If location is available sooner, update it
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    // Update Firestore with the new location
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
                        }
                    }
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(this)
                .requestLocationUpdates(locationRequest, locationCallback, null)
        } else {

        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "Location Update Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}