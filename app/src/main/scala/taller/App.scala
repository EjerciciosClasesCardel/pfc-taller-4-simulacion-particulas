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

    // Parametros del Experiemnto
    val cantidadPar = 1000000
    val dt = 0.01
    val repeticiones = 5

    // Umbrales a commparar
    val umbrales = List(100, 1000, 10000, 100000)

    // Creacion del Sistema
    val sistema: Sistema = Array.tabulate(cantidadPar) { 
      i => Particula(Vector2(i.toDouble, (i % 1000).toDouble), Vector2(1.0,0.5), 1.0)
    }

    println("===========================================")
    println("         SIMULACION DE PARTICULAS          ")
    println("===========================================")

    println(s"Particulas: ${sistema.length}")
    println(s"Nucleos disponibles: ${Runtime.getRuntime.availableProcessors}")
    println(s"Repeticiones por medicion: $repeticiones")
    println(s"Delta de Tiempo: $dt")


    // Medicion secuencial
    println("\nMidiendo version secuencial...")

    val tiempoSecuencial = medir(repeticiones) {
      pasoSecuencial(sistema, dt)
    }

    println(f"Tiempo secuencial: $tiempoSecuencial%.4f ms")

    // Medicones paralelas para cada Umbral
    println("\nMidiendo versiones paralelas...")

    val medicionPar: List[(Int, Double)] = umbrales.map {
      umbral => val tiempo = medir(repeticiones) {
        pasoParalelo(sistema, dt, umbral)
      }
      (umbral, tiempo)
    }

    // Reporte

    println("\n===============================")
    println("           RESULTADOS            ")
    println("=================================")

    println("==============================================================")
    println(f"${"Umbral"}%12s | ${"Tiempo (ms)"}%14s | ${"Aceleracion"}%14s")
    println("==============================================================")

    medicionPar.foreach {
      case (umbral, tiempo) => val aceleracionObt = aceleracion(tiempoSecuencial, tiempo)

      println(f"$umbral%12d | $tiempo%14.4f | $aceleracionObt%14.4f")
    }

    // Identificar el mejor Umbral
    val mejorUmbral = medicionPar.reduceLeft {
      (mejor, actual) => 
        if (actual._2 < mejor._2) {
          actual 
        } else {
          mejor
        }
    }._1

    println("============================================================")
    println(s"umbral mas convceniente: $mejorUmbral")
    println("======================================")
  }
}
