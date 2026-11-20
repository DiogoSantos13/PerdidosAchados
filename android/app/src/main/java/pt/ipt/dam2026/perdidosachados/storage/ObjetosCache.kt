package pt.ipt.dam2026.perdidosachados.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pt.ipt.dam2026.perdidosachados.model.Objeto
import java.io.File

/**
 * Guarda no armazenamento interno (Internal Storage) a última lista de objetos
 * recebida da API, em formato JSON.
 *
 * Serve para que a app continue a mostrar os objetos quando não há ligação
 * à Internet (modo de consulta offline).
 */
class ObjetosCache(context: Context) {

    private val ficheiro = File(context.applicationContext.filesDir, "objetos.json")
    private val gson = Gson()

    /** Escreve a lista no ficheiro (substitui o conteúdo anterior). */
    fun guardar(objetos: List<Objeto>) {
        try {
            ficheiro.writeText(gson.toJson(objetos))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Lê a lista guardada. Devolve uma lista vazia se ainda não existir ficheiro. */
    fun ler(): List<Objeto> {
        return try {
            if (!ficheiro.exists()) return emptyList()
            val tipo = object : TypeToken<List<Objeto>>() {}.type
            gson.fromJson<List<Objeto>>(ficheiro.readText(), tipo) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
