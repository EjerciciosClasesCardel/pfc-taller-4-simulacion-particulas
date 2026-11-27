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

    val dt = 0.5
    val repeticiones = 5
    val tamanos = List(1000000, 2000000)
    val umbrales = List(1000, 10000, 100000)

    println(s"Núcleos disponibles: ${Runtime.getRuntime.availableProcessors}")
    println("n | umbral | secuencial (ms) | paralelo (ms) | aceleración")

    tamanos.foreach { n =>
      val sistema: Sistema = Array.tabulate(n)(i =>
        Particula(Vector2(i, -i), Vector2(1, 2), 1.0 + i % 3))
      val tSec = medir(repeticiones)(pasoSecuencial(sistema, dt))
      // El último candidato es el control: umbral = n, no reparte nada.
      (umbrales :+ n).foreach { u =>
        val tPar = medir(repeticiones)(pasoParalelo(sistema, dt, u))
        println(f"$n%9d | $u%9d | $tSec%10.2f | $tPar%10.2f | ${aceleracion(tSec, tPar)}%6.2f")
      }
    }
  }
}