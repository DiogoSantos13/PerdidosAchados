package pt.uptomar.dam2026.perdidosachados.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import pt.uptomar.dam2026.perdidosachados.R
import pt.uptomar.dam2026.perdidosachados.databinding.ActivityMainBinding
import pt.uptomar.dam2026.perdidosachados.storage.SessionManager
import pt.uptomar.dam2026.perdidosachados.ui.adapter.MainPagerAdapter
import pt.uptomar.dam2026.perdidosachados.util.aplicarMargensDoSistema

/**
 * Ecrã inicial da aplicação.
 *
 * Tem quatro separadores, cada um num fragmento:
 *  - Objetos: todos os objetos perdidos e achados (consulta livre, sem login);
 *  - Os meus: objetos publicados pelo utilizador com sessão iniciada;
 *  - Conta: iniciar/terminar sessão;
 *  - Sobre: curso, disciplina, autores e bibliotecas usadas.
 */
class MainActivity : AppCompatActivity() {

    /** Acesso às views do layout activity_main (View Binding). */
    private lateinit var binding: ActivityMainBinding
    /** Sessão do utilizador (para saber se tem sessão iniciada). */
    private lateinit var sessao: SessionManager

    /** Cria o ecrã inicial: separadores (ViewPager2 + TabLayout) e o botão "Publicar". */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        aplicarMargensDoSistema(binding.main)

        sessao = SessionManager(this)

        // liga os separadores (TabLayout) às páginas (ViewPager2)
        binding.viewPager.adapter = MainPagerAdapter(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { separador, posicao ->
            separador.text = when (posicao) {
                MainPagerAdapter.PAGINA_OBJETOS -> getString(R.string.separador_objetos)
                MainPagerAdapter.PAGINA_MEUS -> getString(R.string.separador_meus)
                MainPagerAdapter.PAGINA_CONTA -> getString(R.string.separador_conta)
                else -> getString(R.string.separador_sobre)
            }
        }.attach()

        // o botão "Publicar" só faz sentido nos separadores com listas de objetos
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            /** Mostra o botão "Publicar" só nos separadores com listas de objetos. */
            override fun onPageSelected(position: Int) {
                if (position == MainPagerAdapter.PAGINA_OBJETOS ||
                    position == MainPagerAdapter.PAGINA_MEUS
                ) {
                    binding.botaoPublicar.show()
                } else {
                    binding.botaoPublicar.hide()
                }
            }
        })

        binding.botaoPublicar.setOnClickListener {
            publicarObjeto()
        }
    }

    /**
     * Abre o formulário para publicar um objeto.
     * Controlo de acesso: só utilizadores com sessão iniciada podem publicar.
     */
    private fun publicarObjeto() {
        if (sessao.temSessao()) {
            startActivity(Intent(this, FormularioObjetoActivity::class.java))
        } else {
            AlertDialog.Builder(this)
                .setTitle(R.string.sessao_necessaria_titulo)
                .setMessage(R.string.sessao_necessaria_publicar)
                .setPositiveButton(R.string.iniciar_sessao) { _, _ ->
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                .setNegativeButton(R.string.cancelar, null)
                .show()
        }
    }
}
