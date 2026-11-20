package pt.ipt.dam2026.perdidosachados.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import pt.ipt.dam2026.perdidosachados.ui.fragment.ContaFragment
import pt.ipt.dam2026.perdidosachados.ui.fragment.ListaObjetosFragment
import pt.ipt.dam2026.perdidosachados.ui.fragment.SobreFragment

/**
 * Indica ao ViewPager2 que fragmento mostrar em cada separador do ecrã inicial.
 */
class MainPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    companion object {
        const val PAGINA_OBJETOS = 0
        const val PAGINA_MEUS = 1
        const val PAGINA_CONTA = 2
        const val PAGINA_SOBRE = 3
        private const val TOTAL_PAGINAS = 4
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            PAGINA_OBJETOS -> ListaObjetosFragment.novaInstancia(false)
            PAGINA_MEUS -> ListaObjetosFragment.novaInstancia(true)
            PAGINA_CONTA -> ContaFragment()
            else -> SobreFragment()
        }
    }

    override fun getItemCount(): Int = TOTAL_PAGINAS
}
