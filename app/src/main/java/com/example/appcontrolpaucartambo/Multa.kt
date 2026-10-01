package com.example.appcontrolpaucartambo

import com.google.gson.annotations.SerializedName

data class Multa(
    @SerializedName("id_multa") val idMulta: Int,
    @SerializedName("monto_original") val montoOriginal: String,
    @SerializedName("descuento") val descuento: String,
    @SerializedName("tipo_multa") val tipoMulta: String,
    @SerializedName("monto_total") val montoTotal: String,
    @SerializedName("monto_pagado") val montoPagado: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("fecha_pago") val fechaPago: String?
)

data class RespuestaMulta(
    val success: Boolean,
    val message: String?,
    val multa: Multa?
)