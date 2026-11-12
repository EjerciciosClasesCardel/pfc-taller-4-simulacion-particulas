package taller

/**
 * El punto 5 pide medir aquí, sobre un sistema de al menos un millón de
 * partículas, el tiempo de `pasoSecuencial` y el de `pasoParalelo` con varios
 * umbrales, y reportar la aceleración de cada uno.
 *
 * Lo que hay ahora es el arranque mínimo sobre dos partículas.
 */
object App {

  def main(args: Array[String]): Unit = {
    val sim = new SimulacionParticulas()
    import sim._

    val sistema: Sistema = Array(
      Particula(Vector2(0, 0), Vector2(1, 1), 1.0),
      Particula(Vector2(2, 0), Vector2(0, 1), 2.0))

    println(s"Núcleos disponibles: ${Runtime.getRuntime.availableProcessors}")
    println(pasoSecuencial(sistema, 0.5).mkString(", "))
  }
}
