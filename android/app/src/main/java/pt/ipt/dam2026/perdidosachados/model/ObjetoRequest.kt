package pt.ipt.dam2026.perdidosachados.model

import com.google.gson.annotations.SerializedName

/**
 * Dados enviados à API para criar (POST) ou alterar (PUT) um objeto.
 *
 * Todos os campos são opcionais: o Gson não envia os campos a null, por isso
 * numa edição só seguem os campos que se pretende alterar
 * (ex.: só o estado, ao marcar um objeto como resolvido).
 */
data class ObjetoRequest(
    @SerializedName("tipo") val tipo: String? = null,
    @SerializedName("titulo") val titulo: String? = null,
    @SerializedName("descricao") val descricao: String? = null,
    @SerializedName("categoria") val categoria: String? = null,
    @SerializedName("local") val local: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("data_ocorrencia") val dataOcorrencia: String? = null,
    @SerializedName("estado") val estado: String? = null,
    @SerializedName("foto_base64") val fotoBase64: String? = null,  // fotografia JPEG em Base64
    // true = apagar as coordenadas GPS que o objeto tinha (só usado na edição)
    @SerializedName("remover_localizacao") val removerLocalizacao: Boolean? = null
)
