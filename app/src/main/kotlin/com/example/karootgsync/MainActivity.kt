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

    private var phoneNumber: String = ""
    enum class AuthStep {
        PHONE, CODE, PASSWORD, READY
    }
    private var currentStep = AuthStep.PHONE

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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        android.net.Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    )
                    startActivity(intent)
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                    101
                )
            }
        }
    }

    private fun initPython() {
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(this))
            }
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val py = Python.getInstance()
                    val module = py.getModule("tg_sync")
                    
                    // Initialize Telethon client
                    val sessionDir = File(filesDir, "tg_sessions").absolutePath
                    module.callAttr("init_client", sessionDir)
                    
                    val isAuthorized = module.callAttr("is_authorized").toBoolean()
                    withContext(Dispatchers.Main) {
                        if (isAuthorized) {
                            currentStep = AuthStep.READY
                            tvStatus.text = "Logged In!"
                            appendLog("Logged into Telegram.")
                        } else {
                            currentStep = AuthStep.PHONE
                            tvStatus.text = "Waiting for Login"
                            etInput.hint = "Enter Phone Number (e.g. +1...)"
                            appendLog("Please log in with your phone number.")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        tvStatus.text = "Initialization Error"
                        appendLog("Error: ${e.message}")
                    }
                }
            }
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

                    when (currentStep) {
                        AuthStep.PHONE -> {
                            phoneNumber = input
                            withContext(Dispatchers.Main) { appendLog("Requesting code for $phoneNumber...") }
                            val res = module.callAttr("request_code", phoneNumber).toString()
                            withContext(Dispatchers.Main) {
                                if (res == "SUCCESS") {
                                    currentStep = AuthStep.CODE
                                    tvStatus.text = "Waiting for Code"
                                    etInput.setText("")
                                    etInput.hint = "Enter Telegram Code"
                                    appendLog("Code sent to Telegram app.")
                                } else {
                                    appendLog(res)
                                }
                            }
                        }
                        AuthStep.CODE -> {
                            withContext(Dispatchers.Main) { appendLog("Submitting code...") }
                            val res = module.callAttr("submit_code", phoneNumber, input, "").toString()
                            withContext(Dispatchers.Main) {
                                when (res) {
                                    "SUCCESS" -> {
                                        currentStep = AuthStep.READY
                                        tvStatus.text = "Logged In!"
                                        etInput.setText("")
                                        appendLog("Login successful!")
                                    }
                                    "PASSWORD_NEEDED" -> {
                                        currentStep = AuthStep.PASSWORD
                                        tvStatus.text = "Enter 2FA Password"
                                        etInput.setText("")
                                        etInput.hint = "Enter 2FA Password"
                                        appendLog("2-Factor Authentication enabled. Please enter your password:")
                                    }
                                    else -> {
                                        appendLog(res)
                                    }
                                }
                            }
                        }
                        AuthStep.PASSWORD -> {
                            withContext(Dispatchers.Main) { appendLog("Submitting password...") }
                            val res = module.callAttr("submit_code", phoneNumber, "", input).toString()
                            withContext(Dispatchers.Main) {
                                if (res == "SUCCESS") {
                                    currentStep = AuthStep.READY
                                    tvStatus.text = "Logged In!"
                                    etInput.setText("")
                                    appendLog("Login successful!")
                                } else {
                                    appendLog(res)
                                }
                            }
                        }
                        AuthStep.READY -> {
                            withContext(Dispatchers.Main) { appendLog("Already logged in.") }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { appendLog("Error: ${e.message}") }
                }
            }
        }

        btnSync.setOnClickListener {
            val chatId = etChatId.text.toString().trim()
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            
            val targetDir: String
            if (chatId.isEmpty()) {
                targetDir = musicDir.absolutePath
                appendLog("Starting sync for all chats in Telegram 'Music' folder...")
            } else {
                targetDir = File(musicDir, chatId.replace("@", "")).absolutePath
                appendLog("Starting sync for $chatId...")
            }

            btnSync.isEnabled = false

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
        android.util.Log.d("KarooTgSync", msg)
        tvLog.text = "$msg\n${tvLog.text}"
    }
}

interface SyncCallback {
    fun onProgress(message: String)
}
