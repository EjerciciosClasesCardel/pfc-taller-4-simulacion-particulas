package taller

import org.scalameter._

/**
 * Mide pasoSecuencial y pasoParalelo sobre sistemas de uno y dos millones de
 * partículas, con varios umbrales, y compara la aceleración medida con la que
 * estima el punto 4. El último umbral de cada tamaño es n: no reparte nada y
 * es la fila de control, donde la aceleración medida debe dar cerca de 1.
 */
object App {

  val sim = new SimulacionParticulas()
  import sim._

  val dt = 0.5
  val rondas = 5

  def crear(n: Int): Sistema =
    Array.tabulate(n)(i => Particula(
      Vector2((i % 1000).toDouble, (i / 1000).toDouble),
      Vector2((i % 7) - 3.0, (i % 5) - 2.0),
      1.0 + (i % 3)))

  // Una medición de ScalaMeter, en ms, empezando con la memoria limpia.
  def tiempo(cuerpo: => Any): Double = {
    System.gc()

    medir(1)(cuerpo)

  }

  // Alterna secuencial y paralelo en cada ronda, para que el ruido de la
  // máquina afecte a los dos por igual, y devuelve la mediana de cada uno.
  def medirPar(secuencial: => Any, paralelo: => Any): (Double, Double) = {
    def mediana(xs: List[Double]): Double = xs.sorted.apply(xs.length / 2)
    val pares = List.fill(rondas)((tiempo(secuencial), tiempo(paralelo)))
    (mediana(pares.map(_._1)), mediana(pares.map(_._2)))
  }

  // Amdahl: S = 1 / (f + (1 - f) / p)  =>  f = (p / S - 1) / (p - 1)
  def fraccionSerial(a: Double, p: Int): Double =
    (p / a - 1.0) / (p - 1.0)

  def main(args: Array[String]): Unit = {
    val p = Runtime.getRuntime.availableProcessors
    println(s"Núcleos disponibles: $p")
    println("| n | umbral | seq (ms) | par (ms) | aceleración | estimada (punto 4) | fracción serial |")
    println("|---:|---:|---:|---:|---:|---:|---:|")

    List(1000000, 2000000).foreach { n =>
      val s = crear(n)
      List(1000, 10000, 100000, n / 8, n).foreach { u =>
        val (tSeq, tPar) = medirPar(pasoSecuencial(s, dt), pasoParalelo(s, dt, u))
        val a = aceleracion(tSeq, tPar)
        val estimada = tiempoEstimado(n, 1, u) / tiempoEstimado(n, p, u)
        val fs = if (u == n) "-" else f"${fraccionSerial(a, p)}%.3f"
        val marca = if (u == n) s"$u (control)" else u.toString
        println(f"| $n | $marca | $tSeq%.2f | $tPar%.2f | $a%.2f | $estimada%.2f | $fs |")
      }
    }
  }
}

