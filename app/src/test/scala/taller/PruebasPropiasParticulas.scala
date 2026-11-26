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
  //Punto 2 ------------------------------------------------------------------------------------------


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


}