package com.example.appcontrolpaucartambo

data class Socio(
    val id: Int,
    val nombre: String,
    val lote: String,
    val estaAlDia: Boolean,
    val montoDeuda: Double = 0.0
)