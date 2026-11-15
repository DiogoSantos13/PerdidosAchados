package pt.uptomar.dam2026.perdidosachados.model

import com.google.gson.annotations.SerializedName

/** Utilizador da aplicação. O papel pode ser "utilizador" ou "admin". */
data class Utilizador(
    @SerializedName("id") val id: Int,
    @SerializedName("nome") val nome: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("papel") val papel: String?
)
