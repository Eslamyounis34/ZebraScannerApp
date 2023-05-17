package com.example.zebrascannerapp

import android.os.Bundle
import android.provider.Contacts
import android.provider.Contacts.PresenceColumns.IDLE
import android.util.Log
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.symbol.emdk.EMDKBase
import com.symbol.emdk.EMDKManager
import com.symbol.emdk.EMDKResults
import com.symbol.emdk.barcode.*
import com.symbol.emdk.barcode.Scanner.DataListener
import com.symbol.emdk.barcode.Scanner.StatusListener
import com.symbol.emdk.barcode.StatusData.ScannerStates


class MainActivity : AppCompatActivity(), EMDKManager.EMDKListener, StatusListener,
    DataListener {
    private lateinit var emdkManager: EMDKManager

    private var barcodeManager: BarcodeManager? = null
    private var scanner: Scanner? = null
    private var statusTextView: TextView? = null
    private var dataView: EditText? = null
    var dataStr = ""
    private var dataLength = 0
    var statusStr = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.textViewStatus)
        dataView = findViewById(R.id.editText1);

        val results = EMDKManager.getEMDKManager(applicationContext, this)
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            updateStatus("EMDKManager object request failed!");
            return;
        } else {
            updateStatus("EMDKManager object initialization is   in   progress.......");
        }

    }

    private fun updateStatus(status: String) {
        runOnUiThread {
            Log.e("testBarCode", status)
            statusTextView!!.setText("" + status);
        }
    }

    private fun setConfig() {
        if (scanner != null) {
            try {
                // Get scanner config
                val config = scanner!!.config
                config.decoderParams.ean13.enabled = true
                // Enable haptic feedback
                if (config.isParamSupported("config.scanParams.decodeHapticFeedback")) {
                    config.scanParams.decodeHapticFeedback = true
                }
                // Set scanner config
                scanner!!.config = config
            } catch (e: ScannerException) {
                updateStatus(e.message!!)
            }
        }
    }

    override fun onOpened(manager: EMDKManager?) {
        this.emdkManager = manager!!
        initBarcodeManager()
        initScanner()

    }

    private fun deInitScanner() {
        if (scanner != null) {
            try {
                // Release the scanner
                scanner!!.release()
            } catch (e: Exception) {
                updateStatus(e.message!!)
            }
        }
    }

    private fun initScanner() {
        if (scanner == null) {
            // Get default scanner defined on the device
            scanner = barcodeManager!!.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT)
            if (scanner != null) {
                // Implement the DataListener interface and pass the pointer of this object to get the data callbacks.
                scanner!!.addDataListener(this)

                // Implement the StatusListener interface and pass the pointer of this object to get the status callbacks.
                scanner!!.addStatusListener(this)

                // Hard trigger. When this mode is set, the user has to manually
                // press the trigger on the device after issuing the read call.
                // NOTE: For devices without a hard trigger, use TriggerType.SOFT_ALWAYS.
                scanner!!.triggerType = Scanner.TriggerType.HARD
                try {
                    // Enable the scanner
                    // NOTE: After calling enable(), wait for IDLE status before calling other scanner APIs
                    // such as setConfig() or read().
                    scanner!!.enable()
                } catch (e: ScannerException) {
                    updateStatus(e.message!!)
                    deInitScanner()
                }
            } else {
                updateStatus("Failed to   initialize the scanner device.")
            }
        }
    }

    private fun initBarcodeManager() {
        barcodeManager = emdkManager.getInstance(EMDKManager.FEATURE_TYPE.BARCODE) as BarcodeManager
        if (barcodeManager == null) {
            Toast.makeText(this, "Barcode scanning is not supported.", Toast.LENGTH_LONG).show();
            finish();
        }
    }


    override fun onClosed() {
        if (emdkManager != null) {
            emdkManager.release()
        }
        updateStatus("EMDK closed unexpectedly! Please close and restart the application.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (emdkManager != null) {
            emdkManager.release()
        }
    }


    override fun onData(scanDataCollection: ScanDataCollection?) {
        if ((scanDataCollection != null) && (scanDataCollection.result) == ScannerResults.SUCCESS) {
            val scanData = scanDataCollection.scanData
            for (data in scanData) {
                var barcodeData = data.data
                var labelType = data.labelType
                dataStr = barcodeData 
            }
            updateData(dataStr)
        }
    }

    private fun updateData(result: String) {
        runOnUiThread {
            if (dataLength++ >= 50) {
                dataView!!.text.clear()
                dataLength = 0
            }
            dataView!!.append(result + "\n")

        }
    }

    override fun onStatus(statusData: StatusData?) {
        var state: ScannerStates = statusData!!.state
        when (state) {
            ScannerStates.IDLE -> {
                statusStr = statusData.getFriendlyName() + " is   enabled and idle..."
                setConfig()
                try {
                    scanner!!.read()
                } catch (e: ScannerException) {
                    updateStatus(e.message.toString())
                }
            }
            ScannerStates.ERROR -> {
                statusStr = "An error has occurred."
            }
            ScannerStates.SCANNING -> {
                statusStr = "Scanning..."
            }
            ScannerStates.DISABLED -> {
                statusStr = "Disabled..."
            }
            ScannerStates.WAITING -> {
                statusStr = "Scanner is waiting for trigger press...";
            }
        }

    }
}


