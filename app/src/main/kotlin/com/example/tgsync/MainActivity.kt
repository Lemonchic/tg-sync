package com.example.tgsync

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
import com.example.tgsync.R

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var etInput: EditText
    private lateinit var btnSubmit: Button
    private lateinit var btnSync: Button
    private lateinit var tvLog: TextView

    private lateinit var layoutLoading: android.view.View
    private lateinit var layoutLogin: android.view.View
    private lateinit var layoutSync: android.view.View
    private lateinit var layoutConfirmLogout: android.view.View

    private lateinit var btnLogout: Button
    private lateinit var btnConfirmLogout: Button
    private lateinit var btnCancelLogout: Button
    private lateinit var btnBackToPhone: Button
    private lateinit var btnExit: Button

    private var phoneNumber: String = ""
    private var deliveryMethod: String = "Telegram App"
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
        btnSync = findViewById(R.id.btnSync)
        tvLog = findViewById(R.id.tvLog)

        layoutLoading = findViewById(R.id.layoutLoading)
        layoutLogin = findViewById(R.id.layoutLogin)
        layoutSync = findViewById(R.id.layoutSync)
        layoutConfirmLogout = findViewById(R.id.layoutConfirmLogout)

        btnLogout = findViewById(R.id.btnLogout)
        btnConfirmLogout = findViewById(R.id.btnConfirmLogout)
        btnCancelLogout = findViewById(R.id.btnCancelLogout)
        btnBackToPhone = findViewById(R.id.btnBackToPhone)
        btnExit = findViewById(R.id.btnExit)

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

    private fun updateUiState() {
        runOnUiThread {
            layoutLoading.visibility = android.view.View.GONE
            if (currentStep == AuthStep.READY) {
                layoutLogin.visibility = android.view.View.GONE
                layoutSync.visibility = android.view.View.VISIBLE
            } else {
                layoutLogin.visibility = android.view.View.VISIBLE
                layoutSync.visibility = android.view.View.GONE
                
                val inputLayout = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.inputLayout)
                when (currentStep) {
                    AuthStep.PHONE -> {
                        tvStatus.text = "Waiting for Login"
                        inputLayout.hint = "Enter Phone Number"
                        etInput.hint = "Enter Phone Number"
                        btnBackToPhone.visibility = android.view.View.GONE
                    }
                    AuthStep.CODE -> {
                        tvStatus.text = "Code sent via $deliveryMethod\nto: $phoneNumber"
                        inputLayout.hint = "Enter Telegram Code"
                        etInput.hint = "Enter Telegram Code"
                        btnBackToPhone.visibility = android.view.View.VISIBLE
                    }
                    AuthStep.PASSWORD -> {
                        tvStatus.text = "2FA Password Required\nfor: $phoneNumber"
                        inputLayout.hint = "Enter 2FA Password"
                        etInput.hint = "Enter 2FA Password"
                        btnBackToPhone.visibility = android.view.View.VISIBLE
                    }
                    else -> {}
                }
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
                    
                    val sessionDir = File(filesDir, "tg_sessions").absolutePath
                    module.callAttr("init_client", sessionDir)
                    
                    val isAuthorized = module.callAttr("is_authorized").toBoolean()
                    withContext(Dispatchers.Main) {
                        if (isAuthorized) {
                            currentStep = AuthStep.READY
                            appendLog("Logged into Telegram.")
                        } else {
                            currentStep = AuthStep.PHONE
                            appendLog("Please log in with your phone number.")
                        }
                        updateUiState()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        tvStatus.text = "Initialization Error: ${e.message}"
                        appendLog("Error: ${e.message}")
                        updateUiState()
                    }
                }
            }
        } catch (e: Exception) {
            tvStatus.text = "Initialization Error: ${e.message}"
            appendLog("Error: ${e.message}")
            updateUiState()
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
                            withContext(Dispatchers.Main) { 
                                tvStatus.text = "Requesting code..."
                                appendLog("Requesting code for $phoneNumber...") 
                            }
                            val res = module.callAttr("request_code", phoneNumber).toString()
                            withContext(Dispatchers.Main) {
                                if (res.startsWith("SUCCESS")) {
                                    currentStep = AuthStep.CODE
                                    etInput.setText("")
                                    deliveryMethod = res.substringAfter("SUCCESS:", "Telegram App")
                                    appendLog("Code sent to $deliveryMethod.")
                                } else {
                                    tvStatus.text = res
                                    appendLog(res)
                                }
                                updateUiState()
                            }
                        }
                        AuthStep.CODE -> {
                            withContext(Dispatchers.Main) { 
                                tvStatus.text = "Submitting code..."
                                appendLog("Submitting code...") 
                            }
                            val res = module.callAttr("submit_code", phoneNumber, input, "").toString()
                            withContext(Dispatchers.Main) {
                                when (res) {
                                    "SUCCESS" -> {
                                        currentStep = AuthStep.READY
                                        etInput.setText("")
                                        appendLog("Login successful!")
                                    }
                                    "PASSWORD_NEEDED" -> {
                                        currentStep = AuthStep.PASSWORD
                                        etInput.setText("")
                                        tvStatus.text = "2FA Password Required"
                                        appendLog("2-Factor Authentication enabled. Please enter your password:")
                                    }
                                    else -> {
                                        tvStatus.text = res
                                        appendLog(res)
                                    }
                                }
                                updateUiState()
                            }
                        }
                        AuthStep.PASSWORD -> {
                            withContext(Dispatchers.Main) { 
                                tvStatus.text = "Submitting password..."
                                appendLog("Submitting password...") 
                            }
                            val res = module.callAttr("submit_code", phoneNumber, "", input).toString()
                            withContext(Dispatchers.Main) {
                                if (res == "SUCCESS") {
                                    currentStep = AuthStep.READY
                                    etInput.setText("")
                                    appendLog("Login successful!")
                                } else {
                                    tvStatus.text = res
                                    appendLog(res)
                                }
                                updateUiState()
                            }
                        }
                        AuthStep.READY -> {
                            withContext(Dispatchers.Main) { appendLog("Already logged in.") }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { 
                        tvStatus.text = "Error: ${e.message}"
                        appendLog("Error: ${e.message}") 
                    }
                }
            }
        }

        btnBackToPhone.setOnClickListener {
            tvStatus.text = "Resetting connection..."
            btnBackToPhone.isEnabled = false
            btnSubmit.isEnabled = false
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val py = Python.getInstance()
                    val module = py.getModule("tg_sync")
                    val sessionDir = File(filesDir, "tg_sessions").absolutePath
                    module.callAttr("reset_client", sessionDir)
                    withContext(Dispatchers.Main) {
                        currentStep = AuthStep.PHONE
                        etInput.setText("")
                        tvStatus.text = "Waiting for Login"
                        btnBackToPhone.isEnabled = true
                        btnSubmit.isEnabled = true
                        updateUiState()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        tvStatus.text = "Reset Error: ${e.message}"
                        btnBackToPhone.isEnabled = true
                        btnSubmit.isEnabled = true
                        updateUiState()
                    }
                }
            }
        }

        btnLogout.setOnClickListener {
            layoutConfirmLogout.visibility = android.view.View.VISIBLE
        }

        btnCancelLogout.setOnClickListener {
            layoutConfirmLogout.visibility = android.view.View.GONE
        }

        btnExit.setOnClickListener {
            finishAndRemoveTask()
        }

        btnConfirmLogout.setOnClickListener {
            layoutConfirmLogout.visibility = android.view.View.GONE
            appendLog("Logging out...")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val py = Python.getInstance()
                    val module = py.getModule("tg_sync")
                    val res = module.callAttr("logout").toString()
                    withContext(Dispatchers.Main) {
                        if (res == "SUCCESS") {
                            currentStep = AuthStep.PHONE
                            appendLog("Logged out from Telegram.")
                            updateUiState()
                        } else {
                            appendLog("Logout error: $res")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { appendLog("Logout Error: ${e.message}") }
                }
            }
        }

        btnSync.setOnClickListener {
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val targetDir = musicDir.absolutePath
            appendLog("Starting sync for all chats in Telegram 'Music' folder...")

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

                    module.callAttr("sync_chat", "", targetDir, callback)

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
