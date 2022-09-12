package com.example.kyrsach

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    private val getButton = findViewById<Button>(R.id.getTimeButton)
    private val setButton = findViewById<Button>(R.id.setTimeButton)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getButton.setOnClickListener {

        }
        setButton.setOnClickListener {

        }
    }

}