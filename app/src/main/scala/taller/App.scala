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
    println()

    // Medición del punto 5
    val dt = 0.01
    val nucleos = Runtime.getRuntime.availableProcessors
    val tamanos = List(1000000, 1500000, 2000000)
    val repeticiones = 5

    // Mismo n, mismo sistema: no hay azar

    def crear(n: Int): Sistema =
      Array.tabulate(n)(i => Particula(
        Vector2((i % 1000).toDouble, (i / 1000).toDouble),
        Vector2((i % 7) - 3.0, (i % 5) - 2.0),
        1.0 + (i % 3)))

    def fraccionSerial(aceleracionMedida: Double, p: Int): Double =
       if (p <= 1) 1.0
       else (1.0 / aceleracionMedida - 1.0 / p) /  (1.0 - 1.0 / p)

    def fmt(x: Double): String = f"$x%.2f"

    println("| n | umbral | T secuencial (ms)  | T paralelo (ms) | aceleración | aceleración estimada | fracción serial |")
    println("|---|---|---|---|---|---|---|")

    tamanos.foreach{ n =>
      val sistemaGrande = crear(n)
      val umbrales = List(n, n/2, n/8, n/64, n/512).distinct
      val tSec = medir(repeticiones)(pasoSecuencial(sistemaGrande, dt))

      umbrales.foreach { u =>
        val tPar = medir(repeticiones)(pasoParalelo(sistemaGrande, dt, u))
        val medida = aceleracion(tSec, tPar)
        val estimado = trabajo(n).toDouble / tiempoEstimado(n, nucleos, u)
        println(s"| $n | $u | ${fmt(tSec)} | ${fmt(tPar)} | ${fmt(medida)} | ${fmt(estimado)} | ${fmt(fraccionSerial(medida, nucleos))} |")

      }
    }
  }
}
