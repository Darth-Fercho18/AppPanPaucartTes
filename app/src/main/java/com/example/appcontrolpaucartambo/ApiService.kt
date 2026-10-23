package com.example.appcontrolpaucartambo

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.HTTP
import retrofit2.http.Headers
import retrofit2.http.Body
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName

data class RespuestaBase(
    @SerializedName("status") val status: String?,
    @SerializedName("success") val success: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String?
) : java.io.Serializable {
    val isSuccess: Boolean
        get() = success == true || status?.uppercase() == "SUCCESS"
}

data class PagoItemPayload(
    @SerializedName("id_cuota") val idCuota: Int,
    @SerializedName("monto_abonado") val montoAbonado: Double
)

data class GuardarPagoPayload(
    @SerializedName("id_cuenta") val idCuenta: Int,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("pagos") val pagos: List<PagoItemPayload>
)

data class EliminarCuotasPayload(
    @SerializedName("ids") val ids: List<Int>
)

data class ActualizarCuotaPayload(
    @SerializedName("id_cuota") val idCuota: Int,
    @SerializedName("descuento") val descuento: Double? = null,
    @SerializedName("monto_original") val montoOriginal: Double? = null,
    @SerializedName("concepto") val concepto: String? = null
)
interface ApiService {

    // Obtener asociados
    @GET("obtener_asociados.php")
    fun getAsociados(): Call<List<Asociado>>

    // Obtener tipos de socio
    @GET("obtener_tipos_socio.php")
    fun obtenerTiposSocio(): Call<List<TipoSocio>>

    // Obtener cuentas bancarias
    @GET("obtener_cuentas.php")
    fun obtenerCuentas(): Call<List<CuentaBancaria>>

    // Guardar / Registrar / Actualizar Asociado (guardar_asociado.php)
    @FormUrlEncoded
    @Headers("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
    @POST("guardar_asociado.php")
    fun guardarAsociado(
        @Field("id_asociado") idAsociado: Int?,
        @Field("dni") dni: String,
        @Field("nombres") nombres: String,
        @Field("apellido_paterno") apellidoPaterno: String,
        @Field("apellido_materno") apellidoMaterno: String,
        @Field("sexo") sexo: String?,
        @Field("fecha_nacimiento") fechaNacimiento: String?,
        @Field("anio_ingreso") anioIngreso: Int?,
        @Field("id_tipo") idTipo: Int,
        @Field("celular") celular: String?,
        @Field("correo") correo: String?,
        @Field("direccion") direccion: String?,
        @Field("nombre_apoderado") nombreApoderado: String?,
        @Field("celular_apoderado") celularApoderado: String?
    ): Call<Void>

    // Métodos de compatibilidad para asociado
    fun agregarAsociado(
        dni: String, nombres: String, apellidoPaterno: String, apellidoMaterno: String,
        sexo: String?, fechaNacimiento: String?, anioIngreso: Int?, idTipo: Int,
        celular: String?, correo: String?, direccion: String?,
        nombreApoderado: String?, celularApoderado: String?
    ) = guardarAsociado(null, dni, nombres, apellidoPaterno, apellidoMaterno, sexo, fechaNacimiento, anioIngreso, idTipo, celular, correo, direccion, nombreApoderado, celularApoderado)

    fun actualizarAsociado(
        idAsociado: Int, dni: String, nombres: String, apellidoPaterno: String, apellidoMaterno: String,
        sexo: String?, fechaNacimiento: String?, anioIngreso: Int?, idTipo: Int,
        celular: String?, correo: String?, direccion: String?,
        nombreApoderado: String?, celularApoderado: String?
    ) = guardarAsociado(idAsociado, dni, nombres, apellidoPaterno, apellidoMaterno, sexo, fechaNacimiento, anioIngreso, idTipo, celular, correo, direccion, nombreApoderado, celularApoderado)

    // Guardar / Registrar / Actualizar Actividad (guardar_actividad.php)
    @FormUrlEncoded
    @POST("guardar_actividad.php")
    fun guardarActividad(
        @Field("id_actividad") idActividad: Int?,
        @Field("nombre_actividad") nombreActividad: String,
        @Field("fecha_actividad") fechaActividad: String,
        @Field("multa_por_falta") multaPorFalta: Double,
        @Field("multa_por_tardanza") multaPorTardanza: Double
    ): Call<Void>

    fun agregarActividad(nombreActividad: String, fechaActividad: String, multaPorFalta: Double, multaPorTardanza: Double) =
        guardarActividad(null, nombreActividad, fechaActividad, multaPorFalta, multaPorTardanza)

    fun actualizarActividad(idActividad: Int, nombreActividad: String, fechaActividad: String, multaPorFalta: Double, multaPorTardanza: Double) =
        guardarActividad(idActividad, nombreActividad, fechaActividad, multaPorFalta, multaPorTardanza)

    @GET("obtener_actividades.php")
    fun obtenerActividades(): Call<List<Actividad>>

    @FormUrlEncoded
    @POST("guardar_asistencia.php")
    fun registrarAsistencia(
        @Field("id_actividad") idActividad: Int,
        @Field("id_asociado") idAsociado: Int,
        @Field("estado") estado: String,
        @Field("monto_multa") montoMulta: Double? = null,
        @Field("descuento") descuento: Double? = null
    ): Call<RespuestaBase>

    @GET("obtener_asistencias.php")
    fun obtenerAsistenciasPorActividad(@Query("id_actividad") idActividad: Int): Call<List<Asistencia>>

    // Obtener detalle de multa por asociado y actividad
    @GET("obtener_multa.php")
    fun obtenerMulta(
        @Query("id_asociado") idAsociado: Int,
        @Query("id_actividad") idActividad: Int
    ): Call<RespuestaMulta>

    @FormUrlEncoded
    @POST("guardar_cuota.php")
    fun actualizarDescuentoMulta(
        @Field("id_cuota") idCuota: Int,
        @Field("descuento") descuento: Double,
        @Field("id_asociado") idAsociado: Int? = null
    ): Call<RespuestaBase>

    // Obtener las deudas / cuotas de un socio específico
    @GET("obtener_cuotas.php")
    fun obtenerDeudasSocio(
        @Query("id_asociado") idAsociado: Int,
        @Query("estado") estado: String? = null
    ): Call<List<DeudaPendiente>>

    // Obtener cuotas / multas de un socio con filtro de tipo
    @GET("obtener_cuotas.php")
    fun obtenerCuotasAsociado(
        @Query("id_asociado") idAsociado: Int,
        @Query("id_tipo_cuota") idTipoCuota: Int? = null,
        @Query("estado") estado: String? = null
    ): Call<List<DeudaPendiente>>

    // Registrar el pago de una o más deudas
    // Registrar el pago de una o más deudas (Endpoint general guardar_pago.php)
    @POST("guardar_pago.php")
    fun guardarPagoJson(
        @Body payload: GuardarPagoPayload
    ): Call<RespuestaBase>

    // Guardar cuota extraordinaria
    @FormUrlEncoded
    @POST("guardar_extraordinaria.php")
    fun guardarExtraordinaria(
        @Field("id_asociado") idAsociado: Int,
        @Field("concepto") concepto: String,
        @Field("monto_original") montoOriginal: Double,
        @Field("id_tipo_cuota") idTipoCuota: Int = 2
    ): Call<RespuestaBase>

    @POST("guardar_extraordinaria.php")
    fun guardarExtraordinariaJson(
        @Body payload: CuotaExtraordinariaPayload
    ): Call<RespuestaBase>

    @HTTP(method = "DELETE", path = "guardar_extraordinaria.php", hasBody = true)
    fun eliminarCuotasJson(
        @Body payload: EliminarCuotasPayload
    ): Call<RespuestaBase>

    @PUT("guardar_extraordinaria.php")
    fun actualizarCuotaJson(
        @Body payload: ActualizarCuotaPayload
    ): Call<RespuestaBase>

    // Mostrar saldos financieros y por entidad bancaria
    @GET("mostrar_saldos.php")
    fun mostrarSaldos(): Call<ResumenFinancieroResponse>

    // Registrar ingreso o egreso
    @FormUrlEncoded
    @POST("guardar_movimiento.php")
    fun registrarMovimiento(
        @Field("id_cuenta") idCuenta: Int,
        @Field("tipo_movimiento") tipoMovimiento: String,
        @Field("categoria") categoria: String?,
        @Field("concepto") concepto: String?,
        @Field("monto") monto: Double,
        @Field("num_comprobante") numComprobante: String?,
        @Field("id_asociado") idAsociado: Int?,
        @Field("fecha_movimiento") fechaMovimiento: String?
    ): Call<RespuestaMovimiento>

    // Eliminar movimiento de caja
    @FormUrlEncoded
    @POST("guardar_movimiento.php")
    fun eliminarMovimiento(
        @Field("id_movimiento") idMovimiento: Int,
        @Field("accion") accion: String = "eliminar",
        @Field("eliminar") eliminar: Int = 1
    ): Call<RespuestaMovimiento>

    // Obtener movimientos de caja
    @GET("obtener_movimientos.php")
    fun obtenerMovimientos(
        @Query("id_cuenta") idCuenta: Int? = null
    ): Call<List<Movimiento>>
}
