package com.stephanpetzl.mqttaudioplayer

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.squareup.otto.Subscribe
import com.stephanpetzl.mqttaudioplayer.event.OnConnectedEvent
import com.stephanpetzl.mqttaudioplayer.event.OnDisconnectedEvent
import com.stephanpetzl.mqttaudioplayer.event.OnPlayerEvent
import com.stephanpetzl.mqttaudioplayer.event.OnStartAudioServiceEvent
import com.stephanpetzl.mqttelectricityreporter.event.OnStatusEvent
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MainBus.register(this)

        startService(AudioService.getStartIntent(this))
        val lastBrokerUri = Prefs.getLastBrokerUri(this)
        if (lastBrokerUri != null) {
            brokerAddressField.setText(lastBrokerUri)
        }

        btnStartService.setOnClickListener {
            MainBus.post(OnStartAudioServiceEvent(brokerAddressField.text.toString(), topicField.text.toString()))
            progressBar.visibility = View.VISIBLE
            btnStartService.visibility = View.GONE
        }

    }

    override fun onPause() {
        super.onPause()
        Prefs.setLastBrokerUri(this, brokerAddressField.text.toString())
    }

    @Subscribe
    fun onConnectedEvent(event: OnConnectedEvent) {
        progressBar.visibility = View.GONE
        btnStartService.visibility = View.VISIBLE
    }

    @Subscribe
    fun onDisconnectedEvent(event: OnDisconnectedEvent) {
        progressBar.visibility = View.GONE
        btnStartService.visibility = View.VISIBLE
    }

    @Subscribe
    fun onStatusEvent(event: OnStatusEvent) {
        tvStatus.text = event.message
    }

    @Subscribe
    fun onPlayerEvent(event: OnPlayerEvent) {
        tvPlayerStatus.text = event.message
    }

}
