package pt.ipt.dam2026.perdidosachados.storage

import android.content.Context
import android.content.SharedPreferences
import pt.ipt.dam2026.perdidosachados.model.Utilizador

/**
 * Guarda localmente a sessão do utilizador (token e dados da conta),
 * usando Shared Preferences.
 *
 * Assim o utilizador não precisa de iniciar sessão sempre que abre a app.
 */
class SessionManager(context: Context) {

    companion object {
        private const val FICHEIRO = "sessao"
        private const val CHAVE_TOKEN = "token"
        private const val CHAVE_ID = "id"
        private const val CHAVE_NOME = "nome"
        private const val CHAVE_EMAIL = "email"
        private const val CHAVE_PAPEL = "papel"
        const val PAPEL_ADMIN = "admin"
    }

    private val preferencias: SharedPreferences =
        context.applicationContext.getSharedPreferences(FICHEIRO, Context.MODE_PRIVATE)

    /** Guarda a sessão depois de um login ou registo com sucesso. */
    fun guardarSessao(token: String, utilizador: Utilizador) {
        val editor = preferencias.edit()
        editor.putString(CHAVE_TOKEN, token)
        editor.putInt(CHAVE_ID, utilizador.id)
        editor.putString(CHAVE_NOME, utilizador.nome)
        editor.putString(CHAVE_EMAIL, utilizador.email)
        editor.putString(CHAVE_PAPEL, utilizador.papel)
        editor.apply()
    }

    /** Termina a sessão, apagando os dados guardados. */
    fun terminarSessao() {
        preferencias.edit().clear().apply()
    }

    /** Indica se existe uma sessão iniciada. */
    fun temSessao(): Boolean = preferencias.getString(CHAVE_TOKEN, null) != null

    /** Devolve o utilizador com sessão iniciada, ou null se não houver sessão. */
    fun utilizador(): Utilizador? {
        if (!temSessao()) return null
        return Utilizador(
            preferencias.getInt(CHAVE_ID, -1),
            preferencias.getString(CHAVE_NOME, ""),
            preferencias.getString(CHAVE_EMAIL, ""),
            preferencias.getString(CHAVE_PAPEL, "")
        )
    }

    /** Indica se o utilizador com sessão iniciada é administrador. */
    fun eAdmin(): Boolean = utilizador()?.papel == PAPEL_ADMIN

    /**
     * Valor a enviar no cabeçalho HTTP "Authorization" ("Bearer token"),
     * ou null se não houver sessão (nesse caso o cabeçalho não é enviado).
     */
    fun cabecalhoAutorizacao(): String? {
        val token = preferencias.getString(CHAVE_TOKEN, null) ?: return null
        return "Bearer $token"
    }
}
