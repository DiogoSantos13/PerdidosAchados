package pt.uptomar.dam2026.perdidosachados.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import pt.uptomar.dam2026.perdidosachados.R
import pt.uptomar.dam2026.perdidosachados.model.APIResult
import pt.uptomar.dam2026.perdidosachados.retrofit.RetrofitInitializer
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import pt.uptomar.dam2026.perdidosachados.databinding.FragmentContaBinding
import pt.uptomar.dam2026.perdidosachados.storage.SessionManager
import pt.uptomar.dam2026.perdidosachados.ui.activity.LoginActivity
import pt.uptomar.dam2026.perdidosachados.ui.activity.RegistoActivity

/**
 * Separador "Conta".
 *
 * Sem sessão: mostra os botões para iniciar sessão ou criar conta.
 * Com sessão: mostra os dados do utilizador e o botão para terminar sessão.
 */
class ContaFragment : Fragment() {

    // o binding só existe entre onCreateView e onDestroyView
    private var _binding: FragmentContaBinding? = null
    /** Atalho para o binding, válido apenas enquanto a view existe. */
    private val binding get() = _binding!!

    /** Sessão do utilizador (mostra os dados da conta e permite terminar sessão). */
    private lateinit var sessao: SessionManager

    /** Cria a view do separador a partir do layout fragment_conta.xml (View Binding). */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContaBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Liga os botões (entrar, criar conta, terminar sessão) depois de a view estar criada. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessao = SessionManager(requireContext())

        binding.botaoEntrar.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
        binding.botaoRegistar.setOnClickListener {
            startActivity(Intent(requireContext(), RegistoActivity::class.java))
        }
        binding.botaoSair.setOnClickListener {
            confirmarTerminarSessao()
        }
    }

    /** Ao voltar ao separador, atualiza o que se mostra (a sessão pode ter mudado no login/registo). */
    override fun onResume() {
        super.onResume()
        // ao voltar do login/registo, o estado da sessão pode ter mudado
        mostrarEstadoDaSessao()
    }

    /** Mostra a zona adequada, consoante exista ou não sessão iniciada. */
    private fun mostrarEstadoDaSessao() {
        val utilizador = sessao.utilizador()
        if (utilizador == null) {
            binding.zonaSemSessao.visibility = View.VISIBLE
            binding.zonaComSessao.visibility = View.GONE
        } else {
            binding.zonaSemSessao.visibility = View.GONE
            binding.zonaComSessao.visibility = View.VISIBLE
            binding.textoNome.text = utilizador.nome
            binding.textoEmail.text = utilizador.email
            val papel = if (sessao.eAdmin()) {
                getString(R.string.papel_admin)
            } else {
                getString(R.string.papel_utilizador)
            }
            binding.textoPapel.text = getString(R.string.perfil, papel)
        }
    }

    /** Pede confirmação antes de terminar a sessão. */
    private fun confirmarTerminarSessao() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.terminar_sessao)
            .setMessage(R.string.terminar_sessao_confirmar)
            .setPositiveButton(R.string.terminar_sessao) { _, _ ->
                // avisa o servidor para invalidar o token (se não houver rede, não faz mal:
                // o token expira sozinho ao fim de 30 dias)
                RetrofitInitializer().apiService().logout(sessao.cabecalhoAutorizacao())
                    .enqueue(object : Callback<APIResult> {
                        /** Resposta do servidor ignorada: a sessão local termina de qualquer forma. */
                        override fun onResponse(call: Call<APIResult>, response: Response<APIResult>) {}
                        /** Sem rede: não faz mal, o token expira sozinho no servidor ao fim de 30 dias. */
                        override fun onFailure(call: Call<APIResult>, t: Throwable) {}
                    })
                sessao.terminarSessao()
                mostrarEstadoDaSessao()
                Toast.makeText(requireContext(), R.string.sessao_terminada, Toast.LENGTH_SHORT)
                    .show()
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    /** Liberta o binding quando a view é destruída (evita fugas de memória). */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
