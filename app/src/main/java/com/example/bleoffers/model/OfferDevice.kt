package com.example.bleoffers.model

data class OfferDevice(
    val name: String,
    val address: String,
    val type: Int,
    val alias: String?,
    val urlOffer: String?,
    val codeOffer: String?
)
