package com.example.appcontrolpaucartambo

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field
import retrofit2.http.POST
import retrofit2.http.Headers
import retrofit2.http.Body
import retrofit2.http.Query

data class RespuestaBase(
    val success: Boolean,
    val message: String?
)
interface ApiService {

    // La que funciona:
    @GET("obtener_asociados.php")
    fun getAsociados(): Call<List<Asociado>>

    // Unificadas con "api/" para que apunten a la misma ruta exitosa:
    @GET("obtener_tipos_socio.php")
    fun obtenerTiposSocio(): Call<List<TipoSocio>>

    @FormUrlEncoded
    @Headers("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
    @POST("agregar_asociado.php")
    fun agregarAsociado(
        @Field("dni") dni: String,
        @Field("nombres") nombres: String,
        @Field("apellido_paterno") apellidoPaterno: String,
        @Field("apellido_materno") apellidoMaterno: String,
        @Field("id_tipo") idTipo: Int,
        @Field("celular") celular: String?,
        @Field("correo") correo: String?,
        @Field("direccion") direccion: String?
    ): Call<Void>

    @FormUrlEncoded
    @POST("agregar_actividad.php")
    fun agregarActividad(
        @Field("nombre_actividad") nombreActividad: String,
        @Field("fecha_actividad") fechaActividad: String,
        @Field("multa_por_falta") multaPorFalta: Double,
        @Field("multa_por_tardanza") multaPorTardanza: Double
    ): Call<Void>

    @Headers("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
    @FormUrlEncoded
    @POST("actualizar_asociado.php")
    fun actualizarAsociado(
        @Field("id_asociado") idAsociado: Int,
        @Field("dni") dni: String,
        @Field("nombres") nombres: String,
        @Field("apellido_paterno") apellidoPaterno: String,
        @Field("apellido_materno") apellidoMaterno: String,
        @Field("id_tipo") idTipo: Int,
        @Field("celular") celular: String?,
        @Field("correo") correo: String?,
        @Field("direccion") direccion: String?
    ): Call<Void>

    @GET("obtener_actividades.php")
    fun obtenerActividades(): Call<List<Actividad>>

    @POST("guardar_asistencia.php")
    fun guardarAsistencias(@Body asistencias: List<Asistencia>): Call<Void>

    @GET("obtener_asistencias.php")
    fun obtenerAsistenciasPorActividad(@Query("id_actividad") idActividad: Int): Call<List<Asistencia>>

    // Obtener detalle de multa por asociado y actividad
    @GET("obtener_multa.php")
    fun obtenerMulta(
        @Query("id_asociado") idAsociado: Int,
        @Query("id_actividad") idActividad: Int
    ): Call<RespuestaMulta>

    // Agrega esta clase si no tienes una clase genérica de respuesta


    // Dentro de tu interface ApiService:
    @FormUrlEncoded
    @POST("actualizar_descuento_multa.php")
    fun actualizarDescuentoMulta(
        @Field("id_multa") idMulta: Int,
        @Field("descuento") descuento: Double
    ): Call<RespuestaBase>

    @FormUrlEncoded
    @POST("actualizar_actividad.php")
    fun actualizarActividad(
        @Field("id_actividad") idActividad: Int,
        @Field("nombre_actividad") nombreActividad: String,
        @Field("fecha_actividad") fechaActividad: String,
        @Field("multa_por_falta") multaPorFalta: Double,
        @Field("multa_por_tardanza") multaPorTardanza: Double
    ): Call<Void>
}