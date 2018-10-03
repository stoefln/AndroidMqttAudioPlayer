package com.stephanpetzl.mqttaudioplayer

import android.content.Context
import android.content.SharedPreferences

class Prefs {

    companion object {
        var sharedPrefs: SharedPreferences? = null

        fun getSharedPrefs(context: Context): SharedPreferences? {
            if(sharedPrefs == null) {
                sharedPrefs = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
            }
            return sharedPrefs
        }

        fun getLastBrokerUri(context: Context): String? {
            return getSharedPrefs(context)!!.getString("lastBrokerUrl", null)
        }

        fun setLastBrokerUri(context: Context, value: String) {
            with (getSharedPrefs(context)!!.edit()) {
                putString("lastBrokerUrl", value)
                commit()
            }
        }

    }
}
