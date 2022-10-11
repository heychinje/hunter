package com.jshunter.hunter

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.jshunter.hunter.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var mainLayoutBinding: ActivityMainBinding
    private val vpnServiceIntent by lazy { Intent(this, HunterVpnService::class.java) }

    private val vpnRightLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) startService(vpnServiceIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainLayoutBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        mainLayoutBinding.apply {
            connectRemoteServer.setOnClickListener {
                val permissionResultIntent = VpnService.prepare(this@MainActivity)
                if (permissionResultIntent == null) startService(vpnServiceIntent)
                else vpnRightLauncher.launch(permissionResultIntent)
            }
            disconnectRemoteServer.setOnClickListener {
                stopService(vpnServiceIntent)
            }
        }
    }
}