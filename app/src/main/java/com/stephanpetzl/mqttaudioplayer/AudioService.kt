package com.stephanpetzl.mqttaudioplayer

import android.arch.lifecycle.LifecycleService
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import com.squareup.otto.Subscribe
import com.stephanpetzl.mqttaudioplayer.event.OnConnectedEvent
import com.stephanpetzl.mqttaudioplayer.event.OnDisconnectedEvent
import com.stephanpetzl.mqttaudioplayer.event.OnPlayerEvent
import com.stephanpetzl.mqttaudioplayer.event.OnStartAudioServiceEvent
import com.stephanpetzl.mqttelectricityreporter.event.OnStatusEvent
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import timber.log.Timber
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.IMqttActionListener




class AudioService : LifecycleService() {

    private var mqttAndroidClient: MqttAndroidClient? = null
    private val clientId = "MqttAudioPlayer"
    private var mqttTopic: String? = null

    private val mp: MediaPlayer by lazy {
        val player = MediaPlayer()
        player.setOnCompletionListener {
            isPlaying = false
            MainBus.post(OnPlayerEvent("Complete"))
            if (mqttAndroidClient != null && mqttAndroidClient!!.isConnected) {
                val message = MqttMessage()
                message.payload = "complete".toByteArray()
                mqttAndroidClient?.publish(mqttTopic, message)
            }
        }
        player
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            MainBus.register(this)
        } catch (e: IllegalArgumentException) {
            // ignore error about double registering
        }
        init()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        mp.setOnCompletionListener(null)
        mp.reset()
        mp.release()
    }
    private fun init() {


    }

    @Subscribe
    fun onStartAudioServiceEvent(event: OnStartAudioServiceEvent) {
        mqttTopic = event.mqttTopic
        connectToMqttBroker(event.brokerAddressUri)
    }

    private var isPlaying: Boolean = false

    private fun connectToMqttBroker(serverUri: String) {
        if (mqttAndroidClient != null && mqttAndroidClient!!.isConnected) {
            mqttAndroidClient!!.disconnect()

        }
        mqttAndroidClient = MqttAndroidClient(applicationContext, serverUri, clientId)
        mqttAndroidClient!!.setCallback(object : MqttCallbackExtended {

            override fun connectComplete(reconnect: Boolean, serverURI: String) {

                if (reconnect) {
                    sendStatus("Reconnected to : $serverURI")
                    // Because Clean Session is true, we need to re-subscribe
                } else {
                    sendStatus("Connected to: $serverURI")
                }
                MainBus.post(OnConnectedEvent())
            }

            override fun connectionLost(cause: Throwable) {
                sendStatus("The Connection was lost.")
                MainBus.post(OnDisconnectedEvent())
            }

            @Throws(Exception::class)
            override fun messageArrived(topic: String, message: MqttMessage) {
                val payload = String(message.payload)
                sendStatus("Incoming message: " + payload)
                if (topic.startsWith(mqttTopic!!)) {
                    if (payload.startsWith("play")) {
                        val params = payload.split(" ")
                        if (params.size < 2) {
                            sendStatus("no URL parameter provided. ")
                            return
                        }
                        var url = params[1]
                        if(isPlaying) {
                            Timber.d("isPlaying. stopp.")
                            mp.stop()
                        }
                        mp.reset()
                        Timber.d("MediaPlayer reset.")
                        mp.setDataSource(url)
                        mp.prepare()
                        mp.start()
                        isPlaying = true
                        MainBus.post(OnPlayerEvent("Playing"))
                    } else if (payload == "stop") {
                        mp.stop()
                        isPlaying = false
                        MainBus.post(OnPlayerEvent("Stopped"))
                    } else if (payload == "pause") {
                        if(isPlaying) {
                            mp.pause()
                        }
                        MainBus.post(OnPlayerEvent("Paused"))
                    }
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {

            }
        })

        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.setAutomaticReconnect(true)
        mqttConnectOptions.isCleanSession = false


        try {
            sendStatus("Connecting to " + serverUri);
            mqttAndroidClient!!.connect(mqttConnectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    val disconnectedBufferOptions = DisconnectedBufferOptions()
                    disconnectedBufferOptions.setBufferEnabled(true)
                    disconnectedBufferOptions.setBufferSize(100)
                    disconnectedBufferOptions.setPersistBuffer(false)
                    disconnectedBufferOptions.setDeleteOldestMessages(false)
                    mqttAndroidClient!!.setBufferOpts(disconnectedBufferOptions)

                    subscribeToTopic()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    sendStatus("Failed to connect to: $serverUri")
                    MainBus.post(OnDisconnectedEvent())
                }
            })


        } catch (ex: MqttException) {
            ex.printStackTrace()
        }
    }

    private fun subscribeToTopic() {
        mqttAndroidClient!!.subscribe(mqttTopic, 2, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                sendStatus("Successfully subscribed to topic $mqttTopic.")
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                sendStatus("Failed to subscribed to topic $mqttTopic.")
            }
        })
    }

    private fun sendStatus(message: String) {
        Timber.d("Status: $message")
        MainBus.post(OnStatusEvent(message))
    }

    companion object {
        fun getStartIntent(context: Context): Intent {
            return Intent(context, AudioService::class.java)
        }
    }
}