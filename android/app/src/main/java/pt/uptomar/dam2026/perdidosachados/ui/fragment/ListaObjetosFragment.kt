package pt.uptomar.dam2026.perdidosachados.ui.fragment

import android.Manifest
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import pt.uptomar.dam2026.perdidosachados.R
import pt.uptomar.dam2026.perdidosachados.databinding.FragmentListaObjetosBinding
import pt.uptomar.dam2026.perdidosachados.model.Objeto
import pt.uptomar.dam2026.perdidosachados.retrofit.RetrofitInitializer
import pt.uptomar.dam2026.perdidosachados.storage.ObjetosCache
import pt.uptomar.dam2026.perdidosachados.storage.SessionManager
import pt.uptomar.dam2026.perdidosachados.ui.activity.DetalheObjetoActivity
import pt.uptomar.dam2026.perdidosachados.ui.adapter.ObjetoListAdapter
import pt.uptomar.dam2026.perdidosachados.util.ErrosApi
import pt.uptomar.dam2026.perdidosachados.util.LocalizacaoHelper
import pt.uptomar.dam2026.perdidosachados.util.distanciaEmMetros
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Lista de objetos perdidos e achados. É usado em dois separadores:
 *
 *  - "Objetos" (apenasMeus = false): todos os objetos, com pesquisa por texto,
 *    filtro por tipo e ordenação por proximidade (GPS). Funciona sem login e,
 *    se não houver Internet, mostra a última lista guardada no dispositivo.
 *  - "Os meus" (apenasMeus = true): objetos do utilizador com sessão iniciada.
 */
class ListaObjetosFragment : Fragment() {

    /** Chave do argumento e atraso da pesquisa; o método novaInstancia cria o fragmento. */
    companion object {
        private const val ARG_APENAS_MEUS = "apenasMeus"

        /** Tempo de espera, depois de o utilizador parar de escrever, até pesquisar. */
        private const val ATRASO_PESQUISA = 500L

        /** Cria o fragmento, indicando se mostra só os objetos do utilizador. */
        fun novaInstancia(apenasMeus: Boolean): ListaObjetosFragment {
            val fragmento = ListaObjetosFragment()
            val argumentos = Bundle()
            argumentos.putBoolean(ARG_APENAS_MEUS, apenasMeus)
            fragmento.arguments = argumentos
            return fragmento
        }
    }

    // o binding só existe entre onCreateView e onDestroyView
    private var _binding: FragmentListaObjetosBinding? = null

    /** Sessão do utilizador, de onde se obtém o token enviado à API. */
    private lateinit var sessao: SessionManager
    /** Cópia local da última lista (Internal Storage), usada quando não há rede. */
    private lateinit var cache: ObjetosCache
    /** Adapter do RecyclerView que desenha cada objeto da lista. */
    private lateinit var adapter: ObjetoListAdapter
    /** Obtém a posição GPS para o modo "Perto de mim". */
    private lateinit var localizacaoHelper: LocalizacaoHelper

    /** true no separador "Os meus" (só os objetos do utilizador); false em "Objetos". */
    private var apenasMeus = false

    /** Objetos recebidos da API (ou lidos do armazenamento local). */
    private var objetos: List<Objeto> = emptyList()

    /** Posição atual do utilizador (null enquanto não for pedida/obtida). */
    private var minhaLocalizacao: Location? = null

    /** Pedido à API em curso; guarda-se para o poder cancelar. */
    private var pedidoEmCurso: Call<List<Objeto>>? = null

    // pesquisa com atraso: evita um pedido à API por cada letra escrita
    private val handler = Handler(Looper.getMainLooper())
    /** Tarefa que faz a pesquisa quando o utilizador pára de escrever. */
    private val pesquisaAtrasada = Runnable { carregarObjetos() }

    // pedido da permissão de localização (resposta do utilizador ao diálogo do sistema)
    private val pedirPermissaoLocalizacao = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        if (resultados.values.any { concedida -> concedida }) {
            obterLocalizacao()
        } else {
            _binding?.chipPerto?.isChecked = false
            mostrarMensagem(getString(R.string.gps_permissao_negada))
        }
    }

    /** Cria a view do separador a partir do layout fragment_lista_objetos.xml (View Binding). */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentListaObjetosBinding.inflate(inflater, container, false)
        _binding = binding
        return binding.root
    }

    /** Configura a lista, os filtros, a pesquisa e o botão "Tentar novamente". */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = _binding ?: return

        apenasMeus = arguments?.getBoolean(ARG_APENAS_MEUS) ?: false
        sessao = SessionManager(requireContext())
        cache = ObjetosCache(requireContext())
        localizacaoHelper = LocalizacaoHelper(requireContext()) { localizacao ->
            aoObterLocalizacao(localizacao)
        }

        // lista: ao tocar num objeto abre-se o ecrã de detalhe
        adapter = ObjetoListAdapter(requireContext()) { objeto ->
            abrirDetalhe(objeto)
        }
        binding.lista.layoutManager = LinearLayoutManager(requireContext())
        binding.lista.adapter = adapter

        // a pesquisa e os filtros só existem no separador "Objetos"
        binding.zonaFiltros.visibility = if (apenasMeus) View.GONE else View.VISIBLE

        binding.chipsTipo.setOnCheckedStateChangeListener { _, _ ->
            carregarObjetos()
        }

        binding.chipPerto.setOnCheckedChangeListener { _, selecionado ->
            if (selecionado) {
                ativarOrdenacaoPorProximidade()
            } else {
                mostrarObjetos()
            }
        }

        binding.editPesquisa.addTextChangedListener(object : TextWatcher {
            /** Não usado (obrigatório pela interface TextWatcher). */
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            /** Não usado (obrigatório pela interface TextWatcher). */
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            /** Depois de cada alteração ao texto, reagenda a pesquisa (só corre quando se pára de escrever). */
            override fun afterTextChanged(s: Editable?) {
                handler.removeCallbacks(pesquisaAtrasada)
                handler.postDelayed(pesquisaAtrasada, ATRASO_PESQUISA)
            }
        })

        binding.botaoTentar.setOnClickListener {
            carregarObjetos()
        }
    }

    /** Recarrega a lista sempre que o separador fica visível. */
    override fun onResume() {
        super.onResume()
        // recarrega sempre que o separador fica visível: pode ter sido publicado,
        // alterado ou eliminado um objeto, ou a sessão pode ter mudado
        carregarObjetos()
    }

    // ------------------------------------------------------------------
    // Chamada à API
    // ------------------------------------------------------------------

    /** Tipo escolhido nos filtros ("perdido", "achado" ou null para todos). */
    private fun tipoSelecionado(): String? {
        val binding = _binding ?: return null
        return when (binding.chipsTipo.checkedChipId) {
            R.id.chip_perdidos -> Objeto.TIPO_PERDIDO
            R.id.chip_achados -> Objeto.TIPO_ACHADO
            else -> null
        }
    }

    /** Texto escrito na caixa de pesquisa (null se estiver vazia). */
    private fun textoPesquisa(): String? {
        val texto = _binding?.editPesquisa?.text?.toString()?.trim()
        return if (texto.isNullOrEmpty()) null else texto
    }

    /** Pede à API a lista de objetos, de acordo com os filtros escolhidos. */
    private fun carregarObjetos() {
        val binding = _binding ?: return

        // "Os meus" exige sessão iniciada
        if (apenasMeus && !sessao.temSessao()) {
            objetos = emptyList()
            adapter.atualizar(objetos, null)
            mostrarZonaVazia(getString(R.string.meus_sem_sessao), false)
            return
        }

        binding.progresso.visibility = View.VISIBLE
        binding.zonaVazia.visibility = View.GONE

        // se ainda houver um pedido anterior a decorrer, deixa de interessar
        pedidoEmCurso?.cancel()

        val api = RetrofitInitializer().apiService()
        val token = sessao.cabecalhoAutorizacao()
        val tipo = tipoSelecionado()
        val pesquisa = textoPesquisa()
        val pedido = if (apenasMeus) {
            api.listarMeusObjetos(token)
        } else {
            api.listarObjetos(tipo, pesquisa, token)
        }
        pedidoEmCurso = pedido

        pedido.enqueue(object : Callback<List<Objeto>> {
            /** Resposta da API: mostra a lista e guarda-a no dispositivo, ou mostra o erro. */
            override fun onResponse(call: Call<List<Objeto>>, response: Response<List<Objeto>>) {
                val b = _binding ?: return
                b.progresso.visibility = View.GONE

                if (response.isSuccessful) {
                    objetos = response.body() ?: emptyList()
                    b.textoAviso.visibility = View.GONE
                    // guarda localmente a lista completa, para consulta sem Internet
                    if (!apenasMeus && tipo == null && pesquisa == null) {
                        cache.guardar(objetos)
                    }
                    mostrarObjetos()
                } else if (response.code() == 401 && apenasMeus) {
                    // o token deixou de ser válido: é preciso voltar a iniciar sessão
                    sessao.terminarSessao()
                    objetos = emptyList()
                    adapter.atualizar(objetos, null)
                    mostrarZonaVazia(getString(R.string.erro_sessao), false)
                } else {
                    objetos = emptyList()
                    adapter.atualizar(objetos, null)
                    mostrarZonaVazia(ErrosApi.mensagem(requireContext(), response), true)
                }
            }

            /** Sem ligação: mostra a última lista guardada no dispositivo (ObjetosCache), se existir. */
            override fun onFailure(call: Call<List<Objeto>>, t: Throwable) {
                if (call.isCanceled) return   // pedido cancelado por nós: ignora-se
                val b = _binding ?: return
                b.progresso.visibility = View.GONE

                // sem ligação: tenta mostrar os objetos guardados no dispositivo
                if (!apenasMeus) {
                    val guardados = filtrarLocalmente(cache.ler(), tipo, pesquisa)
                    if (guardados.isNotEmpty()) {
                        objetos = guardados
                        b.textoAviso.setText(R.string.aviso_offline)
                        b.textoAviso.visibility = View.VISIBLE
                        mostrarObjetos()
                        return
                    }
                }
                objetos = emptyList()
                adapter.atualizar(objetos, null)
                mostrarZonaVazia(ErrosApi.mensagemFalha(requireContext(), t), true)
            }
        })
    }

    /** Aplica os filtros (tipo e texto) a uma lista lida do armazenamento local. */
    private fun filtrarLocalmente(lista: List<Objeto>, tipo: String?, pesquisa: String?): List<Objeto> {
        return lista.filter { objeto ->
            val tipoCerto = tipo == null || objeto.tipo == tipo
            val textoCerto = pesquisa == null ||
                (objeto.titulo ?: "").contains(pesquisa, ignoreCase = true) ||
                (objeto.descricao ?: "").contains(pesquisa, ignoreCase = true) ||
                (objeto.local ?: "").contains(pesquisa, ignoreCase = true)
            tipoCerto && textoCerto
        }
    }

    // ------------------------------------------------------------------
    // Apresentação
    // ------------------------------------------------------------------

    /** Mostra os objetos na lista, ordenados por proximidade se isso estiver ativo. */
    private fun mostrarObjetos() {
        val binding = _binding ?: return
        val localizacao = minhaLocalizacao

        var lista = objetos
        if (binding.chipPerto.isChecked && localizacao != null) {
            // os objetos sem coordenadas ficam no fim da lista
            lista = objetos.sortedBy { objeto ->
                distanciaEmMetros(localizacao, objeto) ?: Float.MAX_VALUE
            }
        }
        adapter.atualizar(lista, localizacao)

        if (lista.isEmpty()) {
            val mensagem = if (apenasMeus) R.string.meus_vazia else R.string.lista_vazia
            mostrarZonaVazia(getString(mensagem), false)
        } else {
            binding.zonaVazia.visibility = View.GONE
        }
    }

    /** Mostra uma mensagem no centro do ecrã, com ou sem o botão "Tentar novamente". */
    private fun mostrarZonaVazia(mensagem: String, comBotaoTentar: Boolean) {
        val binding = _binding ?: return
        binding.progresso.visibility = View.GONE
        binding.textoVazio.text = mensagem
        binding.botaoTentar.visibility = if (comBotaoTentar) View.VISIBLE else View.GONE
        binding.zonaVazia.visibility = View.VISIBLE
    }

    /** Mostra uma mensagem curta (Toast), se o fragmento ainda estiver ligado à atividade. */
    private fun mostrarMensagem(mensagem: String) {
        val contexto = context ?: return
        Toast.makeText(contexto, mensagem, Toast.LENGTH_LONG).show()
    }

    /** Abre o ecrã de detalhe; o objeto segue no Intent em formato JSON. */
    private fun abrirDetalhe(objeto: Objeto) {
        val intent = Intent(requireContext(), DetalheObjetoActivity::class.java)
        intent.putExtra(DetalheObjetoActivity.EXTRA_OBJETO, Gson().toJson(objeto))
        startActivity(intent)
    }

    // ------------------------------------------------------------------
    // GPS: ordenar por proximidade
    // ------------------------------------------------------------------

    /** Chamado quando o utilizador ativa "Perto de mim". */
    private fun ativarOrdenacaoPorProximidade() {
        if (localizacaoHelper.temPermissao()) {
            obterLocalizacao()
        } else {
            pedirPermissaoLocalizacao.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    /** Pede a posição atual ao GPS. A resposta chega a [aoObterLocalizacao]. */
    private fun obterLocalizacao() {
        if (!localizacaoHelper.localizacaoLigada()) {
            _binding?.chipPerto?.isChecked = false
            mostrarMensagem(getString(R.string.gps_desligado))
            return
        }
        mostrarMensagem(getString(R.string.gps_a_obter))
        localizacaoHelper.pedirLocalizacao()
    }

    /** Recebe a posição obtida (ou null, se não foi possível obtê-la). */
    private fun aoObterLocalizacao(localizacao: Location?) {
        val binding = _binding ?: return
        if (localizacao == null) {
            binding.chipPerto.isChecked = false
            mostrarMensagem(getString(R.string.gps_falhou))
        } else {
            minhaLocalizacao = localizacao
            mostrarObjetos()
        }
    }

    /** Cancela pedidos e pesquisas pendentes e liberta o binding. */
    override fun onDestroyView() {
        super.onDestroyView()
        // cancela tudo o que estiver pendente, para não mexer num ecrã que já não existe
        pedidoEmCurso?.cancel()
        handler.removeCallbacks(pesquisaAtrasada)
        localizacaoHelper.parar()
        _binding = null
    }
}
