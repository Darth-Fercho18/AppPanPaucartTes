package com.example.appcontrolpaucartambo

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Convierte "2026-09-25" -> "25/09/2026" (Para mostrar en pantalla)
fun formatearFechaParaMostrar(fechaBD: String?): String {
    if (fechaBD.isNullOrEmpty()) return ""
    return try {
        val formatoEntrada = if (fechaBD.contains(" ")) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        }
        val fecha = formatoEntrada.parse(fechaBD)
        val formatoSalida = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        fecha?.let { formatoSalida.format(it) } ?: fechaBD
    } catch (e: Exception) {
        fechaBD
    }
}

// Convierte "25/09/2026" -> "2026-09-25" (Para guardar en la BD)
fun formatearFechaParaBD(fechaUI: String): String {
    return try {
        val formatoEntrada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fecha = formatoEntrada.parse(fechaUI)
        val formatoSalida = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        fecha?.let { formatoSalida.format(it) } ?: fechaUI
    } catch (e: Exception) {
        fechaUI
    }
}