package com.example.appcontrolpaucartambo

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class AsociadoAdapter(
    private var listaAsociados: List<Asociado>,
    private val onCobrarClick: ((Asociado) -> Unit)? = null // Acción para el botón de cobro
) : RecyclerView.Adapter<AsociadoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreSocio)
        val tvDetalle: TextView = view.findViewById(R.id.tvDetalleSocio)
        val tvEstado: TextView = view.findViewById(R.id.tvEstadoSocio)
        val btnCobrar: MaterialButton = view.findViewById(R.id.btnVerCuenta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Vinculamos la tarjeta de tesorería que creaste antes
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_socio, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val asociado = listaAsociados[position]
        val context = holder.itemView.context

        // Asignar los datos del API a la vista
        holder.tvNombre.text = "${asociado.nombres} ${asociado.apellidoPaterno} ${asociado.apellidoMaterno}"
        holder.tvDetalle.text = "DNI: ${asociado.dni}"

        // Como aún no tenemos el dato de "Deuda" en la API, por ahora lo simularemos como "Al día"
        // (Podrás cambiar esto cuando añadas el control de pagos a la base de datos)
        holder.tvEstado.text = "● Cuenta activa"
        holder.tvEstado.setTextColor(ContextCompat.getColor(context, R.color.primary_green_light))

        // Acción del botón Cobrar
        holder.btnCobrar.setOnClickListener {
            val intent = Intent(holder.itemView.context, CobrosActivity::class.java)
            // Le inyectamos el ID directamente
            intent.putExtra("id_asociado_directo", asociado.idAsociado)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = listaAsociados.size
}