package com.example.micselector

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Senior Android Developer - Microphone Selector Implementation (Arabic)
 * This activity handles audio device enumeration and forces selection
 * using AudioRecord.setPreferredDevice() with VOICE_COMMUNICATION source.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var audioManager: AudioManager
    private var audioRecord: AudioRecord? = null
    
    // UI Elements
    private lateinit var btnPhoneMic: Button
    private lateinit var btnHeadsetMic: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvInfo: TextView

    companion object {
        private const val REQUEST_RECORD_AUDIO = 200
        private const val SAMPLE_RATE = 44100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize AudioManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        initUI()
        checkPermissions()
        
        // Register callback for audio device changes (headset plug/unplug)
        // This ensures the headset button updates automatically
        audioManager.registerAudioDeviceCallback(object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                updateDeviceList()
            }
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                updateDeviceList()
            }
        }, null)

        // Initial update of the device list
        updateDeviceList()
    }

    private fun initUI() {
        btnPhoneMic = findViewById(R.id.btnPhoneMic)
        btnHeadsetMic = findViewById(R.id.btnHeadsetMic)
        tvStatus = findViewById(R.id.tvStatus)
        tvInfo = findViewById(R.id.tvInfo)

        // Set listeners for buttons
        btnPhoneMic.setOnClickListener { selectMicrophone(AudioDeviceInfo.TYPE_BUILTIN_MIC) }
        btnHeadsetMic.setOnClickListener { selectMicrophone(AudioDeviceInfo.TYPE_WIRED_HEADSET) }
    }

    /**
     * Detects available microphones using getDevices(GET_DEVICES_INPUTS)
     * and updates button states and info text in Arabic.
     */
    private fun updateDeviceList() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        var headsetAvailable = false

        // Check for wired headset or bluetooth headset
        for (device in devices) {
            if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || 
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                headsetAvailable = true
            }
        }

        runOnUiThread {
            btnHeadsetMic.isEnabled = headsetAvailable
            if (headsetAvailable) {
                tvInfo.text = "" // Clear "not connected" message
            } else {
                tvInfo.text = getString(R.string.headset_not_connected)
            }
        }
    }

    /**
     * Forces the system to use a specific microphone.
     * Uses AudioRecord.setPreferredDevice() with VOICE_COMMUNICATION source.
     */
    private fun selectMicrophone(deviceType: Int) {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        val targetDevice = devices.find { it.type == deviceType }

        if (targetDevice == null) {
            Toast.makeText(this, getString(R.string.device_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Initialize AudioRecord if not already done
            // Using VOICE_COMMUNICATION as requested
            if (audioRecord == null) {
                val bufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                    )
                    // Start recording to ensure the device routing is applied
                    audioRecord?.startRecording()
                } else {
                    Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_LONG).show()
                    return
                }
            }

            // Apply the preferred device routing
            val success = audioRecord?.setPreferredDevice(targetDevice) ?: false
            
            if (success) {
                val deviceName = when(deviceType) {
                    AudioDeviceInfo.TYPE_BUILTIN_MIC -> getString(R.string.status_phone)
                    AudioDeviceInfo.TYPE_WIRED_HEADSET -> getString(R.string.status_headset)
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> getString(R.string.status_bluetooth)
                    else -> targetDevice.productName.toString()
                }
                
                tvStatus.text = "${getString(R.string.status_prefix)} $deviceName"
                Toast.makeText(this, "${getString(R.string.switched_to)} $deviceName", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.failed_switch), Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Handles runtime permission for RECORD_AUDIO.
     */
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                // Show Arabic message if permission denied
                Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release resources
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
