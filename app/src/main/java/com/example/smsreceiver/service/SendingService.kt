package com.example.smsreceiver.service

import android.app.Activity
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.IBinder
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.example.smsreceiver.MainActivity
import com.example.smsreceiver.NOTIFICATION_CHANNEL_ID
import com.example.smsreceiver.R
import com.example.smsreceiver.SERVICE_PREF
import com.example.smsreceiver.SETTINGS
import com.example.smsreceiver.convertPhone
import com.example.smsreceiver.getSmsManager
import com.example.smsreceiver.manager.SendingManager
import com.example.smsreceiver.model.Sms
import com.example.smsreceiver.registerReceiverCompat
import com.example.smsreceiver.startForegroundService
import com.squareup.okhttp.Callback
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import java.io.IOException
import java.util.Timer
import java.util.TimerTask


val URL_EXTRA = "url"
val PHONE1_EXTRA = "phone1"
val PHONE2_EXTRA = "phone2"
val MESSAGE_EXTRA = "message"
val SERVICE_FLAG_EXTRA = "service_flag"
const val DELIVERED = "SMS_DELIVERED"
const val SENDED = "SMS_SENDED"
const val SMS_EXTRA = "SMS"
private const val NOTIFICATION_ID = 1

class SendingService : Service() {
    private val sendingManager = SendingManager()
    private var timerDelayResponse: Timer? = null
    private var receiverSmsStatusFlag = false
    private lateinit var settings: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val receiverSmsStatus = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, arg1: Intent) {
            sendingManager.cancel()
            when {
                resultCode == Activity.RESULT_OK -> {
                    registerReceiverCompat(
                        receiverSms,
                        IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
                    )
                    timerDelayResponse = Timer()
                    timerDelayResponse?.schedule(object : TimerTask() {
                        override fun run() {
                            sendResponse("Ответное СМС не пришло", false)
                        }
                    }, 2 * 60 * 1000)
                }

                else -> {
                    sendResponse("Сообщение не отправилось", false)
                }
            }
        }
    }

    private val receiverSms = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, arg1: Intent) {
            for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(arg1)) {
                if (convertPhone(smsMessage.originatingAddress!!) == sendingManager.currentSms?.to) {
                    cancelTimer()
                    sendResponse(smsMessage.messageBody, true)
                }
            }
        }
    }


    fun sendResponse(message: String, success: Boolean) {
        sendingManager.sendResponse(message, success).enqueue(object : Callback {
            override fun onFailure(request: Request?, e: IOException?) {
                exceptionBroadcast("ошибка отправки ответа на сервер ${e?.message}", false)
                stopSelf()
            }

            override fun onResponse(response: Response?) {
                if (response?.code() == 200) {
                    if (success) {
                        launchRequest()
                    } else {
                        exceptionBroadcast("информация об ошибки отправлена на сервер", false)
                        stopSelf()
                    }
                } else {
                    exceptionBroadcast(
                        "ошибка отправки ответа на сервер ${response?.code()}",
                        false
                    )
                    stopSelf()
                }
            }
        })
    }

    fun cancelTimer() {
        timerDelayResponse?.cancel()
        timerDelayResponse?.purge()
        timerDelayResponse = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            settings = getSharedPreferences(SETTINGS, MODE_PRIVATE)
            editor = settings.edit()
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(applicationContext, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentText(getString(R.string.notification_title))
                .setContentIntent(pendingIntent)
                .build()
            startForegroundService(this, NOTIFICATION_ID, notification)
            sendingManager.initHttp(
                intent?.getStringExtra(URL_EXTRA) ?: "",
                intent?.getStringExtra(PHONE1_EXTRA) ?: "",
                intent?.getStringExtra(PHONE2_EXTRA) ?: ""
            )
            launchRequest()
        } catch (e: Exception) {
            exceptionBroadcast(e.message!!, false)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    fun launchRequest() {
        sendingManager.launchRequest(::exceptionBroadcast) { sms ->
            if (sms == null) {
                exceptionBroadcast("Сообщение пустое", true)
            } else {
                sendSms(sms)
            }
        }
    }

    fun exceptionBroadcast(message: String, serviceFlag: Boolean) {
        sendBroadcast(Intent(DELIVERED).apply {
            putExtra(MESSAGE_EXTRA, message)
            putExtra(SERVICE_FLAG_EXTRA, serviceFlag)
        })
        editor.putBoolean(SERVICE_PREF,serviceFlag).apply()
    }


    fun sendSms(sms: Sms) {
        val smsService = getSmsManager(this, sms.sim)
        val sendingPI = PendingIntent.getBroadcast(
            this, 0,
            Intent(SENDED).putExtra(SMS_EXTRA, sms), PendingIntent.FLAG_IMMUTABLE
        )
        receiverSmsStatusFlag = true
        registerReceiverCompat(receiverSmsStatus, IntentFilter(SENDED))

        smsService?.sendTextMessage(sms.to, null, sms.msg, sendingPI, null)

    }


    override fun onDestroy() {
        super.onDestroy()
        if (receiverSmsStatusFlag) {
            unregisterReceiver(receiverSmsStatus)
            receiverSmsStatusFlag = false
        }
        cancelTimer()
        sendingManager.cancel()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}