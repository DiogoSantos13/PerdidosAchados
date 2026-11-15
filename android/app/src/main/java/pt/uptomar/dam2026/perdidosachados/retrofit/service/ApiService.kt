package pt.uptomar.dam2026.perdidosachados.retrofit.service

import pt.uptomar.dam2026.perdidosachados.model.APIResult
import pt.uptomar.dam2026.perdidosachados.model.AuthResponse
import pt.uptomar.dam2026.perdidosachados.model.LoginRequest
import pt.uptomar.dam2026.perdidosachados.model.Objeto
import pt.uptomar.dam2026.perdidosachados.model.ObjetoRequest
import pt.uptomar.dam2026.perdidosachados.model.RegistoRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Interface onde se indicam os endpoints da API REST.
 *
 * O Retrofit gera a implementação desta interface. Os parâmetros a null
 * (@Query e @Header) não são enviados no pedido.
 *
 * O parâmetro "token" segue no cabeçalho HTTP Authorization, no formato
 * "Bearer xxxxx" (ver SessionManager.cabecalhoAutorizacao()).
 */
interface ApiService {

    // ---------- Autenticação ----------

    @POST("api/auth/login")
    fun login(@Body pedido: LoginRequest): Call<AuthResponse>

    @POST("api/auth/registar")
    fun registar(@Body pedido: RegistoRequest): Call<AuthResponse>

    /** Invalida o token no servidor (o resultado não é crítico para a app). */
    @POST("api/auth/logout")
    fun logout(@Header("Authorization") token: String?): Call<APIResult>

    // ---------- Objetos ----------

    /** Lista os objetos. Filtros opcionais: tipo ("perdido"/"achado") e texto a pesquisar. */
    @GET("api/objetos")
    fun listarObjetos(
        @Query("tipo") tipo: String?,
        @Query("q") pesquisa: String?,
        @Header("Authorization") token: String?
    ): Call<List<Objeto>>

    /** Lista os objetos publicados pelo utilizador autenticado. */
    @GET("api/objetos/meus")
    fun listarMeusObjetos(@Header("Authorization") token: String?): Call<List<Objeto>>

    @GET("api/objetos/{id}")
    fun obterObjeto(
        @Path("id") id: Int,
        @Header("Authorization") token: String?
    ): Call<Objeto>

    @POST("api/objetos")
    fun criarObjeto(
        @Header("Authorization") token: String?,
        @Body objeto: ObjetoRequest
    ): Call<Objeto>

    @PUT("api/objetos/{id}")
    fun alterarObjeto(
        @Path("id") id: Int,
        @Header("Authorization") token: String?,
        @Body objeto: ObjetoRequest
    ): Call<Objeto>

    @DELETE("api/objetos/{id}")
    fun eliminarObjeto(
        @Path("id") id: Int,
        @Header("Authorization") token: String?
    ): Call<APIResult>
}
