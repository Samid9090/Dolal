package com.parentkidsapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.parentkidsapp.kids.KidsModeActivity
import com.parentkidsapp.parent.ParentModeActivity
import com.parentkidsapp.utils.PreferenceManager

class MainActivity : AppCompatActivity() {

    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preferenceManager = PreferenceManager(this)

        // Check if app was launched via deep link
        handleDeepLink(intent)

        setupViews()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleDeepLink(it) }
    }

    private fun setupViews() {
        val parentModeCard = findViewById<CardView>(R.id.parentModeCard)
        val kidsModeCard = findViewById<CardView>(R.id.kidsModeCard)

        parentModeCard.setOnClickListener {
            startParentMode()
        }

        kidsModeCard.setOnClickListener {
            startKidsMode()
        }
    }

    private fun handleDeepLink(intent: Intent) {
        val data: Uri? = intent.data
        if (data != null && data.scheme == "parentkidsapp") {
            when (data.host) {
                "kids" -> {
                    val pairingToken = data.getQueryParameter("token")
                    if (pairingToken != null) {
                        // Store pairing token and start kids mode
                        preferenceManager.setPairingToken(pairingToken)
                        startKidsMode(true)
                    } else {
                        Toast.makeText(this, "Invalid pairing link", Toast.LENGTH_SHORT).show()
                    }
                }
                else -> {
                    Toast.makeText(this, "Unknown link format", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startParentMode() {
        val intent = Intent(this, ParentModeActivity::class.java)
        startActivity(intent)
    }

    private fun startKidsMode(fromDeepLink: Boolean = false) {
        val intent = Intent(this, KidsModeActivity::class.java)
        intent.putExtra("from_deep_link", fromDeepLink)
        startActivity(intent)
        
        // If started from deep link, finish main activity
        if (fromDeepLink) {
            finish()
        }
    }
}

