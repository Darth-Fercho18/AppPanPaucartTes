package com.example.appcontrolpaucartambo

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Asociado(
    @SerializedName("id_asociado") val idAsociado: Int,
    @SerializedName("dni") val dni: String,
    @SerializedName("nombres") val nombres: String,
    @SerializedName("apellido_paterno") val apellidoPaterno: String,
    @SerializedName("apellido_materno") val apellidoMaterno: String,
    @SerializedName("sexo") val sexo: String?,
    @SerializedName("fecha_nacimiento") val fechaNacimiento: String?,
    @SerializedName("anio_ingreso") val anioIngreso: Int?,
    @SerializedName("id_tipo") val idTipo: Int,
    @SerializedName("nombre_tipo") val nombreTipo: String?,
    @SerializedName("celular") val celular: String?,
    @SerializedName("correo") val correo: String?,
    @SerializedName("direccion") val direccion: String?,
    @SerializedName("nombre_apoderado") val nombreApoderado: String?,
    @SerializedName("celular_apoderado") val celularApoderado: String?,
    @SerializedName("fecha_registro") val fechaRegistro: String?
): Serializable {
    // Propiedad calculada para mantener compatibilidad si se requiere el año de nacimiento
    val anioNacimiento: Int?
        get() {
            if (fechaNacimiento.isNullOrBlank()) return null
            return try {
                fechaNacimiento.trim().take(4).toIntOrNull()
            } catch (_: Exception) {
                null
            }
        }
}
