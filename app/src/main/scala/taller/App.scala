package taller

object App {

  def main(args: Array[String]): Unit = {
    val sim = new SimulacionParticulas()
    import sim._

    println(s"Núcleos disponibles: ${Runtime.getRuntime.availableProcessors}")
    println("particulas,umbral,secuencial_ms,paralelo_ms,aceleracion")

    List(1000000, 1500000).foreach { n =>
      val sistema: Sistema = Array.tabulate(n) { i =>
        Particula(
          Vector2(i.toDouble, (i % 100).toDouble),
          Vector2((i % 5).toDouble, (i % 7).toDouble),
          1.0 + (i % 3))
      }
      val dt = 0.5
      val tiempoSecuencial = medir(5) { pasoSecuencial(sistema, dt) }
      val umbrales = List(n, n / 8, n / 64, n / 1024)

      umbrales.foreach { umbral =>
        val tiempoParalelo = medir(5) { pasoParalelo(sistema, dt, umbral) }
        println(
          s"$n,$umbral,$tiempoSecuencial,$tiempoParalelo," +
            s"${aceleracion(tiempoSecuencial, tiempoParalelo)}")
      }
    }
  }
}
