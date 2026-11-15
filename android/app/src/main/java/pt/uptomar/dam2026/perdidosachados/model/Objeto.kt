package pt.uptomar.dam2026.perdidosachados.model

import com.google.gson.annotations.SerializedName

/**
 * Representa um objeto perdido ou achado, tal como é devolvido pela API.
 *
 * A anotação @SerializedName indica o nome do campo no JSON, que pode ser
 * diferente do nome do atributo na classe.
 */
data class Objeto(
    @SerializedName("id") val id: Int,
    @SerializedName("tipo") val tipo: String?,                 // "perdido" ou "achado"
    @SerializedName("titulo") val titulo: String?,
    @SerializedName("descricao") val descricao: String?,
    @SerializedName("categoria") val categoria: String?,
    @SerializedName("local") val local: String?,               // descrição textual do local
    @SerializedName("latitude") val latitude: Double?,         // coordenadas GPS (opcionais)
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("data_ocorrencia") val dataOcorrencia: String?,   // AAAA-MM-DD
    @SerializedName("estado") val estado: String?,             // "ativo" ou "resolvido"
    @SerializedName("foto_url") val fotoUrl: String?,          // caminho relativo da fotografia
    @SerializedName("utilizador_id") val utilizadorId: Int,
    @SerializedName("utilizador_nome") val utilizadorNome: String?,
    @SerializedName("contacto") val contacto: String?,         // só vem preenchido com sessão iniciada
    @SerializedName("criado_em") val criadoEm: String?
) {
    companion object {
        const val TIPO_PERDIDO = "perdido"
        const val TIPO_ACHADO = "achado"
        const val ESTADO_ATIVO = "ativo"
        const val ESTADO_RESOLVIDO = "resolvido"
    }

    /** Indica se o objeto tem coordenadas GPS associadas. */
    fun temCoordenadas(): Boolean = latitude != null && longitude != null
}
