package pt.uptomar.dam2026.perdidosachados.util

import android.location.Location
import pt.uptomar.dam2026.perdidosachados.model.Objeto

/**
 * Calcula a distância, em metros, entre a posição [origem] (dada pelo GPS)
 * e o local onde o [objeto] foi perdido/achado.
 * Devolve null se o objeto não tiver coordenadas.
 */
fun distanciaEmMetros(origem: Location, objeto: Objeto): Float? {
    val latitude = objeto.latitude ?: return null
    val longitude = objeto.longitude ?: return null
    val resultado = FloatArray(1)
    Location.distanceBetween(origem.latitude, origem.longitude, latitude, longitude, resultado)
    return resultado[0]
}
