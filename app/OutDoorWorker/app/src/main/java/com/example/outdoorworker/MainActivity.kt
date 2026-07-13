package com.example.outdoorworker

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

//my imports for logout
import android.content.Intent
import android.widget.Button
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

//my imports for clock in and clock out
import android.Manifest
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import androidx.appcompat.app.AlertDialog

//import for work manager
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    //my variables for clock in and clock out
    private lateinit var fusedLocationClient: FusedLocationProviderClient

//     workamanager but im using it
    private val workManager = WorkManager.getInstance(this)

//    // variables for location updates
//    private var locationUpdateService: LocationUpdateService? = null
//    private var isServiceBound = false
//
//    private val serviceConnection = object : ServiceConnection {
//        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//            val binder = service as LocationUpdateService.LocalBinder
//            locationUpdateService = binder.getService()
//            isServiceBound = true
//        }
//
//        override fun onServiceDisconnected(name: ComponentName?) {
//            locationUpdateService = null
//            isServiceBound = false
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //--------------------------------------------------------------------------------
        //code for logout
        // Find the logout button and set an OnClickListener
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            // Sign out the user
            FirebaseAuth.getInstance().signOut()

            // Sign out the user from GoogleSignInClient
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInClient.signOut()



            // Navigate back to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
           //--------------------------------------------------------------------------------



        }

        //my code for clock in and clock out

        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val clockInButton = findViewById<Button>(R.id.clockInButton)
        val clockOutButton = findViewById<Button>(R.id.clockOutButton)


        clockOutButton.isEnabled = false

        clockInButton.setOnClickListener {
            // Check if the app has the ACCESS_FINE_LOCATION and ACCESS_BACKGROUND_LOCATION permissions
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // If not, request the permissions
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION), 0)
            } else {
                // If the permission is granted, enqueue the LocationUpdateWorker
                val workRequest = PeriodicWorkRequestBuilder<LocationUpdateWorker>(15, TimeUnit.MINUTES).build()
                workManager.enqueue(workRequest)

//                // If the permission is granted, start the locationupdateservice
//                val intent = Intent(this, LocationUpdateService::class.java)
//                startService(intent)


                // Get the current date and time
                val currentTime = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())

                // Update the Firestore document for the current user
                val db = FirebaseFirestore.getInstance()
                val user = FirebaseAuth.getInstance().currentUser
                user?.let {
                    db.collection("users").document(it.uid).update(
                        "clockIn", currentTime,
                        "clockOut", "",
                        "locations", listOf<String>()
                    ).addOnSuccessListener {
                        Toast.makeText(this, "Clock in successful", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                // after clocking in, enable the clock out button and disable the clock in button
                clockInButton.isEnabled = false
                clockOutButton.isEnabled = true
                logoutButton.isEnabled = false

            }

        }



        clockOutButton.setOnClickListener {
            // Get the current date and time
            val currentTime = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())

          //stop the location updates
          workManager.cancelAllWork()

//            //stop the location updates
//            val serviceIntent = Intent(this, LocationUpdateService::class.java)
//            stopService(serviceIntent)
//            if (isServiceBound) {
//                locationUpdateService?.stopLocationUpdates()
//                unbindService(serviceConnection)
//                isServiceBound = false
//            }

            // Update the Firestore document for the current user
            val db = FirebaseFirestore.getInstance()
            val user = FirebaseAuth.getInstance().currentUser
            user?.let {
                db.collection("users").document(it.uid).update(
                    "clockOut", currentTime
                ).addOnSuccessListener {
                    Toast.makeText(this, "Clock out successful", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            // after clocking out, enable the clock in button and disable the clock out button
            clockInButton.isEnabled = true
            clockOutButton.isEnabled = false
            logoutButton.isEnabled = true
        }


    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            0 -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission was granted
                    Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    // Permission was denied
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()

                    // Show a dialog explaining why the app needs the location permission
                    AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission to clock in and clock out. Please grant the permission in your device settings.")
                        .setPositiveButton("OK") { _, _ ->
                            // Open the app's system settings
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                        .show()
                }
                return
            }
            else -> {
                // Ignore all other requests
            }
        }
    }



}