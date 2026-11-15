package pt.uptomar.dam2026.perdidosachados.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import pt.uptomar.dam2026.perdidosachados.retrofit.RetrofitInitializer
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Carrega as fotografias dos objetos a partir da API e guarda-as na cache da app.
 *
 * Foi escrito por nós, sem bibliotecas externas (como Glide ou Coil):
 *  1. se a imagem já estiver na pasta de cache, é lida do disco;
 *  2. caso contrário é descarregada numa thread secundária e guardada em cache;
 *  3. no fim, a imagem é colocada na ImageView, já na thread principal.
 *
 * O Android pode apagar a pasta de cache quando precisa de espaço, o que não é
 * problema: a imagem volta a ser descarregada.
 */
object ImageLoader {

    // threads secundárias para não bloquear a interface durante o download
    private val executor = Executors.newFixedThreadPool(3)

    // permite voltar à thread principal para atualizar a interface
    private val threadPrincipal = Handler(Looper.getMainLooper())

    /**
     * Mostra na [imageView] a fotografia com o caminho [fotoUrl] (relativo à API).
     * Enquanto carrega, ou se não houver foto, mostra a imagem [imagemPorOmissao].
     */
    fun carregar(context: Context, fotoUrl: String?, imageView: ImageView, imagemPorOmissao: Int) {
        imageView.setImageResource(imagemPorOmissao)

        if (fotoUrl.isNullOrBlank()) {
            imageView.tag = null
            return
        }

        // a tag regista que imagem esta ImageView está à espera de receber;
        // numa lista as views são reutilizadas e podem entretanto mudar de objeto
        imageView.tag = fotoUrl
        val pastaCache = context.applicationContext.cacheDir

        executor.execute {
            val bitmap = obterBitmap(pastaCache, fotoUrl)
            threadPrincipal.post {
                if (bitmap != null && imageView.tag == fotoUrl) {
                    imageView.setImageBitmap(bitmap)
                }
            }
        }
    }

    /** Lê a imagem da cache ou, se não existir, descarrega-a da API. */
    private fun obterBitmap(pastaCache: File, fotoUrl: String): Bitmap? {
        return try {
            val ficheiro = File(pastaCache, "foto_" + fotoUrl.hashCode() + ".jpg")
            if (!ficheiro.exists()) {
                descarregar(RetrofitInitializer.BASE_URL + fotoUrl, ficheiro)
            }
            BitmapFactory.decodeFile(ficheiro.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Descarrega o conteúdo do [endereco] para o [destino]. */
    private fun descarregar(endereco: String, destino: File) {
        val ligacao = URL(endereco).openConnection() as HttpURLConnection
        ligacao.connectTimeout = 10000
        ligacao.readTimeout = 15000
        try {
            if (ligacao.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Erro HTTP " + ligacao.responseCode)
            }
            // escreve primeiro num ficheiro temporário, para nunca ficar
            // em cache uma imagem incompleta se o download falhar a meio
            val temporario = File(destino.parentFile, destino.name + ".tmp")
            ligacao.inputStream.use { entrada ->
                temporario.outputStream().use { saida ->
                    entrada.copyTo(saida)
                }
            }
            temporario.renameTo(destino)
        } finally {
            ligacao.disconnect()
        }
    }
}
