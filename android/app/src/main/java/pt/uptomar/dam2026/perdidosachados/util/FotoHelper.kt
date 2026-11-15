package pt.uptomar.dam2026.perdidosachados.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Funções de apoio às fotografias dos objetos: criar o ficheiro onde a câmara
 * grava a foto, reduzir o tamanho da imagem e convertê-la para Base64.
 */
object FotoHelper {

    /** Maior dimensão (largura ou altura), em pixéis, das fotografias enviadas à API. */
    private const val LADO_MAXIMO = 1024

    /** Qualidade da compressão JPEG (0 a 100). */
    private const val QUALIDADE_JPEG = 75

    /**
     * Cria um ficheiro vazio na cache da app e devolve o respetivo Uri, para ser
     * entregue à app da câmara. A pasta "fotos" está declarada em res/xml/file_paths.xml.
     */
    fun criarUriParaFoto(context: Context): Uri {
        val pasta = File(context.cacheDir, "fotos")
        pasta.mkdirs()
        val ficheiro = File(pasta, "captura_" + System.currentTimeMillis() + ".jpg")
        return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", ficheiro)
    }

    /**
     * Lê a imagem indicada pelo [uri], reduzida para no máximo [LADO_MAXIMO] pixéis
     * de lado e já com a orientação corrigida. Devolve null se não for possível ler.
     *
     * As fotografias da câmara têm vários megabytes; reduzi-las poupa memória,
     * dados móveis e espaço no servidor.
     */
    fun carregarBitmapReduzido(context: Context, uri: Uri): Bitmap? {
        return try {
            // 1.º passo: ler só as dimensões da imagem (não ocupa memória)
            val opcoes = BitmapFactory.Options()
            opcoes.inJustDecodeBounds = true
            context.contentResolver.openInputStream(uri)?.use { entrada ->
                BitmapFactory.decodeStream(entrada, null, opcoes)
            }
            if (opcoes.outWidth <= 0 || opcoes.outHeight <= 0) return null

            // 2.º passo: ler a imagem já reduzida para metade, um quarto, etc.
            var fator = 1
            while (maxOf(opcoes.outWidth, opcoes.outHeight) / (fator * 2) >= LADO_MAXIMO) {
                fator *= 2
            }
            opcoes.inJustDecodeBounds = false
            opcoes.inSampleSize = fator
            var bitmap = context.contentResolver.openInputStream(uri)?.use { entrada ->
                BitmapFactory.decodeStream(entrada, null, opcoes)
            } ?: return null

            // 3.º passo: ajustar ao tamanho máximo exato
            val maior = maxOf(bitmap.width, bitmap.height)
            if (maior > LADO_MAXIMO) {
                val escala = LADO_MAXIMO.toFloat() / maior
                bitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * escala).toInt(),
                    (bitmap.height * escala).toInt(),
                    true
                )
            }

            // 4.º passo: rodar a imagem, se a câmara a gravou "deitada"
            val graus = rotacaoDaFoto(context, uri)
            if (graus != 0) {
                val matriz = Matrix()
                matriz.postRotate(graus.toFloat())
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Lê dos metadados EXIF quantos graus a fotografia tem de ser rodada. */
    private fun rotacaoDaFoto(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { entrada ->
                val orientacao = ExifInterface(entrada).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
                when (orientacao) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /** Comprime a imagem em JPEG e converte-a em texto Base64, para seguir no JSON. */
    fun paraBase64(bitmap: Bitmap): String {
        val saida = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, QUALIDADE_JPEG, saida)
        return Base64.encodeToString(saida.toByteArray(), Base64.NO_WRAP)
    }
}
