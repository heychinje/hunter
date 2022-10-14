package com.jshunter.hunter

data class VpnConfig(
    val address: String,
    val addressPrefixLength: Int,
    val dns: String = "8.8.8.8",
    val route: String = "0.0.0.0",
    val mtu: Int = Short.MAX_VALUE.toInt(),
    val session: String = "HunterVpn"
)
