package com.example.appcontrolpaucartambo

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class SaldoBanco(
    @SerializedName("banco") val banco: String,
    @SerializedName("total") val total: Double
) : Serializable

data class ResumenFinancieroResponse(
    @SerializedName("status") val status: String,
    @SerializedName("total_general_soles") val totalGeneralSoles: Double,
    @SerializedName("saldos_por_banco_soles") val saldosPorBancoSoles: List<SaldoBanco>,
    @SerializedName("total_general_dolares") val totalGeneralDolares: Double,
    @SerializedName("saldos_por_banco_dolares") val saldosPorBancoDolares: List<SaldoBanco>
) : Serializable
