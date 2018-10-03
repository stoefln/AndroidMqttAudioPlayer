package com.stephanpetzl.mqttaudioplayer

import android.os.Handler
import com.squareup.otto.Bus
import android.os.Looper



class MainBus {
    companion object {

        private val handler: Handler by lazy {
            Handler(Looper.getMainLooper())
        }

        private val instance: Bus by lazy {
            Bus()
        }

        fun register(o: Any){
            instance.register(o)
        }
        fun post(event: Any){
            if (Looper.myLooper() == Looper.getMainLooper()) {
                instance.post(event)
            } else {
                handler.post(Runnable { instance.post(event) })
            }
        }
    }
}
