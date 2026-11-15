package pt.uptomar.dam2026.perdidosachados.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import pt.uptomar.dam2026.perdidosachados.R

/**
 * Separador "Sobre", acessível a partir do ecrã inicial.
 *
 * Mostra o curso, a disciplina e o ano letivo, o número, nome e fotografia dos
 * autores, e as bibliotecas e código de terceiros usados no trabalho.
 * Todo o conteúdo é estático e está definido em fragment_sobre.xml e strings.xml.
 */
class SobreFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sobre, container, false)
    }
}
