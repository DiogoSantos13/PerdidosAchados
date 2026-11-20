package pt.ipt.dam2026.perdidosachados.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Ajusta as margens interiores da [view] principal de um ecrã para que o
 * conteúdo não fique por baixo das barras do sistema nem do teclado
 * (necessário porque as atividades usam enableEdgeToEdge()).
 */
fun aplicarMargensDoSistema(view: View) {
    ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
        val margens = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
        )
        v.setPadding(margens.left, margens.top, margens.right, margens.bottom)
        insets
    }
}
