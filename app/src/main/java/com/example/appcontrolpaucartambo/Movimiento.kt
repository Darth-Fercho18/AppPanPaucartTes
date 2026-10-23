
package com.example.appcontrolpaucartambo

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Movimiento(
    @SerializedName("id_movimiento")
    val idMovimiento: Int,

    @SerializedName("id_cuenta")
    val idCuenta: Int,

    @SerializedName("nombre_cuenta")
    val nombreCuenta: String?,

    @SerializedName("tipo_movimiento")
    val tipoMovimiento: String,

    @SerializedName("categoria")
    val categoria: String?,

    @SerializedName("concepto")
    val concepto: String?,

    @SerializedName("monto")
    val monto: Double,

    @SerializedName("num_comprobante")
    val numComprobante: String?,

    @SerializedName("id_asociado")
    val idAsociado: Int?,

    @SerializedName("asociado_nombre")
    val asociadoNombre: String?,

    @SerializedName("nombre")
    val nombre: String?,

    @SerializedName("fecha_movimiento")
    val fechaMovimiento: String?,

    // NUEVOS CAMPOS PARA VERIFICACIÓN QR

    @SerializedName("token_publico")
    val tokenPublico: String? = null,

    @SerializedName("estado_comprobante")
    val estadoComprobante: String? = null

) : Serializable
