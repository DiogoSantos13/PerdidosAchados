package pt.uptomar.dam2026.perdidosachados.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import pt.uptomar.dam2026.perdidosachados.R
import pt.uptomar.dam2026.perdidosachados.databinding.ActivityLoginBinding
import pt.uptomar.dam2026.perdidosachados.model.AuthResponse
import pt.uptomar.dam2026.perdidosachados.model.LoginRequest
import pt.uptomar.dam2026.perdidosachados.retrofit.RetrofitInitializer
import pt.uptomar.dam2026.perdidosachados.storage.SessionManager
import pt.uptomar.dam2026.perdidosachados.util.ErrosApi
import pt.uptomar.dam2026.perdidosachados.util.aplicarMargensDoSistema
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Ecrã de início de sessão.
 *
 * Valida os dados introduzidos, autentica o utilizador na API e, em caso de
 * sucesso, guarda o token localmente (SessionManager) e fecha o ecrã.
 */
class LoginActivity : AppCompatActivity() {

    /** Acesso às views do layout activity_login (View Binding). */
    private lateinit var binding: ActivityLoginBinding

    /** Cria o ecrã de login e liga os botões (voltar, entrar e criar conta). */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        aplicarMargensDoSistema(binding.main)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.botaoEntrar.setOnClickListener {
            iniciarSessao()
        }
        binding.botaoRegistar.setOnClickListener {
            // troca este ecrã pelo de registo
            startActivity(Intent(this, RegistoActivity::class.java))
            finish()
        }
    }

    /** Valida os campos. Mostra o erro junto do campo errado e devolve false se algo falhar. */
    private fun validar(email: String, password: String): Boolean {
        var valido = true

        if (email.isEmpty()) {
            binding.campoEmail.error = getString(R.string.erro_campo_obrigatorio)
            valido = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.campoEmail.error = getString(R.string.erro_email_invalido)
            valido = false
        } else {
            binding.campoEmail.error = null
        }

        if (password.isEmpty()) {
            binding.campoPassword.error = getString(R.string.erro_campo_obrigatorio)
            valido = false
        } else {
            binding.campoPassword.error = null
        }

        return valido
    }

    /** Envia as credenciais à API. */
    private fun iniciarSessao() {
        val email = binding.editEmail.text.toString().trim()
        val password = binding.editPassword.text.toString()
        if (!validar(email, password)) return

        mostrarProgresso(true)
        val pedido = RetrofitInitializer().apiService().login(LoginRequest(email, password))
        pedido.enqueue(object : Callback<AuthResponse> {
            /** Resposta da API: guarda a sessão e fecha o ecrã, ou mostra o erro (ex.: credenciais erradas). */
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                mostrarProgresso(false)
                val corpo = response.body()
                val token = corpo?.token
                val utilizador = corpo?.utilizador
                if (response.isSuccessful && token != null && utilizador != null) {
                    SessionManager(this@LoginActivity).guardarSessao(token, utilizador)
                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.bem_vindo, utilizador.nome),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    // ex.: "E-mail ou palavra-passe incorretos."
                    Toast.makeText(
                        this@LoginActivity,
                        ErrosApi.mensagem(this@LoginActivity, response),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            /** Falha de rede: mostra a mensagem de erro. */
            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                mostrarProgresso(false)
                Toast.makeText(
                    this@LoginActivity,
                    ErrosApi.mensagemFalha(this@LoginActivity, t),
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    /** Enquanto o pedido decorre, mostra o indicador e impede cliques repetidos. */
    private fun mostrarProgresso(aDecorrer: Boolean) {
        binding.progresso.visibility = if (aDecorrer) View.VISIBLE else View.GONE
        binding.botaoEntrar.isEnabled = !aDecorrer
    }
}
