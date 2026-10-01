package com.example.appcontrolpaucartambo

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Asociado(
    @SerializedName("id_asociado") val idAsociado: Int,
    @SerializedName("dni") val dni: String,
    @SerializedName("nombres") val nombres: String,
    @SerializedName("apellido_paterno") val apellidoPaterno: String,
    @SerializedName("apellido_materno") val apellidoMaterno: String,
    @SerializedName("id_tipo") val idTipo: Int,
    @SerializedName("celular") val celular: String?,        // Puede ser null
    @SerializedName("correo") val correo: String?,          // Puede ser null
    @SerializedName("direccion") val direccion: String?,    // Puede ser null
    @SerializedName("fecha_registro") val fechaRegistro: String
): Serializable