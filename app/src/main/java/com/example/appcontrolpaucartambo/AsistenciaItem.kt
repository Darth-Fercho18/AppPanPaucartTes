package com.example.appcontrolpaucartambo

data class AsistenciaItem(
    val asociado: Asociado, // Tu modelo original intacto (debe contener el idAsociado)
    var estadoSeleccionado: String = "Seleccionar..."
)