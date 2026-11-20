package pt.ipt.dam2026.perdidosachados.storage

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import pt.ipt.dam2026.perdidosachados.retrofit.RetrofitInitializer

/**
 * Mostra as fotografias dos objetos, descarregadas da API.
 *
 * Usa a biblioteca Glide (https://github.com/bumptech/glide), que trata sozinha de:
 *  - descarregar a imagem numa thread secundária;
 *  - guardá-la em cache (memória e disco), para não a pedir outra vez;
 *  - cancelar o pedido quando a ImageView é reutilizada pela lista (RecyclerView).
 *
 * Esta classe existe apenas para concentrar num só sítio o endereço da API e
 * a imagem por omissão, evitando repetir o código em todos os ecrãs.
 */
object ImageLoader {

    /**
     * Mostra na [imageView] a fotografia com o caminho [fotoUrl] (relativo à API).
     * Enquanto carrega, se falhar, ou se não houver foto, mostra [imagemPorOmissao].
     */
    fun carregar(context: Context, fotoUrl: String?, imageView: ImageView, imagemPorOmissao: Int) {
        if (fotoUrl.isNullOrBlank()) {
            // sem foto: cancela qualquer pedido anterior desta view e mostra a imagem por omissão
            Glide.with(context).clear(imageView)
            imageView.setImageResource(imagemPorOmissao)
            return
        }

        Glide.with(context)
            .load(RetrofitInitializer.BASE_URL + fotoUrl)
            .placeholder(imagemPorOmissao)   // enquanto descarrega
            .error(imagemPorOmissao)         // se o download falhar
            .into(imageView)
    }

    /** Cancela um carregamento pendente da [imageView] (por exemplo, antes de pôr outra imagem). */
    fun cancelar(context: Context, imageView: ImageView) {
        Glide.with(context).clear(imageView)
    }
}
