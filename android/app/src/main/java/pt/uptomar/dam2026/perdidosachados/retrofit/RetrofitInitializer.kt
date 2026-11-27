package pt.uptomar.dam2026.perdidosachados.retrofit

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import pt.uptomar.dam2026.perdidosachados.retrofit.service.ApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Parametriza a localização da API e cria o serviço Retrofit.
 *
 * Biblioteca Retrofit (Square, Apache 2.0): https://square.github.io/retrofit/
 * Biblioteca Gson (Google, Apache 2.0): https://github.com/google/gson
 * Biblioteca OkHttp (Square, Apache 2.0), usada pelo Retrofit: https://square.github.io/okhttp/
 */
class RetrofitInitializer {

    /** Membros partilhados por toda a app: endereço da API e o serviço Retrofit (criado uma só vez). */
    companion object {
        /**
         * Endereço base da API (tem de terminar em "/").
         *
         * Por omissão é a API alojada no Render (HTTPS), que funciona em qualquer
         * telemóvel ou emulador sem configurar nada. No plano gratuito o serviço
         * "adormece" quando não é usado: o primeiro pedido pode demorar cerca de 1 minuto.
         *
         * Desenvolvimento local (API a correr no PC com "python app.py"):
         *   trocar por "http://localhost:5000/" e, no PC, com o emulador ligado,
         *   correr UMA vez:
         *       adb reverse tcp:5000 tcp:5000
         *   (o adb reencaminha a porta 5000 do emulador para a porta 5000 do PC;
         *   é preciso repetir sempre que o emulador é reiniciado).
         *   Alternativa sem adb: "http://10.0.2.2:5000/" (o "localhost" do PC visto
         *   pelo emulador) — nalguns PCs dá timeout, daí usarmos o adb reverse.
         */
        const val BASE_URL = "https://perdidosachados-api.onrender.com/"

        // O Retrofit é criado uma única vez e reutilizado em toda a app
        // setLenient (como nos slides) aceita JSON menos rigoroso durante o desenvolvimento
        private val gson: Gson = GsonBuilder().setLenient().create()

        /**
         * Cliente HTTP com tempos de espera ajustados:
         *  - ligação (connect): 15 s — se a API estiver desligada, o erro aparece depressa;
         *  - resposta (read/write): 60 s — uma API alojada num plano gratuito (ex.: Render)
         *    "adormece" quando não é usada, e o primeiro pedido pode demorar cerca de um minuto
         *    a responder (a ligação é aceite logo; a demora está na resposta).
         */
        private val clienteHttp: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        private val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(clienteHttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        private val servico: ApiService = retrofit.create(ApiService::class.java)
    }

    /** Devolve o serviço com os endpoints da API. */
    fun apiService(): ApiService = servico
}
