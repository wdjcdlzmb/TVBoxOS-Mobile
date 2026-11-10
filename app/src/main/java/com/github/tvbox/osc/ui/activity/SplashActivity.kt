package com.github.tvbox.osc.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.base.BaseVbActivity
import com.github.tvbox.osc.databinding.ActivitySplashBinding

class SplashActivity : BaseVbActivity<ActivitySplashBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
    }

    override fun init() {
        App.getInstance().isNormalStart = true
        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        finish()
    }
}
