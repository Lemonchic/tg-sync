package com.example.karootgsync

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var etInput: EditText
    private lateinit var btnSubmit: Button
    private lateinit var etChatId: EditText
    private lateinit var btnSync: Button
    private lateinit var tvLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        etInput = findViewById(R.id.etInput)
        btnSubmit = findViewById(R.id.btnSubmit)
        etChatId = findViewById(R.id.etChatId)
        btnSync = findViewById(R.id.btnSync)
        tvLog = findViewById(R.id.tvLog)

        checkPermissions()

        initPython()
        
        setupListeners()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                101
            )
        }
    }

    private fun initPython() {
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(this))
            }
            val py = Python.getInstance()
            val module = py.getModule("tg_sync")
            
            // Initialize Telethon client
            val sessionDir = File(filesDir, "tg_sessions").absolutePath
            module.callAttr("init_client", sessionDir)
            
            tvStatus.text = "TDLib Status: Ready / Waiting for Login"
            appendLog("Python initialized successfully.")
        } catch (e: Exception) {
            tvStatus.text = "Initialization Error"
            appendLog("Error: ${e.message}")
        }
    }

    private fun setupListeners() {
        btnSubmit.setOnClickListener {
            val input = etInput.text.toString().trim()
            if (input.isEmpty()) return@setOnClickListener

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val py = Python.getInstance()
                    val module = py.getModule("tg_sync")

                    // Simple state machine based on input
                    if (input.startsWith("+")) {
                        // It's a phone number
                        withContext(Dispatchers.Main) { appendLog("Requesting code for $input...") }
                        val res = module.callAttr("request_code", input).toString()
                        withContext(Dispatchers.Main) {
                            if (res == "SUCCESS") {
                                tvStatus.text = "Waiting for Code"
                                etInput.setText("")
                                etInput.hint = "Enter Telegram Code"
                                appendLog("Code sent to Telegram app.")
                            } else {
                                appendLog(res)
                            }
                        }
                    } else {
                        // It's a code
                        withContext(Dispatchers.Main) { appendLog("Submitting code...") }
                        val phone = "" // In a real app we'd save the phone number state, but our python script submits it.
                        // Wait, submit_code in python needs the phone number. Let's adjust the python script to not need phone for sign_in if we already have the request? 
                        // Actually, telethon sign_in requires phone. Let's fix this by keeping the phone number in Kotlin.
                        // For now, I will modify the python script to remember the phone number.
                        val res = module.callAttr("submit_code", "dummy", input, "").toString()
                        withContext(Dispatchers.Main) {
                            if (res == "SUCCESS") {
                                tvStatus.text = "Logged In!"
                                etInput.setText("")
                                appendLog("Login successful!")
                            } else {
                                appendLog(res)
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { appendLog("Error: ${e.message}") }
                }
            }
        }

        btnSync.setOnClickListener {
            val chatId = etChatId.text.toString().trim()
            if (chatId.isEmpty()) {
                appendLog("Please enter a chat ID or username.")
                return@setOnClickListener
            }

            btnSync.isEnabled = false
            appendLog("Starting sync for $chatId...")

            // Define target directory: /Music/<chatId>
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val targetDir = File(musicDir, chatId.replace("@", "")).absolutePath

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val py = Python.getInstance()
                    val module = py.getModule("tg_sync")
                    
                    val callback = object : SyncCallback {
                        override fun onProgress(message: String) {
                            runOnUiThread { appendLog(message) }
                        }
                    }

                    module.callAttr("sync_chat", chatId, targetDir, callback)

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { appendLog("Sync Error: ${e.message}") }
                } finally {
                    withContext(Dispatchers.Main) { btnSync.isEnabled = true }
                }
            }
        }
    }

    private fun appendLog(msg: String) {
        tvLog.append("$msg\n")
        // Scroll to bottom could be added here
    }
}

interface SyncCallback {
    fun onProgress(message: String)
}
