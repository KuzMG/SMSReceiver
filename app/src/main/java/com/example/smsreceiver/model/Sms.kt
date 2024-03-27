package com.example.smsreceiver.model

import android.os.Parcel
import android.os.Parcelable
import org.json.JSONObject

data class Sms(val id: Int,val to: String, val msg: String, val sim: Int) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readInt()
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeString(to)
        dest.writeString(msg)
        dest.writeInt(sim)
    }

    companion object CREATOR : Parcelable.Creator<Sms> {

        override fun createFromParcel(parcel: Parcel): Sms {
            return Sms(parcel)
        }

        override fun newArray(size: Int): Array<Sms?> {
            return arrayOfNulls(size)
        }

        fun fromResponse(body: String): Sms? {
            try {
                val jsonObject = JSONObject(body)
                val data = jsonObject.getJSONObject("data")
                return Sms(
                    data.getInt("id"),
                    data.getString("to"),
                    data.getString("msg"),
                    data.getInt("sim")
                )
            } catch (e: Exception) {
                return null;
            }
        }
    }
}
