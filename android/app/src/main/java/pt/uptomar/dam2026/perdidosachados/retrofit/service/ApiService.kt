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

    /** Inicia sessão com email e palavra-passe; a resposta traz o token de sessão e o utilizador. */
    @POST("api/auth/login")
    fun login(@Body pedido: LoginRequest): Call<AuthResponse>

    /** Cria uma conta nova; a resposta traz já o token (fica com sessão iniciada). */
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

    /** Devolve um objeto pelo seu id (o contacto só vem preenchido com sessão iniciada). */
    @GET("api/objetos/{id}")
    fun obterObjeto(
        @Path("id") id: Int,
        @Header("Authorization") token: String?
    ): Call<Objeto>

    /** Publica um objeto novo (requer sessão iniciada). */
    @POST("api/objetos")
    fun criarObjeto(
        @Header("Authorization") token: String?,
        @Body objeto: ObjetoRequest
    ): Call<Objeto>

    /** Altera um objeto; só o dono ou um administrador o pode fazer. */
    @PUT("api/objetos/{id}")
    fun alterarObjeto(
        @Path("id") id: Int,
        @Header("Authorization") token: String?,
        @Body objeto: ObjetoRequest
    ): Call<Objeto>

    /** Elimina um objeto; só o dono ou um administrador o pode fazer. */
    @DELETE("api/objetos/{id}")
    fun eliminarObjeto(
        @Path("id") id: Int,
        @Header("Authorization") token: String?
    ): Call<APIResult>
}
