package pt.uptomar.dam2026.perdidosachados.ui.activity

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import pt.uptomar.dam2026.perdidosachados.R
import pt.uptomar.dam2026.perdidosachados.databinding.ActivityFormularioObjetoBinding
import pt.uptomar.dam2026.perdidosachados.model.Objeto
import pt.uptomar.dam2026.perdidosachados.model.ObjetoRequest
import pt.uptomar.dam2026.perdidosachados.retrofit.RetrofitInitializer
import pt.uptomar.dam2026.perdidosachados.storage.ImageLoader
import pt.uptomar.dam2026.perdidosachados.storage.SessionManager
import pt.uptomar.dam2026.perdidosachados.util.ErrosApi
import pt.uptomar.dam2026.perdidosachados.util.Formatacao
import pt.uptomar.dam2026.perdidosachados.util.FotoHelper
import pt.uptomar.dam2026.perdidosachados.util.LocalizacaoHelper
import pt.uptomar.dam2026.perdidosachados.util.aplicarMargensDoSistema
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Formulário para publicar um objeto novo ou editar um objeto existente.
 *
 * É aqui que se usa o hardware do dispositivo:
 *  - CÂMARA: para fotografar o objeto (em alternativa, escolhe-se uma foto da galeria);
 *  - GPS: para registar o sítio exato onde o objeto foi perdido ou achado.
 *
 * Se o Intent trouxer um objeto (EXTRA_OBJETO), o formulário abre em modo de
 * edição, já preenchido. Todos os dados são validados antes de seguirem para
 * a API, tanto na inserção como na edição.
 */
class FormularioObjetoActivity : AppCompatActivity() {

    /** Chave do extra do Intent, e limites de tamanho dos campos do formulário. */
    companion object {
        /** Extra do Intent com o objeto a editar, em JSON (ausente = objeto novo). */
        const val EXTRA_OBJETO = "objeto"

        // limites de tamanho dos campos (iguais aos validados pela API)
        private const val TITULO_MINIMO = 3
        private const val DESCRICAO_MINIMO = 10
        private const val LOCAL_MINIMO = 3

        // chaves para guardar o estado quando o ecrã é recriado (ex.: rotação)
        private const val ESTADO_URI_CAMARA = "uriCamara"
        private const val ESTADO_URI_FOTO = "uriFoto"
        private const val ESTADO_LATITUDE = "latitude"
        private const val ESTADO_LONGITUDE = "longitude"
        private const val ESTADO_DATA = "data"
        private const val ESTADO_ALTERADO = "alterado"
    }

    /** Acesso às views do layout activity_formulario_objeto (View Binding). */
    private lateinit var binding: ActivityFormularioObjetoBinding
    /** Sessão do utilizador, de onde se obtém o token enviado à API. */
    private lateinit var sessao: SessionManager
    /** Obtém a posição GPS do telemóvel quando o utilizador pede "usar a minha localização". */
    private lateinit var localizacaoHelper: LocalizacaoHelper

    /** Objeto em edição (null quando se está a publicar um objeto novo). */
    private var objetoAEditar: Objeto? = null

    /** Data em que o objeto foi perdido/achado (por omissão, hoje). */
    private var dataOcorrencia: Calendar = Calendar.getInstance()

    /** Coordenadas GPS do local (null se não foram registadas). */
    private var latitude: Double? = null
    /** Longitude do local (null se não foi registada). */
    private var longitude: Double? = null

    /** Ficheiro onde a app da câmara vai gravar a fotografia. */
    private var uriCamara: Uri? = null

    /** Fotografia escolhida nesta sessão (tirada com a câmara ou vinda da galeria). */
    private var uriFoto: Uri? = null

    /** A mesma fotografia, já reduzida e convertida em Base64, pronta a enviar. */
    private var fotoBase64: String? = null

    /** Indica se o utilizador alterou algo (para avisar antes de sair sem guardar). */
    private var alterado = false

    // thread secundária para preparar a fotografia sem bloquear a interface
    private val executor = Executors.newSingleThreadExecutor()

    // ---------- respostas a pedidos feitos ao sistema ----------

    // permissão da câmara
    private val pedirPermissaoCamara = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (concedida) {
            abrirCamara()
        } else {
            mostrarMensagem(getString(R.string.camara_permissao_negada))
        }
    }

    // fotografia tirada com a câmara (true = o utilizador confirmou a foto)
    private val tirarFoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { sucesso ->
        val uri = uriCamara
        if (sucesso && uri != null) {
            usarFoto(uri)
        }
    }

    // fotografia escolhida na galeria
    private val escolherFoto = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            usarFoto(uri)
        }
    }

    // permissão de localização
    private val pedirPermissaoLocalizacao = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        if (resultados.values.any { concedida -> concedida }) {
            obterLocalizacao()
        } else {
            mostrarMensagem(getString(R.string.gps_permissao_negada))
        }
    }

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFormularioObjetoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        aplicarMargensDoSistema(binding.main)

        sessao = SessionManager(this)

        // controlo de acesso: sem sessão iniciada não se pode publicar nem editar
        if (!sessao.temSessao()) {
            mostrarMensagem(getString(R.string.sessao_necessaria_publicar))
            finish()
            return
        }

        localizacaoHelper = LocalizacaoHelper(this) { localizacao ->
            aoObterLocalizacao(localizacao)
        }

        // lista de categorias (definida em strings.xml)
        val categorias = resources.getStringArray(R.array.categorias)
        val adaptadorCategorias =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        adaptadorCategorias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategoria.adapter = adaptadorCategorias

        // modo de edição: o objeto chega em JSON
        val json = intent.getStringExtra(EXTRA_OBJETO)
        if (json != null) {
            objetoAEditar = try {
                Gson().fromJson(json, Objeto::class.java)
            } catch (e: Exception) {
                null
            }
        }
        val objeto = objetoAEditar
        if (objeto != null) {
            binding.toolbar.setTitle(R.string.formulario_titulo_editar)
            // só se preenche a partir do objeto na primeira vez; depois de uma
            // rotação, o Android repõe sozinho o que o utilizador já tinha escrito
            if (savedInstanceState == null) {
                preencherCampos(objeto, categorias)
            } else {
                ImageLoader.carregar(this, objeto.fotoUrl, binding.imagemFoto, R.drawable.ic_sem_foto)
            }
        }

        if (savedInstanceState != null) {
            reporEstado(savedInstanceState)
        }

        mostrarData()
        mostrarCoordenadas()
        configurarBotoes()
        configurarAvisoAoSair()
    }

    /** Preenche o formulário com os dados do objeto que está a ser editado. */
    private fun preencherCampos(objeto: Objeto, categorias: Array<String>) {
        if (objeto.tipo == Objeto.TIPO_ACHADO) {
            binding.radioAchado.isChecked = true
        } else {
            binding.radioPerdido.isChecked = true
        }
        binding.editTitulo.setText(objeto.titulo)
        binding.editDescricao.setText(objeto.descricao)
        binding.editLocal.setText(objeto.local)

        val posicao = categorias.indexOf(objeto.categoria)
        if (posicao >= 0) {
            binding.spinnerCategoria.setSelection(posicao)
        }

        dataOcorrencia = Formatacao.dataDaApi(objeto.dataOcorrencia)
        latitude = objeto.latitude
        longitude = objeto.longitude

        // mostra a fotografia que o objeto já tem
        ImageLoader.carregar(this, objeto.fotoUrl, binding.imagemFoto, R.drawable.ic_sem_foto)
    }

    /** Liga cada botão do formulário (voltar, data, câmara, galeria, localização, guardar) à sua ação. */
    private fun configurarBotoes() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.botaoData.setOnClickListener { escolherData() }
        binding.botaoCamara.setOnClickListener { pedirCamara() }
        binding.botaoGaleria.setOnClickListener {
            try {
                escolherFoto.launch("image/*")
            } catch (e: Exception) {
                mostrarMensagem(getString(R.string.erro_sem_galeria))
            }
        }
        binding.botaoGps.setOnClickListener { pedirLocalizacao() }
        binding.botaoRemoverGps.setOnClickListener {
            latitude = null
            longitude = null
            alterado = true
            mostrarCoordenadas()
        }
        binding.botaoGuardar.setOnClickListener { guardar() }
    }

    /** Guarda o que não é reposto automaticamente quando o ecrã é recriado. */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ESTADO_URI_CAMARA, uriCamara?.toString())
        outState.putString(ESTADO_URI_FOTO, uriFoto?.toString())
        outState.putLong(ESTADO_DATA, dataOcorrencia.timeInMillis)
        outState.putBoolean(ESTADO_ALTERADO, alterado)
        val lat = latitude
        val lon = longitude
        if (lat != null && lon != null) {
            outState.putDouble(ESTADO_LATITUDE, lat)
            outState.putDouble(ESTADO_LONGITUDE, lon)
        }
    }

    /** Repõe o estado guardado em [onSaveInstanceState]. */
    private fun reporEstado(estado: Bundle) {
        uriCamara = estado.getString(ESTADO_URI_CAMARA)?.let { Uri.parse(it) }
        dataOcorrencia.timeInMillis = estado.getLong(ESTADO_DATA, dataOcorrencia.timeInMillis)
        alterado = estado.getBoolean(ESTADO_ALTERADO, false)
        if (estado.containsKey(ESTADO_LATITUDE)) {
            latitude = estado.getDouble(ESTADO_LATITUDE)
            longitude = estado.getDouble(ESTADO_LONGITUDE)
        } else {
            latitude = null
            longitude = null
        }
        val foto = estado.getString(ESTADO_URI_FOTO)
        if (foto != null) {
            usarFoto(Uri.parse(foto))
        }
    }

    override fun onStop() {
        super.onStop()
        // deixa de usar o GPS quando o ecrã não está visível (poupa bateria)
        if (::localizacaoHelper.isInitialized) {
            localizacaoHelper.parar()
            binding.botaoGps.isEnabled = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }

    // ------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------

    /** Mostra no botão a data escolhida, no formato DD/MM/AAAA. */
    private fun mostrarData() {
        binding.botaoData.text = Formatacao.dataParaMostrar(Formatacao.dataParaApi(dataOcorrencia))
    }

    /** Abre o calendário. Não deixa escolher datas no futuro. */
    private fun escolherData() {
        val dialogo = DatePickerDialog(
            this,
            { _, ano, mes, dia ->
                dataOcorrencia.set(ano, mes, dia)
                alterado = true
                mostrarData()
            },
            dataOcorrencia.get(Calendar.YEAR),
            dataOcorrencia.get(Calendar.MONTH),
            dataOcorrencia.get(Calendar.DAY_OF_MONTH)
        )
        dialogo.datePicker.maxDate = System.currentTimeMillis()
        dialogo.show()
    }

    // ------------------------------------------------------------------
    // Câmara
    // ------------------------------------------------------------------

    /** Confirma a permissão da câmara (pedindo-a se for preciso) e abre a câmara. */
    private fun pedirCamara() {
        val concedida = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (concedida) {
            abrirCamara()
        } else {
            pedirPermissaoCamara.launch(Manifest.permission.CAMERA)
        }
    }

    /** Abre a app da câmara, que grava a fotografia num ficheiro da nossa cache. */
    private fun abrirCamara() {
        try {
            val uri = FotoHelper.criarUriParaFoto(this)
            uriCamara = uri
            tirarFoto.launch(uri)
        } catch (e: Exception) {
            // ex.: o dispositivo não tem nenhuma app de câmara
            e.printStackTrace()
            mostrarMensagem(getString(R.string.erro_sem_camara))
        }
    }

    /**
     * Passa a usar a fotografia indicada: numa thread secundária, reduz a imagem
     * e converte-a em Base64; depois mostra-a no formulário.
     */
    private fun usarFoto(uri: Uri) {
        binding.botaoGuardar.isEnabled = false   // só se guarda com a foto já preparada
        executor.execute {
            val bitmap: Bitmap? = FotoHelper.carregarBitmapReduzido(this, uri)
            val base64 = if (bitmap != null) FotoHelper.paraBase64(bitmap) else null
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                binding.botaoGuardar.isEnabled = true
                if (bitmap != null && base64 != null) {
                    uriFoto = uri
                    fotoBase64 = base64
                    alterado = true
                    ImageLoader.cancelar(this, binding.imagemFoto)   // cancela um carregamento pendente do Glide
                    binding.imagemFoto.setImageBitmap(bitmap)
                } else {
                    mostrarMensagem(getString(R.string.erro_ler_foto))
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // GPS
    // ------------------------------------------------------------------

    /** Confirma a permissão de localização (pedindo-a se for preciso) e lê o GPS. */
    private fun pedirLocalizacao() {
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
            mostrarMensagem(getString(R.string.gps_desligado))
            return
        }
        binding.botaoGps.isEnabled = false
        binding.textoGps.setText(R.string.gps_a_obter)
        localizacaoHelper.pedirLocalizacao()
    }

    /** Recebe a posição obtida (ou null, se não foi possível obtê-la). */
    private fun aoObterLocalizacao(localizacao: Location?) {
        if (isFinishing || isDestroyed) return
        binding.botaoGps.isEnabled = true
        if (localizacao == null) {
            mostrarMensagem(getString(R.string.gps_falhou))
        } else {
            latitude = localizacao.latitude
            longitude = localizacao.longitude
            alterado = true
        }
        mostrarCoordenadas()
    }

    /** Mostra as coordenadas registadas, ou a indicação de que não há posição. */
    private fun mostrarCoordenadas() {
        val lat = latitude
        val lon = longitude
        if (lat != null && lon != null) {
            binding.textoGps.text = getString(
                R.string.gps_posicao,
                String.format(Locale.US, "%.5f", lat),
                String.format(Locale.US, "%.5f", lon)
            )
            binding.botaoRemoverGps.visibility = View.VISIBLE
        } else {
            binding.textoGps.setText(R.string.gps_sem_posicao)
            binding.botaoRemoverGps.visibility = View.GONE
        }
    }

    // ------------------------------------------------------------------
    // Validação e envio para a API
    // ------------------------------------------------------------------

    /**
     * Valida os dados introduzidos. Os erros são mostrados junto de cada campo.
     * Devolve true se estiver tudo correto.
     */
    private fun validar(titulo: String, descricao: String, local: String): Boolean {
        var valido = true

        if (titulo.length < TITULO_MINIMO) {
            binding.campoTitulo.error = getString(R.string.erro_texto_curto, TITULO_MINIMO)
            valido = false
        } else {
            binding.campoTitulo.error = null
        }

        if (descricao.length < DESCRICAO_MINIMO) {
            binding.campoDescricao.error = getString(R.string.erro_texto_curto, DESCRICAO_MINIMO)
            valido = false
        } else {
            binding.campoDescricao.error = null
        }

        if (local.length < LOCAL_MINIMO) {
            binding.campoLocal.error = getString(R.string.erro_texto_curto, LOCAL_MINIMO)
            valido = false
        } else {
            binding.campoLocal.error = null
        }

        // a data não pode ser no futuro (o calendário já o impede; fica a dupla verificação)
        if (dataOcorrencia.timeInMillis > System.currentTimeMillis()) {
            mostrarMensagem(getString(R.string.erro_data_futura))
            valido = false
        }

        if (!valido) {
            // leva o utilizador até ao início do formulário, onde estão os erros
            binding.scroll.smoothScrollTo(0, 0)
        }
        return valido
    }

    /** Valida e envia o objeto para a API (POST se for novo, PUT se for edição). */
    private fun guardar() {
        val titulo = binding.editTitulo.text.toString().trim()
        val descricao = binding.editDescricao.text.toString().trim()
        val local = binding.editLocal.text.toString().trim()
        if (!validar(titulo, descricao, local)) return

        val tipo = if (binding.radioAchado.isChecked) Objeto.TIPO_ACHADO else Objeto.TIPO_PERDIDO
        val semCoordenadas = latitude == null || longitude == null

        val dados = ObjetoRequest(
            tipo = tipo,
            titulo = titulo,
            descricao = descricao,
            categoria = binding.spinnerCategoria.selectedItem.toString(),
            local = local,
            latitude = if (semCoordenadas) null else latitude,
            longitude = if (semCoordenadas) null else longitude,
            dataOcorrencia = Formatacao.dataParaApi(dataOcorrencia),
            fotoBase64 = fotoBase64,   // null = mantém a fotografia que já existir
            // numa edição, permite apagar as coordenadas que o objeto tinha
            removerLocalizacao = if (semCoordenadas) true else null
        )

        val api = RetrofitInitializer().apiService()
        val token = sessao.cabecalhoAutorizacao()
        val objeto = objetoAEditar
        val pedido = if (objeto == null) {
            api.criarObjeto(token, dados)
        } else {
            api.alterarObjeto(objeto.id, token, dados)
        }

        mostrarProgresso(true)
        pedido.enqueue(object : Callback<Objeto> {
            override fun onResponse(call: Call<Objeto>, response: Response<Objeto>) {
                if (isFinishing || isDestroyed) return
                mostrarProgresso(false)
                if (response.isSuccessful) {
                    val mensagem = if (objeto == null) {
                        R.string.objeto_publicado
                    } else {
                        R.string.objeto_alterado
                    }
                    Toast.makeText(this@FormularioObjetoActivity, mensagem, Toast.LENGTH_SHORT)
                        .show()
                    // avisa o ecrã anterior de que houve alterações
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    // ex.: erro de validação devolvido pela API, ou sessão expirada
                    mostrarMensagem(ErrosApi.mensagem(this@FormularioObjetoActivity, response))
                }
            }

            override fun onFailure(call: Call<Objeto>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                mostrarProgresso(false)
                mostrarMensagem(ErrosApi.mensagemFalha(this@FormularioObjetoActivity, t))
            }
        })
    }

    // ------------------------------------------------------------------
    // Interface
    // ------------------------------------------------------------------

    /**
     * Se o utilizador tentar sair com alterações por guardar, pede confirmação,
     * para não se perder por engano o que já foi preenchido.
     */
    private fun configurarAvisoAoSair() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!haAlteracoesPorGuardar()) {
                    finish()
                    return
                }
                AlertDialog.Builder(this@FormularioObjetoActivity)
                    .setTitle(R.string.sair_titulo)
                    .setMessage(R.string.sair_confirmar)
                    .setPositiveButton(R.string.sair_descartar) { _, _ -> finish() }
                    .setNegativeButton(R.string.sair_continuar, null)
                    .show()
            }
        })
    }

    /** Indica se o formulário tem dados que se perdem ao sair sem guardar. */
    private fun haAlteracoesPorGuardar(): Boolean {
        if (alterado) return true   // fotografia, data ou posição GPS

        val titulo = binding.editTitulo.text.toString().trim()
        val descricao = binding.editDescricao.text.toString().trim()
        val local = binding.editLocal.text.toString().trim()
        val objeto = objetoAEditar

        return if (objeto == null) {
            // objeto novo: basta já ter sido escrito algum texto
            titulo.isNotEmpty() || descricao.isNotEmpty() || local.isNotEmpty()
        } else {
            // edição: compara com os valores que o objeto tinha
            val tipo = if (binding.radioAchado.isChecked) Objeto.TIPO_ACHADO else Objeto.TIPO_PERDIDO
            titulo != objeto.titulo || descricao != objeto.descricao ||
                local != objeto.local || tipo != objeto.tipo ||
                binding.spinnerCategoria.selectedItem.toString() != objeto.categoria
        }
    }

    /** Enquanto o pedido decorre, mostra o indicador e impede cliques repetidos. */
    private fun mostrarProgresso(aDecorrer: Boolean) {
        binding.progresso.visibility = if (aDecorrer) View.VISIBLE else View.GONE
        binding.botaoGuardar.isEnabled = !aDecorrer
    }

    /** Mostra uma mensagem curta (Toast) ao utilizador. */
    private fun mostrarMensagem(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
    }
}
