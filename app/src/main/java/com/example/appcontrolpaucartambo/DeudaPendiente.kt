package com.example.appcontrolpaucartambo

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class DeudaPendiente(
    @SerializedName("id_cuota") val idDeuda: Int,
    @SerializedName("id_asociado") val idAsociado: Int,
    @SerializedName("id_tipo_cuota") val idTipoCuota: Int?,
    @SerializedName("tipo_cuota") val tipoCuota: String?,
    @SerializedName("id_actividad") val idActividad: Int?,
    @SerializedName("nombre_actividad") val nombreActividad: String?,
    @SerializedName("concepto") val concepto: String,
    @SerializedName("monto_original") val montoOriginal: Double?,
    @SerializedName("descuento") val descuento: Double?,
    @SerializedName("monto_total") val montoTotal: Double?,
    @SerializedName("monto_pagado") val montoPagado: Double?,
    @SerializedName("saldo_pendiente") var monto: Double, // Saldo pendiente (monto_total - monto_pagado)
    @SerializedName("estado") val estado: String?,
    @SerializedName("mes") val mes: Int?,
    @SerializedName("anio") val anio: Int?,
    @SerializedName("fecha_emision") val fechaEmision: String? = null,
    var estaSeleccionada: Boolean = false,
    var idCuentaSeleccionada: Int = 1 // Por defecto 1: Efectivo
) : Serializable {
    val tipoDeuda: String get() = tipoCuota ?: "Cuota"
}
