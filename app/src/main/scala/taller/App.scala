package taller

/**
 * El punto 5: mide, sobre sistemas de uno a cuatro millones de partículas, el
 * tiempo de `pasoSecuencial` y el de `pasoParalelo` con varios umbrales, y
 * reporta la aceleración de cada uno.
 *
 * Los argumentos son opcionales: el primero es el número de repeticiones por
 * medición (10 por omisión) y el resto son los tamaños del sistema.
 */
object App {

  def main(args: Array[String]): Unit = {
    val sim = new SimulacionParticulas()
    import sim._

    val sistema: Sistema = Array(
      Particula(Vector2(0, 0), Vector2(1, 1), 1.0),
      Particula(Vector2(2, 0), Vector2(0, 1), 2.0))

    println(s"Núcleos disponibles: ${Runtime.getRuntime.availableProcessors}")
    println(s"Memoria máxima de la JVM (MB): ${Runtime.getRuntime.maxMemory / (1024 * 1024)}")
    println(pasoSecuencial(sistema, 0.5).mkString(", "))
    println()

    // Medición del punto 5
    val dt = 0.01
    val nucleos = Runtime.getRuntime.availableProcessors
    val repeticiones = args.headOption.map(_.toInt).getOrElse(10)
    val tamanos =
      if (args.length > 1) args.toList.tail.map(_.toInt)
      else List(1000000, 2000000, 4000000)

    // Mismo n, mismo sistema: no hay azar
    def crear(n: Int): Sistema =
      Array.tabulate(n)(i => Particula(
        Vector2((i % 1000).toDouble, (i / 1000).toDouble),
        Vector2((i % 7) - 3.0, (i % 5) - 2.0),
        1.0 + (i % 3)))

    def fraccionSerial(aceleracionMedida: Double, p: Int): Double =
      if (p <= 1) 1.0
      else (1.0 / aceleracionMedida - 1.0 / p) / (1.0 - 1.0 / p)

    def fmt(x: Double): String = f"$x%.2f"

    println("| n | umbral | T secuencial (ms) | T paralelo (ms) | aceleración | aceleración estimada | fracción serial |")
    println("|---|---|---|---|---|---|---|")

    tamanos.foreach { n =>
      val sistemaGrande = crear(n)
      val umbrales = List(n, n / 2, n / 8, n / 64, n / 512).distinct

      // Calentamiento que se descarta: la primera vez que se pide memoria para
      // un sistema de este tamaño la máquina tarda más (reserva las páginas y
      // optimiza el código), y esa demora caería sobre la primera fila.
      medir(repeticiones)(pasoSecuencial(sistemaGrande, dt))
      medir(repeticiones)(pasoParalelo(sistemaGrande, dt, n / 8))

      umbrales.foreach { u =>
        // El secuencial se mide justo antes de cada umbral: así los dos
        // tiempos comparten las condiciones de la máquina (memoria, recolector
        // de basura, temperatura) y la fila de control no depende de una sola
        // medición hecha antes.
        val tSec = medir(repeticiones)(pasoSecuencial(sistemaGrande, dt))
        val tPar = medir(repeticiones)(pasoParalelo(sistemaGrande, dt, u))
        val medida = aceleracion(tSec, tPar)
        val estimado = trabajo(n).toDouble / tiempoEstimado(n, nucleos, u)
        println(s"| $n | $u | ${fmt(tSec)} | ${fmt(tPar)} | ${fmt(medida)} | ${fmt(estimado)} | ${fmt(fraccionSerial(medida, nucleos))} |")
      }
    }

    // Mediciones punto 3

    println()
    println("| Función | n | umbral | T sec (ms) | T par (ms) | aceleración |")
    println("|---|---|---|---|---|---|")

    tamanos.foreach { n =>
      val s = crear(n)
      val umbrales = List(n, n / 2, n / 8, n / 64, n / 512).distinct

      umbrales.foreach { u =>
        val tES = medir(repeticiones)(energiaSecuencial(s))
        val tEP = medir(repeticiones)(energiaParalela(s, u))
        val tCS = medir(repeticiones)(centroDeMasaSecuencial(s))
        val tCP = medir(repeticiones)(centroDeMasaParalelo(s, u))

        println(s"| Energía | $n | $u | ${fmt(tES)} | ${fmt(tEP)} | ${fmt(aceleracion(tES, tEP))} |")
        println(s"| Centro de masa | $n | $u | ${fmt(tCS)} | ${fmt(tCP)} | ${fmt(aceleracion(tCS, tCP))} |")
      }
    }
  }
}
