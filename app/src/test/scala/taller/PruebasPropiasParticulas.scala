package taller

import org.scalatest.funsuite.AnyFunSuite
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import scala.util.Random

@RunWith(classOf[JUnitRunner])
class PruebasPropiasParticulas extends AnyFunSuite {
  val sim = new SimulacionParticulas()
  import sim._

  //Punto 1 ------------------------------------------------------------------------------------------

  test("pasoSecuencial: Mueve varias partículas con diferentes velocidades") {
    val particulasMovimiento = Array(
      Particula(Vector2(2.5, 3.5), Vector2(1.5, -2.5), 4.5),
      Particula(Vector2(-3.5, 6.5), Vector2(-1.5, 3.5), 2.5),
      Particula(Vector2(7.5, -4.5), Vector2(2.5, 1.5), 6.5)
    )

    val resultadoMovimiento = pasoSecuencial(particulasMovimiento, 2.5)

    assert(resultadoMovimiento(0).pos == Vector2(6.25, -2.75))
    assert(resultadoMovimiento(1).pos == Vector2(-7.25, 15.25))
    assert(resultadoMovimiento(2).pos == Vector2(13.75, -0.75))
  }

  test("pasoSecuencial: retrocede las partículas con un dt negativo") {
    val particulasRetroceso = Array(
      Particula(Vector2(8.5, -6.5), Vector2(2.5, -1.5), 3.5),
      Particula(Vector2(-9.5, 4.5), Vector2(-3.5, 2.5), 7.5)
    )

    val resultadoRetroceso = pasoSecuencial(particulasRetroceso, -1.5)

    assert(resultadoRetroceso(0).pos == Vector2(4.75, -4.25))
    assert(resultadoRetroceso(1).pos == Vector2(-4.25, 0.75))
  }

  test("pasoSegmento: Procesa únicamente un segmento intermedio") {
    val sistemaSegmentado = Array(
      Particula(Vector2(1.5, 2.5), Vector2(3.5, 1.5), 2.5),
      Particula(Vector2(4.5, 5.5), Vector2(-2.5, 3.5), 5.5),
      Particula(Vector2(7.5, 8.5), Vector2(1.5, -3.5), 4.5),
      Particula(Vector2(10.5, 11.5), Vector2(2.5, 2.5), 8.5),
      Particula(Vector2(13.5, 14.5), Vector2(-1.5, -2.5), 6.5)
    )

    val resultadoSegmento = pasoSegmento(sistemaSegmentado, 2.5, 1, 4)

    assert(resultadoSegmento.length == 3)
    assert(resultadoSegmento(0).pos == Vector2(-1.75, 14.25))
    assert(resultadoSegmento(1).pos == Vector2(11.25, -0.25))
    assert(resultadoSegmento(2).pos == Vector2(16.75, 17.75))
  }

  test("pasoParalelo: Conserva todas las partículas en un sistema impar") {
    val sistemaImpar = Array(
      Particula(Vector2(2.5, 4.5), Vector2(1.5, 2.5), 3.5),
      Particula(Vector2(5.5, 7.5), Vector2(-2.5, 1.5), 4.5),
      Particula(Vector2(8.5, 10.5), Vector2(3.5, -1.5), 5.5),
      Particula(Vector2(11.5, 13.5), Vector2(-1.5, -3.5), 6.5),
      Particula(Vector2(14.5, 16.5), Vector2(2.5, 3.5), 7.5),
      Particula(Vector2(17.5, 19.5), Vector2(-3.5, 2.5), 8.5),
      Particula(Vector2(20.5, 22.5), Vector2(1.5, -2.5), 9.5)
    )

    val resultadoParalelo = pasoParalelo(sistemaImpar, 1.75, 2)
    val resultadoSecuencial = pasoSecuencial(sistemaImpar, 1.75)

    assert(resultadoParalelo.length == sistemaImpar.length)

    assert(resultadoParalelo.zip(resultadoSecuencial).forall {
      case (a, b) =>
        a.pos == b.pos && a.vel == b.vel && a.masa == b.masa
    })
  }

  test("pasoParalelo: Conserva los atributos originales de cada partícula") {
    val sistemaAtributos = Array(
      Particula(Vector2(-2.5, 3.5), Vector2(4.5, -1.5), 11.5),
      Particula(Vector2(6.5, -7.5), Vector2(-3.5, 5.5), 13.5),
      Particula(Vector2(9.5, 2.5), Vector2(1.5, 4.5), 15.5),
      Particula(Vector2(-8.5, -9.5), Vector2(2.5, -4.5), 17.5)
    )

    val resultadoAtributos = pasoParalelo(sistemaAtributos, 3.5, 1)

    assert(resultadoAtributos.zip(sistemaAtributos).forall {
      case (nueva, original) =>
        nueva.vel == original.vel &&
          nueva.masa == original.masa
    })
  }
  //Punto 2 ------------------------------------------------------------------------------------------

  test("energiaSecuencial: Calcula correctamente energías con velocidades negativas") {
    val particulasEnergia = Array(
      Particula(Vector2(3.5, 4.5), Vector2(-3.5, 2.5), 4.5),
      Particula(Vector2(-5.5, 7.5), Vector2(1.5, -4.5), 6.5),
      Particula(Vector2(8.5, -2.5), Vector2(-2.5, -3.5), 2.5)
    )

    val energiaCalculada = energiaSecuencial(particulasEnergia)

    val energiaEsperada =
      0.5 * 4.5 * (3.5 * 3.5 + 2.5 * 2.5) +
        0.5 * 6.5 * (1.5 * 1.5 + 4.5 * 4.5) +
        0.5 * 2.5 * (2.5 * 2.5 + 3.5 * 3.5)

    assert(Math.abs(energiaCalculada - energiaEsperada) < 1e-6)
  }

  test("masaTotal: Suma correctamente masas decimales de varias partículas") {
    val particulasMasas = Array(
      Particula(Vector2(2.5, 3.5), Vector2(1.5, 2.5), 12.75),
      Particula(Vector2(4.5, 5.5), Vector2(3.5, 4.5), 18.25),
      Particula(Vector2(6.5, 7.5), Vector2(5.5, 6.5), 9.75),
      Particula(Vector2(8.5, 9.5), Vector2(7.5, 8.5), 14.25)
    )

    assert(Math.abs(masaTotal(particulasMasas) - 55.0) < 1e-6)
  }

  test("centroDeMasaSecuencial: Calcula el centro ponderado de tres partículas") {
    val particulasCentro = Array(
      Particula(Vector2(2.5, 3.5), Vector2(0.0, 0.0), 2.0),
      Particula(Vector2(8.5, 5.5), Vector2(0.0, 0.0), 4.0),
      Particula(Vector2(-3.5, 9.5), Vector2(0.0, 0.0), 6.0)
    )

    val centroCalculado = centroDeMasaSecuencial(particulasCentro)

    assert(Math.abs(centroCalculado.x - 1.5) < 1e-6)
    assert(Math.abs(centroCalculado.y - 7.1666667) < 1e-6)
  }

  test("centroDeMasaSecuencial: Considera correctamente posiciones negativas") {
    val particulasMixtas = Array(
      Particula(Vector2(-7.5, 4.5), Vector2(0.0, 0.0), 3.0),
      Particula(Vector2(5.5, -8.5), Vector2(0.0, 0.0), 5.0),
      Particula(Vector2(9.5, 6.5), Vector2(0.0, 0.0), 2.0)
    )

    val centroMixto = centroDeMasaSecuencial(particulasMixtas)

    assert(Math.abs(centroMixto.x - 2.4) < 1e-6)
    assert(Math.abs(centroMixto.y + 1.6) < 1e-6)
  }

  test("centroDeMasaSecuencial: Verifica el equilibrio de las posiciones ponderadas") {
    val particulasEquilibrio = Array(
      Particula(Vector2(3.5, 2.5), Vector2(1.5, 1.5), 4.0),
      Particula(Vector2(7.5, 6.5), Vector2(-1.5, 2.5), 8.0),
      Particula(Vector2(-4.5, 9.5), Vector2(2.5, -1.5), 12.0),
      Particula(Vector2(5.5, -3.5), Vector2(-2.5, -2.5), 16.0)
    )

    val centroEquilibrio = centroDeMasaSecuencial(particulasEquilibrio)
    val masaEquilibrio = masaTotal(particulasEquilibrio)

    val sumaX = particulasEquilibrio.map(p => p.pos.x * p.masa).sum
    val sumaY = particulasEquilibrio.map(p => p.pos.y * p.masa).sum

    assert(Math.abs(centroEquilibrio.x * masaEquilibrio - sumaX) < 1e-6)
    assert(Math.abs(centroEquilibrio.y * masaEquilibrio - sumaY) < 1e-6)
  }

  //Punto 3 ------------------------------------------------------------------------------------------

  test("Punto 3: la energía paralela suma los aportes de tres partículas") {
    val sistema: Sistema = Array(
      Particula(Vector2(0, 0), Vector2(3, 4), 2.0),
      Particula(Vector2(1, 0), Vector2(-2, 0), 3.0),
      Particula(Vector2(2, 0), Vector2(0, -3), 4.0)
    )

    assert(math.abs(energiaParalela(sistema, 2) - 49.0) < 1e-6)
  }

  test("Punto 3: una partícula sin masa aporta cero energía aunque se mueva") {
    val sistema: Sistema = Array(
      Particula(Vector2(0, 0), Vector2(6, 8), 0.0),
      Particula(Vector2(1, 0), Vector2(0, -4), 0.5)
    )

    assert(math.abs(energiaParalela(sistema, 1) - 4.0) < 1e-6)
  }

  test("Punto 3: el centro de masa pondera ambas coordenadas con masas distintas") {
    val sistema: Sistema = Array(
      Particula(Vector2(-3, 2), Vector2(0, 0), 2.0),
      Particula(Vector2(2, -4), Vector2(0, 0), 1.0),
      Particula(Vector2(4, 5), Vector2(0, 0), 5.0)
    )

    val centro = centroDeMasaParalelo(sistema, 2)

    assert(math.abs(centro.x - 2.0) < 1e-6)
    assert(math.abs(centro.y - 3.125) < 1e-6)
  }

  test("Punto 3: un sistema con partículas de masa cero tiene centro en el origen") {
    val sistema: Sistema = Array(
      Particula(Vector2(5, -2), Vector2(0, 0), 0.0),
      Particula(Vector2(-7, 4), Vector2(0, 0), 0.0)
    )

    assert(centroDeMasaParalelo(sistema, 1) == Vector2(0, 0))
  }

  test("Punto 3: una mitad sin masa no desplaza el centro de la otra mitad") {
    val sistema: Sistema = Array(
      Particula(Vector2(100, 100), Vector2(0, 0), 0.0),
      Particula(Vector2(-100, 100), Vector2(0, 0), 0.0),
      Particula(Vector2(2, 4), Vector2(0, 0), 1.0),
      Particula(Vector2(6, 0), Vector2(0, 0), 3.0)
    )

    val centro = centroDeMasaParalelo(sistema, 2)

    assert(math.abs(centro.x - 5.0) < 1e-6)
    assert(math.abs(centro.y - 1.0) < 1e-6)
  }

  //Punto 4 ------------------------------------------------------------------------------------------

  test("Punto 4: un sistema vacío tiene trabajo y profundidad cero") {

    assert(trabajo(0) == 0)
    assert(profundidad(0, 4) == 0)
  }

  test("Punto 4: el tramo menor puede determinar la profundidad") {

    assert(profundidad(17, 8) == 9)
  }

  test("Punto 4: el umbral cero se trata como uno") {

    assert(profundidad(9, 0) == 5)
  }

  test("Punto 4: la división se detiene cuando el tramo alcanza el umbral") {

    assert(profundidad(12, 3) == 5)
  }

  test("Punto 4: el tiempo estimado conserva la parte decimal") {

    assert(math.abs(tiempoEstimado(5, 2, 2) - 6.5) < 1e-6)
  }


  //Punto 5 ------------------------------------------------------------------------------------------

  test("aceleracion: Calcula una aceleración con tiempos diferentes") {
    assert(Math.abs(aceleracion(375.0, 25.0) - 15.0) < 1e-6)
  }

  test("aceleracion: Obtiene un valor inferior a uno cuando el paralelo es más lento") {
    val resultadoAceleracion = aceleracion(45.0, 180.0)

    assert(Math.abs(resultadoAceleracion - 0.25) < 1e-6)
  }

  test("umbralConveniente: Selecciona un umbral válido entre varios candidatos") {
    val sistemaUmbrales = Array.fill(350)(
      Particula(
        Vector2(2.5, -3.5),
        Vector2(1.5, 4.5),
        6.5
      )
    )

    val candidatosUmbral = List(3, 9, 27, 81, 243)

    val resultadoUmbral =
      umbralConveniente(sistemaUmbrales, 1.25, candidatosUmbral)

    assert(candidatosUmbral.contains(resultadoUmbral))
  }

  test("medir: Obtiene un tiempo positivo al ejecutar una operación") {
    val resultadoMedicion = medir(5) {
      (1 to 1500).map(numero => numero * numero).sum
    }

    assert(resultadoMedicion > 0.0)
  }

  test("umbralConveniente: Devuelve el único candidato disponible") {
    val sistemaUnico = Array(
      Particula(Vector2(3.5, 5.5), Vector2(2.5, -1.5), 4.5),
      Particula(Vector2(7.5, 9.5), Vector2(-3.5, 2.5), 8.5),
      Particula(Vector2(-5.5, 6.5), Vector2(1.5, -4.5), 12.5),
      Particula(Vector2(11.5, -8.5), Vector2(3.5, 5.5), 16.5),
      Particula(Vector2(13.5, 15.5), Vector2(-2.5, -3.5), 20.5),
      Particula(Vector2(-9.5, -11.5), Vector2(4.5, 1.5), 24.5)
    )

    val candidatosUnicos = List(5)

    assert(
      umbralConveniente(sistemaUnico, 2.75, candidatosUnicos) == 5
    )
  }

}