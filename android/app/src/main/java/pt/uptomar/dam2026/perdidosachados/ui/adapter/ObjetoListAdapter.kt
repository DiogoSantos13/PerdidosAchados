package pt.uptomar.dam2026.perdidosachados.ui.adapter

import android.content.Context
import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import pt.uptomar.dam2026.perdidosachados.R
import pt.uptomar.dam2026.perdidosachados.databinding.ItemObjetoBinding
import pt.uptomar.dam2026.perdidosachados.model.Objeto
import pt.uptomar.dam2026.perdidosachados.storage.ImageLoader
import pt.uptomar.dam2026.perdidosachados.util.Formatacao
import pt.uptomar.dam2026.perdidosachados.util.distanciaEmMetros

/**
 * Faz o mapeamento entre a lista de objetos e os cartões (item_objeto.xml)
 * mostrados na RecyclerView.
 *
 * @param aoClicar função chamada quando o utilizador toca num objeto
 */
class ObjetoListAdapter(
    private val context: Context,
    private val aoClicar: (Objeto) -> Unit
) : RecyclerView.Adapter<ObjetoListAdapter.ViewHolder>() {

    private var objetos: List<Objeto> = emptyList()

    // posição atual do utilizador; se for conhecida, mostra-se a distância a cada objeto
    private var minhaLocalizacao: Location? = null

    /** Substitui os dados mostrados e redesenha a lista. */
    fun atualizar(novosObjetos: List<Objeto>, localizacao: Location?) {
        objetos = novosObjetos
        minhaLocalizacao = localizacao
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemObjetoBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(objetos[position])
    }

    override fun getItemCount(): Int = objetos.size

    inner class ViewHolder(private val binding: ItemObjetoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /** Preenche o cartão com os dados do [objeto]. */
        fun bindView(objeto: Objeto) {
            // "Perdido" a vermelho, "Achado" a verde
            if (objeto.tipo == Objeto.TIPO_ACHADO) {
                binding.itemTipo.setText(R.string.tipo_achado)
                binding.itemTipo.setTextColor(ContextCompat.getColor(context, R.color.achado))
            } else {
                binding.itemTipo.setText(R.string.tipo_perdido)
                binding.itemTipo.setTextColor(ContextCompat.getColor(context, R.color.perdido))
            }

            binding.itemEstado.visibility =
                if (objeto.estado == Objeto.ESTADO_RESOLVIDO) View.VISIBLE else View.GONE

            binding.itemTitulo.text = objeto.titulo
            binding.itemLocal.text = objeto.local

            // data e, se soubermos onde está o utilizador, a distância ao objeto
            var linhaData = Formatacao.dataParaMostrar(objeto.dataOcorrencia)
            val localizacao = minhaLocalizacao
            if (localizacao != null) {
                val metros = distanciaEmMetros(localizacao, objeto)
                if (metros != null) {
                    linhaData += " · " + context.getString(
                        R.string.a_distancia, Formatacao.distancia(metros)
                    )
                }
            }
            binding.itemData.text = linhaData

            ImageLoader.carregar(context, objeto.fotoUrl, binding.itemImagem, R.drawable.ic_sem_foto)

            binding.root.setOnClickListener { aoClicar(objeto) }
        }
    }
}
