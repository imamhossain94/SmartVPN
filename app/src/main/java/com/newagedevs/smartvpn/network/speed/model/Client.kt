package com.newagedevs.smartvpn.network.speed.model

data class Client(
    val ip: String,
    val lat: String,
    val lon: String,
    val isp: String,
    val isprating: String,
    val rating: String,
    val ispdlavg: String,
    val ispulavg: String,
    val loggedin: String,
    val country: String
)
