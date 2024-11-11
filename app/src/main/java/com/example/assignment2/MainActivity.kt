package com.example.assignment2

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client

class MainActivity : AppCompatActivity() {
    private var mqttClient : Mqtt5BlockingClient? = null
    private var clientID : String = ""
    private var topic : String = "assignment/location"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mqttClient = Mqtt5Client.builder()
            .identifier(clientID)
            .serverHost("http://broker-816036749.sundaebytestt.com")
            .serverPort(1883)
            .buildBlocking()
    }
}