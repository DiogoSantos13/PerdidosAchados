package pt.uptomar.dam2026.perdidosachados.ui.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import pt.uptomar.dam2026.perdidosachados.R
import pt.uptomar.dam2026.perdidosachados.databinding.ActivityDetalheObjetoBinding
import pt.uptomar.dam2026.perdidosachados.model.APIResult
import pt.uptomar.dam2026.perdidosachados.model.Objeto
import pt.uptomar.dam2026.perdidosachados.model.ObjetoRequest
import pt.uptomar.dam2026.perdidosachados.retrofit.RetrofitInitializer
import pt.uptomar.dam2026.perdidosachados.storage.ImageLoader
import pt.uptomar.dam2026.perdidosachados.storage.SessionManager
import pt.uptomar.dam2026.perdidosachados.util.ErrosApi
import pt.uptomar.dam2026.perdidosachados.util.Formatacao
import pt.uptomar.dam2026.perdidosachados.util.LocalizacaoHelper
import pt.uptomar.dam2026.perdidosachados.util.aplicarMargensDoSistema
import pt.uptomar.dam2026.perdidosachados.util.distanciaEmMetros
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Ecrã de detalhe de um objeto.
 *
 * Qualquer pessoa pode ver o objeto e abrir o local no mapa.
 * Controlo de acesso:
 *  - o contacto do autor só é mostrado a utilizadores com sessão iniciada;
 *  - editar, marcar como resolvido e eliminar só estão disponíveis para o
 *    dono do objeto e para o administrador (a API volta a verificar isto).
 */
class DetalheObjetoActivity : AppCompatActivity() {

    /** Chave do extra do Intent usado para abrir este ecrã. */
    companion object {
        /** Extra do Intent com o objeto a mostrar, em formato JSON. */
        const val EXTRA_OBJETO = "objeto"
    }

    /** Acesso às views do layout activity_detalhe_objeto (View Binding). */
    private lateinit var binding: ActivityDetalheObjetoBinding
    /** Sessão do utilizador (token e dados), guardada em SharedPreferences. */
    private lateinit var sessao: SessionManager
    /** Objeto que está a ser mostrado (recebido no Intent em formato JSON). */
    private lateinit var objeto: Objeto

    // abre o formulário de edição; no regresso, se houve alterações, atualiza o ecrã
    private val editarObjeto = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK) {
            atualizarDaApi()
        }
    }

    /** Cria o ecrã: lê o objeto recebido no Intent, liga os botões às ações e pede à API a versão mais recente. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDetalheObjetoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        aplicarMargensDoSistema(binding.main)

        sessao = SessionManager(this)

        // o objeto chega em JSON, vindo da lista
        val recebido = try {
            Gson().fromJson(intent.getStringExtra(EXTRA_OBJETO), Objeto::class.java)
        } catch (e: Exception) {
            null
        }
        if (recebido == null) {
            Toast.makeText(this, R.string.erro_nao_encontrado, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        objeto = recebido

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.botaoMapa.setOnClickListener { abrirMapa() }
        binding.botaoContactar.setOnClickListener { contactarAutor() }
        binding.botaoEstado.setOnClickListener { alternarEstado() }
        binding.botaoEditar.setOnClickListener {
            val intent = Intent(this, FormularioObjetoActivity::class.java)
            intent.putExtra(FormularioObjetoActivity.EXTRA_OBJETO, Gson().toJson(objeto))
            editarObjeto.launch(intent)
        }
        binding.botaoEliminar.setOnClickListener { confirmarEliminar() }

        mostrarObjeto()
        // vai buscar a versão mais recente (e o contacto, se houver sessão iniciada)
        atualizarDaApi()
    }

    /** Ao voltar a este ecrã, volta a desenhá-lo (a sessão pode ter mudado, ex.: login feito a partir daqui). */
    override fun onResume() {
        super.onResume()
        // a sessão pode ter mudado entretanto (ex.: login feito a partir daqui)
        if (::objeto.isInitialized) {
            mostrarObjeto()
        }
    }

    /** Preenche o ecrã com os dados do objeto. */
    private fun mostrarObjeto() {
        if (objeto.tipo == Objeto.TIPO_ACHADO) {
            binding.textoTipo.setText(R.string.tipo_achado)
            binding.textoTipo.setTextColor(ContextCompat.getColor(this, R.color.achado))
        } else {
            binding.textoTipo.setText(R.string.tipo_perdido)
            binding.textoTipo.setTextColor(ContextCompat.getColor(this, R.color.perdido))
        }
        val resolvido = objeto.estado == Objeto.ESTADO_RESOLVIDO
        binding.textoEstado.visibility = if (resolvido) View.VISIBLE else View.GONE

        binding.textoTitulo.text = objeto.titulo
        binding.textoDescricao.text = objeto.descricao
        binding.textoCategoria.text = getString(R.string.detalhe_categoria, objeto.categoria)
        binding.textoLocal.text = getString(R.string.detalhe_local, objeto.local)
        binding.textoData.text = getString(
            R.string.detalhe_data, Formatacao.dataParaMostrar(objeto.dataOcorrencia)
        )
        binding.textoAutor.text = getString(R.string.detalhe_autor, objeto.utilizadorNome)

        ImageLoader.carregar(this, objeto.fotoUrl, binding.imagem, R.drawable.ic_sem_foto)

        // mapa e distância: só se o objeto tiver coordenadas GPS
        binding.botaoMapa.visibility = if (objeto.temCoordenadas()) View.VISIBLE else View.GONE
        mostrarDistancia()

        // contacto: só com sessão iniciada, e não faz sentido contactar-se a si próprio
        val utilizador = sessao.utilizador()
        val eDono = utilizador != null && utilizador.id == objeto.utilizadorId
        if (utilizador == null) {
            binding.botaoContactar.visibility = View.GONE
            binding.textoContactoAviso.visibility = View.VISIBLE
        } else {
            binding.textoContactoAviso.visibility = View.GONE
            binding.botaoContactar.visibility =
                if (!eDono && !objeto.contacto.isNullOrBlank()) View.VISIBLE else View.GONE
        }

        // gestão: só para o dono do objeto e para o administrador
        val podeGerir = eDono || sessao.eAdmin()
        binding.zonaGestao.visibility = if (podeGerir) View.VISIBLE else View.GONE
        binding.botaoEstado.setText(
            if (resolvido) R.string.marcar_ativo else R.string.marcar_resolvido
        )
    }

    /**
     * Mostra a que distância o utilizador está do local do objeto.
     * Usa a última posição conhecida do GPS e só se a permissão já tiver sido
     * dada (neste ecrã não se interrompe o utilizador com pedidos de permissão).
     */
    private fun mostrarDistancia() {
        binding.textoDistancia.visibility = View.GONE
        val localizacao = LocalizacaoHelper(this) { }.ultimaLocalizacaoConhecida() ?: return
        val metros = distanciaEmMetros(localizacao, objeto) ?: return
        binding.textoDistancia.text = getString(
            R.string.detalhe_distancia, Formatacao.distancia(metros)
        )
        binding.textoDistancia.visibility = View.VISIBLE
    }

    // ------------------------------------------------------------------
    // Ações disponíveis para todos
    // ------------------------------------------------------------------

    /** Abre a app de mapas do dispositivo no local do objeto. */
    private fun abrirMapa() {
        val latitude = objeto.latitude ?: return
        val longitude = objeto.longitude ?: return
        val etiqueta = Uri.encode(objeto.titulo ?: "")
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($etiqueta)")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.erro_sem_app_mapas, Toast.LENGTH_LONG).show()
        }
    }

    /** Abre a app de e-mail com uma mensagem para o autor da publicação. */
    private fun contactarAutor() {
        val contacto = objeto.contacto ?: return
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$contacto"))
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_assunto, objeto.titulo))
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // sem app de e-mail instalada: mostra o endereço para ser copiado
            Toast.makeText(
                this, getString(R.string.erro_sem_app_email, contacto), Toast.LENGTH_LONG
            ).show()
        }
    }

    // ------------------------------------------------------------------
    // Chamadas à API
    // ------------------------------------------------------------------

    /** Vai buscar à API a versão atual do objeto. */
    private fun atualizarDaApi() {
        val pedido = RetrofitInitializer().apiService()
            .obterObjeto(objeto.id, sessao.cabecalhoAutorizacao())
        pedido.enqueue(object : Callback<Objeto> {
            /** Resposta da API: mostra o objeto atualizado ou, se já não existir (404), avisa e fecha o ecrã. */
            override fun onResponse(call: Call<Objeto>, response: Response<Objeto>) {
                if (isFinishing || isDestroyed) return
                val atualizado = response.body()
                if (response.isSuccessful && atualizado != null) {
                    objeto = atualizado
                    mostrarObjeto()
                } else if (response.code() == 404) {
                    // foi eliminado entretanto (por exemplo, pelo administrador)
                    Toast.makeText(
                        this@DetalheObjetoActivity, R.string.erro_nao_encontrado, Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }

            /** Sem ligação: mantêm-se no ecrã os dados que vieram da lista. */
            override fun onFailure(call: Call<Objeto>, t: Throwable) {
                // sem ligação: mantêm-se no ecrã os dados que vieram da lista
                t.printStackTrace()
            }
        })
    }

    /** Marca o objeto como resolvido (entregue ao dono) ou volta a pô-lo ativo. */
    private fun alternarEstado() {
        val novoEstado = if (objeto.estado == Objeto.ESTADO_RESOLVIDO) {
            Objeto.ESTADO_ATIVO
        } else {
            Objeto.ESTADO_RESOLVIDO
        }
        mostrarProgresso(true)
        // na edição só se envia o campo que muda
        val pedido = RetrofitInitializer().apiService().alterarObjeto(
            objeto.id, sessao.cabecalhoAutorizacao(), ObjetoRequest(estado = novoEstado)
        )
        pedido.enqueue(object : Callback<Objeto> {
            /** Resposta da API: mostra o novo estado, ou a mensagem de erro (ex.: sem permissão). */
            override fun onResponse(call: Call<Objeto>, response: Response<Objeto>) {
                if (isFinishing || isDestroyed) return
                mostrarProgresso(false)
                val atualizado = response.body()
                if (response.isSuccessful && atualizado != null) {
                    objeto = atualizado
                    mostrarObjeto()
                    Toast.makeText(
                        this@DetalheObjetoActivity, R.string.estado_alterado, Toast.LENGTH_SHORT
                    ).show()
                } else {
                    mostrarErro(ErrosApi.mensagem(this@DetalheObjetoActivity, response))
                }
            }

            /** Falha de rede: esconde o indicador de progresso e mostra a mensagem de erro. */
            override fun onFailure(call: Call<Objeto>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                mostrarProgresso(false)
                mostrarErro(ErrosApi.mensagemFalha(this@DetalheObjetoActivity, t))
            }
        })
    }

    /** Pede confirmação antes de eliminar: a operação não pode ser desfeita. */
    private fun confirmarEliminar() {
        AlertDialog.Builder(this)
            .setTitle(R.string.eliminar_titulo)
            .setMessage(getString(R.string.eliminar_confirmar, objeto.titulo))
            .setPositiveButton(R.string.eliminar) { _, _ -> eliminar() }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    /** Elimina o objeto na API e fecha o ecrã. */
    private fun eliminar() {
        mostrarProgresso(true)
        val pedido = RetrofitInitializer().apiService()
            .eliminarObjeto(objeto.id, sessao.cabecalhoAutorizacao())
        pedido.enqueue(object : Callback<APIResult> {
            /** Resposta da API: se foi eliminado, avisa e fecha o ecrã; caso contrário mostra o erro. */
            override fun onResponse(call: Call<APIResult>, response: Response<APIResult>) {
                if (isFinishing || isDestroyed) return
                mostrarProgresso(false)
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@DetalheObjetoActivity, R.string.objeto_eliminado, Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    mostrarErro(ErrosApi.mensagem(this@DetalheObjetoActivity, response))
                }
            }

            /** Falha de rede: esconde o indicador de progresso e mostra a mensagem de erro. */
            override fun onFailure(call: Call<APIResult>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                mostrarProgresso(false)
                mostrarErro(ErrosApi.mensagemFalha(this@DetalheObjetoActivity, t))
            }
        })
    }

    /** Enquanto um pedido decorre, mostra o indicador e desativa os botões de gestão. */
    private fun mostrarProgresso(aDecorrer: Boolean) {
        binding.progresso.visibility = if (aDecorrer) View.VISIBLE else View.GONE
        binding.botaoEstado.isEnabled = !aDecorrer
        binding.botaoEditar.isEnabled = !aDecorrer
        binding.botaoEliminar.isEnabled = !aDecorrer
    }

    /** Mostra uma mensagem de erro curta (Toast) ao utilizador. */
    private fun mostrarErro(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
    }
}
