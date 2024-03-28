package com.example.smsreceiver

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.smsreceiver.model.Sms
import com.example.smsreceiver.service.DELIVERED
import com.example.smsreceiver.service.MESSAGE_EXTRA
import com.example.smsreceiver.service.PHONE1_EXTRA
import com.example.smsreceiver.service.PHONE2_EXTRA
import com.example.smsreceiver.service.SENDED
import com.example.smsreceiver.service.SERVICE_FLAG_EXTRA
import com.example.smsreceiver.service.SMS_EXTRA
import com.example.smsreceiver.service.SendingService
import com.example.smsreceiver.service.URL_EXTRA

const val SETTINGS = "settings"
private const val URL_PREF = "url"
private const val PHONE1_PREF = "phone1"
private const val PHONE2_PREF = "phone2"
const val SERVICE_PREF = "service"

class MainActivity : AppCompatActivity() {

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                check()
            }
        }
    private val receiverSmsStatus = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, arg1: Intent) {
            when {
                resultCode == Activity.RESULT_OK -> {
                    val sms = arg1.getParcelableExtraCompat<Sms>(SMS_EXTRA)
                    smsInfoTextView.setText("$sms \nотправлено")
                }
            }
        }
    }

    private val receiverGeneral = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, arg1: Intent) {
            val message = arg1.getStringExtra(MESSAGE_EXTRA)
            val serviceFlag = arg1.getBooleanExtra(SERVICE_FLAG_EXTRA, false)
            when {
                resultCode == Activity.RESULT_OK -> {
                    smsInfoTextView.setText(message)
                    if(viewModel.serviceFlag)
                        viewModel.serviceFlag = serviceFlag
                }
            }
        }
    }
    val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable) {

            viewModel.input(

                urlEditText.text.toString(),
                phone1EditText.text.toString(),
                phone2EditText.text.toString()
            )
        }

    }

    private lateinit var phone1EditText: EditText
    private lateinit var phone2EditText: EditText
    private lateinit var urlEditText: EditText
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var smsInfoTextView: TextView
    private lateinit var viewModel: MainViewModel
    private lateinit var settings: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        settings = getSharedPreferences(SETTINGS, MODE_PRIVATE)
        editor = settings.edit()
        urlEditText = findViewById(R.id.url_edit_text)
        phone1EditText = findViewById(R.id.phone1_edit_text)
        phone2EditText = findViewById(R.id.phone2_edit_text)
        startButton = findViewById(R.id.start_button)
        stopButton = findViewById(R.id.stop_button)
        smsInfoTextView = findViewById(R.id.sms_info_text_view)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        viewModel.liveData.observe(this) {
            if(!viewModel.serviceFlag){
                startButton.isEnabled = it
            }
        }
        viewModel.serviceLiveData.observe(this) {
            if (it)
                startButton.isEnabled = !it
            else if(urlEditText.text.isNotEmpty() && (phone1EditText.text.isNotEmpty() || phone2EditText.text.isNotEmpty())){
                startButton.isEnabled = !it
            }
            stopButton.isEnabled = it
        }

    }

    override fun onStart() {
        super.onStart()
        urlEditText.addTextChangedListener(textWatcher)
        phone1EditText.addTextChangedListener(textWatcher)
        phone2EditText.addTextChangedListener(textWatcher)

        startButton.setOnClickListener {
            check()
        }
        stopButton.setOnClickListener {
            stopSending()
        }
        settings.run {
            urlEditText.setText(getString(URL_PREF, "")!!)
            phone1EditText.setText(getString(PHONE1_PREF, "")!!)
            phone2EditText.setText(getString(PHONE2_PREF, "")!!)
            viewModel.serviceFlag = getBoolean(SERVICE_PREF, false)
        }
    }

    fun startSending() {
        viewModel.serviceFlag = true
        ContextCompat.startForegroundService(
            this,
            Intent(this, SendingService::class.java)
                .putExtra(URL_EXTRA, viewModel.url)
                .putExtra(PHONE1_EXTRA, viewModel.phone1)
                .putExtra(PHONE2_EXTRA, viewModel.phone2)
        )
    }

    fun stopSending() {
        viewModel.serviceFlag = false
        stopService(Intent(this, SendingService::class.java))
    }

    fun check() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission.launch(Manifest.permission.SEND_SMS)
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECEIVE_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission.launch(Manifest.permission.RECEIVE_SMS)
        } else {
            startSending()
        }
//                this.shouldShowRequestPermissionRationale(Manifest.permission.SEND_SMS) -> {
//                    Toast.makeText(
//                        this,
//                        "Вы запретили отправлять сообщения!",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
    }


    override fun onResume() {
        super.onResume()
        registerReceiverCompat(receiverGeneral, IntentFilter(DELIVERED))
        registerReceiverCompat(receiverSmsStatus, IntentFilter(SENDED))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiverGeneral)
        unregisterReceiver(receiverSmsStatus)
    }

    override fun onStop() {
        super.onStop()
        editor.apply {
            putString(URL_PREF, viewModel.url)
            putString(PHONE1_PREF, viewModel.phone1)
            putString(PHONE2_PREF, viewModel.phone2)
            putBoolean(SERVICE_PREF, viewModel.serviceFlag)
        }.apply()
    }
}