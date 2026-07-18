package dev.mysd.android

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import dev.mysd.game.FoundationStatus

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 20f
            setPadding(48, 48, 48, 48)
            text = getString(
                R.string.foundation_status,
                FoundationStatus.phase,
                FoundationStatus.engineTickAfterStart(),
            )
        })
    }
}
