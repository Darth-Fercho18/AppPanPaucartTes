package com.example.appcontrolpaucartambo

import com.google.gson.annotations.SerializedName

data class Asistencia(
    @SerializedName("id_actividad") val idActividad: Int,
    @SerializedName("id_asociado") val idAsociado: Int,
    @SerializedName("estado") val estado: String
    // El estado enviará exactamente 'ASISTIO', 'FALTO', etc.
)