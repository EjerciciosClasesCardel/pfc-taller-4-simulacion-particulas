# Informe de paralelización: un paso de la simulación de partículas

## El cálculo

Un paso de la simulación aplica `mover` a cada una de las $n$ partículas. Cada
partícula se mueve sin mirar a las demás, así que el trabajo se puede repartir
sin coordinación entre los tramos.

```Scala
def pasoSegmento(s: Sistema, dt: Double, desde: Int, hasta: Int): Sistema =
  (for (i <- desde until hasta) yield mover(s(i), dt)).toArray

def pasoSecuencial(s: Sistema, dt: Double): Sistema =
  pasoSegmento(s, dt, 0, s.length)
```

La versión paralela usa la misma función sobre cada tramo; lo único que cambia
es quién reparte:

```Scala
def pasoParalelo(s: Sistema, dt: Double, umbral: Int): Sistema = {
  val piso = Math.max(umbral, 1)

  def paso(desde: Int, hasta: Int): Sistema =
    if (hasta - desde <= piso) pasoSegmento(s, dt, desde, hasta)
    else {
      val mitad = desde + (hasta - desde) / 2
      val (izquierda, derecha) = parallel(paso(desde, mitad), paso(mitad, hasta))
      izquierda ++ derecha
    }

  paso(0, s.length)
}
```

## La estrategia de reparto

El tramo se parte por la mitad en `mitad`, y cada mitad se resuelve sin mirar la
otra: ningún índice aparece en las dos y ninguno se queda por fuera, porque un
tramo va de `desde` a `mitad` y el otro de `mitad` a `hasta`. Por debajo del
umbral el tramo ya no se parte y se resuelve con `pasoSegmento`, la misma
función que usa la versión secuencial.

Lo que combina los resultados parciales es la concatenación, que es asociativa
y conserva el orden porque `izquierda` va siempre antes que `derecha`. La
corrección se argumenta en `correccion.md`.

El árbol de llamados para un sistema de ocho partículas con umbral 2, con las
hojas resolviendo en secuencial:

```mermaid
graph TD
    A["0 a 8: parallel"] --> B["0 a 4: parallel"]
    A --> C["4 a 8: parallel"]
    B --> D["0 a 2: pasoSegmento"]
    B --> E["2 a 4: pasoSegmento"]
    C --> F["4 a 6: pasoSegmento"]
    C --> G["6 a 8: pasoSegmento"]
```

## Lo que cuesta repartir

* **Cada partición agrega dos llamados** a `paso` y un llamado a `parallel`.
  Con tramos muy chicos, ese costo puede superar el del cálculo que se reparte.
* **La concatenación copia.** Como no se puede escribir en una celda de un
  arreglo ya creado, cada `izquierda ++ derecha` copia los dos arreglos en uno
  nuevo. La versión secuencial no hace esa copia. Es un costo propio del
  reparto que hay que tener presente al comparar tiempos.
* **El control.** Con el umbral igual al tamaño del sistema, `paso(0, n)` se
  resuelve de una vez con `pasoSegmento` y no se llama a `parallel`: el trabajo
  es el mismo que el de `pasoSecuencial`, y la aceleración medida tiene que ser
  cercana a 1.

## Mediciones

Las mediciones con un millón de partículas se toman en `App.scala` y se
reportan en el punto 5, con la tabla de aceleraciones por tamaño y por umbral,
la fila de control y la comparación con la estimación del punto 4. Esta sección
se completa entonces.