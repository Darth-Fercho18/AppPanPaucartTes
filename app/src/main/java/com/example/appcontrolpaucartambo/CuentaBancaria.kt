package com.example.appcontrolpaucartambo

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CuentaBancaria(
    @SerializedName("id_cuenta") val idCuenta: Int,
    @SerializedName("nombre_cuenta") val nombreCuenta: String? = null,
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("moneda") val moneda: String? = null,
    @SerializedName("saldo_actual") val saldoActual: Double? = null
) : Serializable {
    override fun toString(): String = nombreCuenta ?: nombre ?: "Cuenta $idCuenta"
}
