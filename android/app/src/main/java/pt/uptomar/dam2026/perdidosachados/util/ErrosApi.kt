package pt.uptomar.dam2026.perdidosachados.util

import android.content.Context
import com.google.gson.Gson
import pt.uptomar.dam2026.perdidosachados.R
import pt.uptomar.dam2026.perdidosachados.model.ErroAPI
import retrofit2.Response
import java.io.IOException

/**
 * Transforma os erros da API e da rede em mensagens claras, em português,
 * para mostrar ao utilizador.
 */
object ErrosApi {

    /**
     * Mensagem para uma resposta da API com código de erro (4xx ou 5xx).
     * A API devolve {"erro": "mensagem"}; se não for possível ler essa
     * mensagem, usa-se um texto genérico de acordo com o código HTTP.
     */
    fun mensagem(context: Context, resposta: Response<*>): String {
        try {
            val corpo = resposta.errorBody()?.string()
            if (!corpo.isNullOrBlank()) {
                val erro = Gson().fromJson(corpo, ErroAPI::class.java)
                if (!erro?.erro.isNullOrBlank()) {
                    return erro.erro!!
                }
            }
        } catch (e: Exception) {
            // o corpo não era JSON válido: segue para as mensagens genéricas
            e.printStackTrace()
        }
        return when (resposta.code()) {
            401 -> context.getString(R.string.erro_sessao)
            403 -> context.getString(R.string.erro_permissao)
            404 -> context.getString(R.string.erro_nao_encontrado)
            else -> context.getString(R.string.erro_servidor, resposta.code())
        }
    }

    /** Mensagem para um pedido que nem chegou a obter resposta (ex.: sem Internet). */
    fun mensagemFalha(context: Context, falha: Throwable): String {
        falha.printStackTrace()
        return if (falha is IOException) {
            context.getString(R.string.erro_ligacao)
        } else {
            context.getString(R.string.erro_inesperado)
        }
    }
}
