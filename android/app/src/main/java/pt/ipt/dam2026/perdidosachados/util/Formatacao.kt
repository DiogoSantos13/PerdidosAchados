package pt.ipt.dam2026.perdidosachados.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Funções para apresentar datas e distâncias ao utilizador. */
object Formatacao {

    // formato usado pela API (AAAA-MM-DD) e formato mostrado ao utilizador (DD/MM/AAAA)
    private const val FORMATO_API = "yyyy-MM-dd"
    private const val FORMATO_ECRA = "dd/MM/yyyy"

    /** Converte "2026-11-13" em "13/11/2026". Se a data for inválida devolve-a tal como veio. */
    fun dataParaMostrar(dataApi: String?): String {
        if (dataApi.isNullOrBlank()) return ""
        return try {
            val data = SimpleDateFormat(FORMATO_API, Locale.US).parse(dataApi)
            if (data == null) dataApi else SimpleDateFormat(FORMATO_ECRA, Locale.US).format(data)
        } catch (e: Exception) {
            dataApi
        }
    }

    /** Converte uma data (Calendar) no formato usado pela API. */
    fun dataParaApi(data: Calendar): String =
        SimpleDateFormat(FORMATO_API, Locale.US).format(data.time)

    /** Converte uma data no formato da API num Calendar (hoje, se a data for inválida). */
    fun dataDaApi(dataApi: String?): Calendar {
        val calendario = Calendar.getInstance()
        try {
            val data = SimpleDateFormat(FORMATO_API, Locale.US).parse(dataApi ?: "")
            if (data != null) calendario.time = data
        } catch (e: Exception) {
            // mantém a data de hoje
        }
        return calendario
    }

    /** Apresenta uma distância em metros ("120 m") ou quilómetros ("1,4 km"). */
    fun distancia(metros: Float): String {
        return if (metros < 1000) {
            String.format(Locale.US, "%d m", metros.toInt())
        } else {
            String.format(Locale("pt", "PT"), "%.1f km", metros / 1000)
        }
    }
}
