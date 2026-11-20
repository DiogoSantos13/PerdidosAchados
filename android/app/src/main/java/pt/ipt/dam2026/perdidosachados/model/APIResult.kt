package pt.ipt.dam2026.perdidosachados.model

import com.google.gson.annotations.SerializedName

/** Resultado genérico de uma operação na API. Ex.: {"code":"200","description":"ok"} */
data class APIResult(
    @SerializedName("code") val code: String?,
    @SerializedName("description") val description: String?
)

/** Corpo devolvido pela API quando ocorre um erro. Ex.: {"erro":"Objeto não encontrado."} */
data class ErroAPI(
    @SerializedName("erro") val erro: String?
)
