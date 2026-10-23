package com.example.appcontrolpaucartambo

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class RespuestaMovimiento(
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String?,
    @SerializedName("id_movimiento") val idMovimiento: Int?
) : Serializable
