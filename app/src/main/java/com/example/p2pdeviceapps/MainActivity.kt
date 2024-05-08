package com.example.p2pdeviceapps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.p2pdeviceapps.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
// wifi connect was modifed version from yatapone github "samplewificonnector"

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 999
    }

    private var permissionRejected: Boolean = false
    private var isDialogDisplayed: Boolean = false
    private lateinit var binding: ActivityMainBinding
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiScanReceiver: BroadcastReceiver
    private lateinit var suggestionPostConnectionReceiver: BroadcastReceiver
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager

        wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                Log.d(TAG, "onReceive: success=$success")
                // success or failure processing
            }
        }
        binding.btnConnect.setOnClickListener() {
            connectToESP()
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilter)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: ")
        updateConnectionStatus()
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onResume: permission.ACCESS_FINE_LOCATION is granted.")
        } else {
            Log.d(TAG, "onResume: permission.ACCESS_FINE_LOCATION is required.")
            if (!permissionRejected) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_CODE
                )
            } else if (!isDialogDisplayed) {
                isDialogDisplayed = true
                AlertDialog.Builder(this)
                    .setTitle("a permission is required")
                    .setMessage("ACCESS_FINE_LOCATION permission is required for scanning Wi-Fi list. Restart app and grant ACCESS_FINE_LOCATION permission.")
                    .setPositiveButton("OK") { _, _ ->
                        finish()
                    }
                    .show()
            }
        }


        connectivityManager.registerDefaultNetworkCallback(object :
            ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "onAvailable: The default network is now: $network")
            }

            override fun onLost(network: Network) {
                Log.d(
                    TAG,
                    "onLost: The application no longer has a default network. The last default network was $network"
                )
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                Log.d(
                    TAG,
                    "onCapabilitiesChanged: The default network changed capabilities: $networkCapabilities"
                )
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                Log.d(
                    TAG,
                    "onLinkPropertiesChanged: The default network changed link properties: $linkProperties"
                )
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(
            TAG,
            "onRequestPermissionsResult: requestCode=$requestCode, permissions=${permissions[0]}, grantResults=${grantResults[0]}"
        )
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            permissionRejected = true
        }
    }

    private fun updateConnectionStatus() {
        val networkInfo = connectivityManager.activeNetworkInfo
        if (networkInfo != null && networkInfo.isConnected) {
            val wifiInfo = wifiManager.connectionInfo
            if (wifiInfo != null && wifiInfo.ssid == "\"ESP32Master\"") {
                // Device is connected to the ESP32Master network
                binding.txtConnected.text = "Connected"
            } else {
                // Device is connected to some other network
                binding.txtConnected.text = "Not Connected"
            }
        } else {
            // Device is not connected to any network
            binding.txtConnected.text = "Not Connected"
        }
    }


    private fun connectToESP() {
        val ssid = "ESP32Master"
        val pass = "password"
        Log.d(TAG, "connectByWifiNetworkSpecifier: ssid=$ssid, pass=$pass")
        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(pass) // Assuming WPA2 security, change as necessary

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifier.build())
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d(TAG, "onAvailable: network=$network")
                // do success processing here..
            }

            override fun onUnavailable() {
                super.onUnavailable()
                Log.d(TAG, "onAvailable: ")
                // do failure processing here..
            }
        }
        connectivityManager.requestNetwork(request, networkCallback)
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: ")
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        unregisterReceiver(wifiScanReceiver)

        try {
            unregisterReceiver(suggestionPostConnectionReceiver)
        } catch (e: Exception) {
            Log.d(TAG, "onDestroy: unregisterReceiver: e=$e")
        }

        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.d(TAG, "onDestroy: unregisterNetworkCallback: e=$e")
        }

        super.onDestroy()
    }
}