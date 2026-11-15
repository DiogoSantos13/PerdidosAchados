package pt.uptomar.dam2026.perdidosachados.retrofit

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import pt.uptomar.dam2026.perdidosachados.retrofit.service.ApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Parametriza a localização da API e cria o serviço Retrofit.
 *
 * Biblioteca Retrofit (Square, Apache 2.0): https://square.github.io/retrofit/
 * Biblioteca Gson (Google, Apache 2.0): https://github.com/google/gson
 */
class RetrofitInitializer {

    companion object {
        /**
         * Endereço base da API (tem de terminar em "/").
         *
         * - "http://10.0.2.2:5000/" -> API a correr no computador onde corre o emulador
         * - para a entrega/Google Play, trocar pelo endereço público (de preferência HTTPS),
         *   ex.: "https://o-vosso-utilizador.pythonanywhere.com/"
         */
        const val BASE_URL = "http://10.0.2.2:5000/"

        // O Retrofit é criado uma única vez e reutilizado em toda a app
        private val gson: Gson = GsonBuilder().create()

        private val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        private val servico: ApiService = retrofit.create(ApiService::class.java)
    }

    /** Devolve o serviço com os endpoints da API. */
    fun apiService(): ApiService = servico
}
