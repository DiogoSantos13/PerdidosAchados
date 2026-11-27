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

    /** Membros partilhados por toda a app: endereço da API e o serviço Retrofit (criado uma só vez). */
    companion object {
        /**
         * Endereço base da API (tem de terminar em "/").
         *
         * Desenvolvimento local (API a correr no PC com "python app.py"):
         *   "http://localhost:5000/" + no PC, com o emulador ligado, correr UMA vez:
         *       adb reverse tcp:5000 tcp:5000
         *   (o adb reencaminha a porta 5000 do emulador para a porta 5000 do PC;
         *   é preciso repetir sempre que o emulador é reiniciado).
         *   Alternativa sem adb: "http://10.0.2.2:5000/" (o "localhost" do PC visto
         *   pelo emulador) — nalguns PCs dá timeout, daí usarmos o adb reverse.
         *
         * Entrega/Google Play: trocar pelo endereço público da API (HTTPS).
         */
        const val BASE_URL = "http://localhost:5000/"

        // O Retrofit é criado uma única vez e reutilizado em toda a app
        // setLenient (como nos slides) aceita JSON menos rigoroso durante o desenvolvimento
        private val gson: Gson = GsonBuilder().setLenient().create()

        private val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        private val servico: ApiService = retrofit.create(ApiService::class.java)
    }

    /** Devolve o serviço com os endpoints da API. */
    fun apiService(): ApiService = servico
}
