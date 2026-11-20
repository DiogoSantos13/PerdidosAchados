package pt.ipt.dam2026.perdidosachados.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * Obtém a localização atual do dispositivo através do GPS (e da rede, se disponível).
 *
 * Usa o LocationManager do Android, sem bibliotecas externas.
 * Modo de utilização:
 *  1. confirmar/pedir a permissão de localização (fica a cargo do ecrã);
 *  2. chamar [pedirLocalizacao];
 *  3. o resultado chega à função [aoObter]: a localização, ou null se não
 *     foi possível obtê-la dentro do tempo limite.
 *
 * @param aoObter função chamada (uma única vez por pedido) com o resultado
 */
class LocalizacaoHelper(
    context: Context,
    private val aoObter: (Location?) -> Unit
) : LocationListener {

    companion object {
        /** Tempo máximo de espera por uma posição, em milissegundos. */
        private const val TEMPO_LIMITE = 15000L
    }

    private val contexto = context.applicationContext
    private val locationManager =
        contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val handler = Handler(Looper.getMainLooper())

    // se o tempo limite for atingido, devolve a última posição conhecida (ou null)
    private val tempoEsgotado = Runnable {
        parar()
        aoObter(ultimaLocalizacaoConhecida())
    }

    /** Indica se o utilizador já deu permissão de localização (exata ou aproximada). */
    fun temPermissao(): Boolean {
        return ContextCompat.checkSelfPermission(
            contexto, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                contexto, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    /** Fornecedores de localização (GPS e rede) que estão ligados no dispositivo. */
    private fun fornecedoresAtivos(): List<String> {
        val ativos = ArrayList<String>()
        for (fornecedor in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            try {
                if (locationManager.isProviderEnabled(fornecedor)) ativos.add(fornecedor)
            } catch (e: Exception) {
                // o dispositivo não tem este fornecedor
            }
        }
        return ativos
    }

    /** Indica se a localização está ligada nas definições do dispositivo. */
    fun localizacaoLigada(): Boolean = fornecedoresAtivos().isNotEmpty()

    /**
     * Pede a localização atual. O resultado é entregue em [aoObter].
     * Só deve ser chamado depois de a permissão ter sido concedida.
     */
    @SuppressLint("MissingPermission")
    fun pedirLocalizacao() {
        val fornecedores = fornecedoresAtivos()
        if (!temPermissao() || fornecedores.isEmpty()) {
            aoObter(null)
            return
        }
        parar() // cancela um pedido anterior que ainda esteja a decorrer
        try {
            for (fornecedor in fornecedores) {
                locationManager.requestLocationUpdates(
                    fornecedor, 0L, 0f, this, Looper.getMainLooper()
                )
            }
            handler.postDelayed(tempoEsgotado, TEMPO_LIMITE)
        } catch (e: Exception) {
            e.printStackTrace()
            parar()
            aoObter(null)
        }
    }

    /** Devolve a posição conhecida mais recente, sem ligar o GPS (pode ser null). */
    @SuppressLint("MissingPermission")
    fun ultimaLocalizacaoConhecida(): Location? {
        if (!temPermissao()) return null
        var melhor: Location? = null
        for (fornecedor in fornecedoresAtivos()) {
            try {
                val localizacao = locationManager.getLastKnownLocation(fornecedor)
                if (localizacao != null && (melhor == null || localizacao.time > melhor.time)) {
                    melhor = localizacao
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return melhor
    }

    /** Deixa de receber atualizações (poupa bateria). Chamar no onStop/onDestroy do ecrã. */
    fun parar() {
        handler.removeCallbacks(tempoEsgotado)
        try {
            locationManager.removeUpdates(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Chamado pelo Android quando há uma nova posição: basta-nos a primeira. */
    override fun onLocationChanged(location: Location) {
        parar()
        aoObter(location)
    }

    // Os três métodos seguintes têm de existir (vazios) para a app não falhar
    // em versões do Android anteriores à 11, onde não têm implementação por omissão.
    @Deprecated("Deixou de ser chamado a partir do Android 10")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String) {
    }

    override fun onProviderDisabled(provider: String) {
    }
}
