package pt.ipt.dam2026.perdidosachados.model

import com.google.gson.annotations.SerializedName

/** Dados enviados no login. */
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

/** Dados enviados no registo de uma conta nova. */
data class RegistoRequest(
    @SerializedName("nome") val nome: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

/** Resposta da API a um login ou registo com sucesso. */
data class AuthResponse(
    @SerializedName("token") val token: String?,
    @SerializedName("utilizador") val utilizador: Utilizador?
)
