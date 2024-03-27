package com.example.smsreceiver

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.telephony.SmsManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getSystemService

fun Context.registerReceiverCompat(receiver: BroadcastReceiver, filter: IntentFilter) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        registerReceiver(
            receiver, filter,
            AppCompatActivity.RECEIVER_EXPORTED
        )
    } else {
        registerReceiver(receiver, filter)
    }
}

inline fun <reified T : Any> Bundle.getParcelableArray(key: String): Array<out Any>? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getParcelableArray("pdus", T::class.java)
    } else {
        this.getParcelableArray("pdus")
    }

inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        getParcelableExtra(key)
    }

fun startForegroundService(context: Service, id: Int, notofication: Notification) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.startForeground(
            id,
            notofication,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
        )
    } else {
        context.startForeground(id, notofication)
    }
}

fun getSmsManager(context: Service, sim: Int) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(context, SmsManager::class.java)!!.createForSubscriptionId(sim)
    } else {
        getSystemService(context, SmsManager::class.java)
    }


fun convertPhone(phone: String) =
    if (phone[0] == '+') {
        phone.drop(1)
    } else if (phone[0] == '8') {
        phone.replace('8', '7')
    } else {
        phone
    }