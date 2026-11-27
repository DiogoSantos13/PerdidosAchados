package pt.uptomar.dam2026.perdidosachados.ui.activity

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import pt.uptomar.dam2026.perdidosachados.R
import pt.uptomar.dam2026.perdidosachados.databinding.ActivityRegistoBinding
import pt.uptomar.dam2026.perdidosachados.model.AuthResponse
import pt.uptomar.dam2026.perdidosachados.model.RegistoRequest
import pt.uptomar.dam2026.perdidosachados.retrofit.RetrofitInitializer
import pt.uptomar.dam2026.perdidosachados.storage.SessionManager
import pt.uptomar.dam2026.perdidosachados.util.ErrosApi
import pt.uptomar.dam2026.perdidosachados.util.aplicarMargensDoSistema
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Ecrã de criação de conta.
 *
 * Valida os dados, cria a conta na API e, em caso de sucesso, fica logo com a
 * sessão iniciada (a API devolve o token no registo).
 */
class RegistoActivity : AppCompatActivity() {

    /** Tamanhos mínimos validados no formulário de registo (iguais aos da API). */
    companion object {
        private const val NOME_MINIMO = 3
        private const val PASSWORD_MINIMO = 6
    }

    /** Acesso às views do layout activity_registo (View Binding). */
    private lateinit var binding: ActivityRegistoBinding

    /** Cria o ecrã de registo e liga os botões às ações. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegistoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        aplicarMargensDoSistema(binding.main)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.botaoRegistar.setOnClickListener {
            criarConta()
        }
    }

    /** Valida todos os campos, assinalando os erros junto de cada um. */
    private fun validar(nome: String, email: String, password: String, confirmar: String): Boolean {
        var valido = true

        if (nome.length < NOME_MINIMO) {
            binding.campoNome.error = getString(R.string.erro_nome_curto, NOME_MINIMO)
            valido = false
        } else {
            binding.campoNome.error = null
        }

        if (email.isEmpty()) {
            binding.campoEmail.error = getString(R.string.erro_campo_obrigatorio)
            valido = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.campoEmail.error = getString(R.string.erro_email_invalido)
            valido = false
        } else {
            binding.campoEmail.error = null
        }

        if (password.length < PASSWORD_MINIMO) {
            binding.campoPassword.error = getString(R.string.erro_password_curta, PASSWORD_MINIMO)
            valido = false
        } else {
            binding.campoPassword.error = null
        }

        if (confirmar != password) {
            binding.campoConfirmar.error = getString(R.string.erro_password_diferente)
            valido = false
        } else {
            binding.campoConfirmar.error = null
        }

        return valido
    }

    /** Envia os dados da nova conta à API. */
    private fun criarConta() {
        val nome = binding.editNome.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()
        val password = binding.editPassword.text.toString()
        val confirmar = binding.editConfirmar.text.toString()
        if (!validar(nome, email, password, confirmar)) return

        mostrarProgresso(true)
        val pedido = RetrofitInitializer().apiService()
            .registar(RegistoRequest(nome, email, password))
        pedido.enqueue(object : Callback<AuthResponse> {
            /** Resposta da API: conta criada e sessão iniciada, ou mensagem de erro (ex.: email já registado). */
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                mostrarProgresso(false)
                val corpo = response.body()
                val token = corpo?.token
                val utilizador = corpo?.utilizador
                if (response.isSuccessful && token != null && utilizador != null) {
                    SessionManager(this@RegistoActivity).guardarSessao(token, utilizador)
                    Toast.makeText(
                        this@RegistoActivity, R.string.conta_criada, Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    // ex.: "Já existe uma conta com este e-mail."
                    Toast.makeText(
                        this@RegistoActivity,
                        ErrosApi.mensagem(this@RegistoActivity, response),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            /** Falha de rede: mostra a mensagem de erro. */
            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                mostrarProgresso(false)
                Toast.makeText(
                    this@RegistoActivity,
                    ErrosApi.mensagemFalha(this@RegistoActivity, t),
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    /** Enquanto o pedido decorre, mostra o indicador e impede cliques repetidos. */
    private fun mostrarProgresso(aDecorrer: Boolean) {
        binding.progresso.visibility = if (aDecorrer) View.VISIBLE else View.GONE
        binding.botaoRegistar.isEnabled = !aDecorrer
    }
}
