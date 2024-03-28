package com.example.smsreceiver.manager

import android.util.Log
import com.example.smsreceiver.model.Sms
import com.example.smsreceiver.service.SendingService
import com.squareup.okhttp.Call
import com.squareup.okhttp.HttpUrl
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask


private const val QUERY_PARAMS_PHONE1 = "first_number"
private const val QUERY_PARAMS_PHONE2 = "second_number"

class SendingManager {
    private var url: String = ""
    private var phone1: String = ""
    private var phone2: String = ""
    var currentSms: Sms? = null
    val client = OkHttpClient()
    lateinit var request: Request
    private val PERIOD = 10000L
    private var timer: Timer? = null

    fun initHttp(url: String, phone1: String, phone2: String) {
        this.url = url
        this.phone1 = phone1
        this.phone2 = phone2
        val httpBuilder = HttpUrl.parse("$url/-api-/request").newBuilder()
        if (phone1.isNotEmpty()) {
            httpBuilder.addQueryParameter(QUERY_PARAMS_PHONE1, phone1)
        }
        if (phone2.isNotEmpty()) {
            httpBuilder.addQueryParameter(QUERY_PARAMS_PHONE2, phone2)
        }
        request = Request.Builder()
            .url(httpBuilder.build())
            .build()
    }


    fun launchRequest(throws: (String, Boolean) -> Unit, sendSMS: (Sms?) -> Unit) {
        Log.i(SendingService::class.simpleName, "Отправка началась")
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                try {
                    val respone = client.newCall(request).execute()
                    val sms = Sms.fromResponse(respone.body().string())
                    currentSms = sms
                    sendSMS(sms)
                } catch (e: Exception) {
                    throws(e.message!!, true)
                }

            }
        }, 0, PERIOD)
    }

    fun sendResponse(message: String, success: Boolean): Call {
        val jsonData = JSONObject()
        jsonData
            .accumulate("id", currentSms?.id)
            .accumulate("success", success)
            .accumulate("msg", message)
            .accumulate("phone", currentSms?.to)
        val requestBody = RequestBody.create(
            MediaType.parse("application/json"),
            jsonData.toString()
        )
        val requestStatus = Request.Builder()
            .url("$url/-api-/status")
            .post(requestBody)
            .build()
        return client.newCall(requestStatus)
    }


    fun cancel() {
        timer?.cancel()
        timer?.purge()
        timer = null
    }
}