package com.example.heartratemonitor

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeActivity: AppCompatActivity() {

    private lateinit var measureHeartBeatBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        supportActionBar?.hide()

        measureHeartBeatBtn = findViewById(R.id.heartBeatBtn)
        measureHeartBeatBtn.setOnClickListener {
            Intent(this, HeartBeatActivity::class.java).apply {
                this@HomeActivity.startActivity(this)
            }
        }
    }
}