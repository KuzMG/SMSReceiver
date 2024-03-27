package com.example.smsreceiver

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    var url = ""
    var phone1 = ""
    var phone2 = ""

    var serviceFlag = false
        set(value) {
            mutableServiceLiveData.value = value
            field = value
        }
    val liveData: LiveData<Boolean>
        get() = mutableLiveData
    private val mutableLiveData = MutableLiveData<Boolean>()

    val serviceLiveData: LiveData<Boolean>
        get() = mutableServiceLiveData
    private val mutableServiceLiveData = MutableLiveData<Boolean>()

    fun input(url: String, phone1: String, phone2: String) {
        this.url = url
        this.phone1 = phone1
        this.phone2 = phone2
        mutableLiveData.value = url.isNotEmpty() && (phone1.isNotEmpty() || phone2.isNotEmpty())

    }
}