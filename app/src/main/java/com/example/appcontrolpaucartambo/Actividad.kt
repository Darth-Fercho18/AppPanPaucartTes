package com.example.appcontrolpaucartambo

import com.google.gson.annotations.SerializedName

data class Actividad(
    @SerializedName("id_actividad") val idActividad: Int,
    @SerializedName("nombre_actividad") val nombreActividad: String,
    @SerializedName("fecha_actividad") val fechaActividad: String, // Formato "YYYY-MM-DD"
    @SerializedName("multa_por_falta") val multaPorFalta: Double,
    @SerializedName("multa_por_tardanza") val multaPorTardanza: Double
) {
    // Esto hace que el Spinner muestre automáticamente "REUNION ORDINARIA 2026-09-05"
    override fun toString(): String {
        return "$nombreActividad $fechaActividad"
    }
}