package taller

import org.scalatest.funsuite.AnyFunSuite
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import scala.util.Random

/**
 * Las pruebas no miden tiempos: un tiempo depende de la máquina y de lo que
 * esté corriendo al lado, así que una prueba que lo compare fallaría sin que
 * nada esté mal.
 */
@RunWith(classOf[JUnitRunner])
class SimulacionParticulasTest extends AnyFunSuite {

  val sim = new SimulacionParticulas()
  import sim._

  val eps = 1e-9

  val p = Particula(Vector2(0, 0), Vector2(1, 2), 1.0)

  val s: Sistema = Array(
    Particula(Vector2(0, 0), Vector2(3, 4), 2.0), // norma 5
    Particula(Vector2(4, 0), Vector2(0, 0), 2.0))

  val vacio: Sistema = Array()

  val azar = new Random(7)
  val grande: Sistema = Array.fill(1000)(Particula(
    Vector2(azar.nextDouble() * 10, azar.nextDouble() * 10),
    Vector2(azar.nextDouble() * 2, azar.nextDouble() * 2),
    azar.nextDouble() * 5 + 0.1))

  def cerca(a: Double, b: Double): Boolean = Math.abs(a - b) < 1e-6

  def cerca(a: Vector2, b: Vector2): Boolean =
    Math.abs(a.x - b.x) < 1e-6 && Math.abs(a.y - b.y) < 1e-6

  // Punto 1 -------------------------------------------------------------------

  test("pasoSecuencial: la posición avanza velocidad por dt") {
    assert(pasoSecuencial(Array(p), 2.0).head.pos == Vector2(2.0, 4.0))
  }

  test("pasoSecuencial: con dt cero nada se mueve") {
    assert(pasoSecuencial(Array(p), 0.0).head.pos == Vector2(0.0, 0.0))
  }

  test("pasoSecuencial: la velocidad y la masa no cambian") {
    val despues = pasoSecuencial(Array(p), 3.0).head
    assert(despues.vel == p.vel && despues.masa == p.masa)
  }

  test("pasoSecuencial: sobre el sistema vacío no hay nada que mover") {
    assert(pasoSecuencial(vacio, 1.0).isEmpty)
  }

  test("pasoSegmento: solo calcula el tramo pedido") {
    val tramo = pasoSegmento(s, 1.0, 1, 2)
    assert(tramo.length == 1 && tramo.head.pos == Vector2(4.0, 0.0))
  }

  test("pasoSegmento: el tramo vacío da un sistema vacío") {
    assert(pasoSegmento(s, 1.0, 1, 1).isEmpty)
  }

  test("pasoParalelo: da lo mismo que el secuencial en un caso simple") {
    assert(pasoParalelo(Array(p), 2.0, 1).head.pos == Vector2(2.0, 4.0))
  }

  test("pasoParalelo: coincide con el secuencial para varios umbrales") {
    val umbrales = List(1, 2, 7, 64, 1000, 5000)
    assert(umbrales.forall { u =>
      pasoParalelo(grande, 0.5, u).zip(pasoSecuencial(grande, 0.5))
        .forall { case (a, b) => cerca(a.pos, b.pos) }
    })
  }

  test("pasoParalelo: conserva el orden y el tamaño del sistema") {
    assert(pasoParalelo(grande, 0.5, 32).length == grande.length)
  }

  test("pasoParalelo: con umbral cero no se queda sin fondo") {
    assert(pasoParalelo(Array(p), 1.0, 0).length == 1)
  }

  val tres: Sistema = Array(
    Particula(Vector2(1, 1), Vector2(-1, 2), 1.5),
    Particula(Vector2(-2, 0), Vector2(0.5, 0.5), 4.0),
    Particula(Vector2(3, -3), Vector2(2, -2), 0.5))

  test("mover: con dt negativo la particula retrocede"){
    assert(mover(tres(0), -2.0).pos == Vector2(3.0, -3.0))
  }

  test("pasoSecuencial: cada partícula usa su propia velocidad") {
    val despues = pasoSecuencial(tres, 2.0)
    assert(despues.map(_.pos).toList ==
      List(Vector2(-1.0, 5.0), Vector2(-1.0, 1.0), Vector2(7.0, -7.0)))
  }

  test("pasoSegmento: un tramo del medio respeta los índices") {
    val tramo = pasoSegmento(tres, 1.0, 1, 3)
    assert(tramo.map(_.pos).toList == List(Vector2(-1.5, 0.5), Vector2(5.0, -5.0)))
  }

  test("pasoParalelo: con umbral igual al tamaño coincide con el secuencial"){
    assert(pasoParalelo(tres, 0.5, tres.length).toList == pasoSecuencial(tres, 0.5).toList)
  }

  test("pasoParalelo: con umbral negativo y sistema impar conserva orden y tamaño"){
    assert(pasoParalelo(tres, 1.0, -4).toList == pasoSecuencial(tres, 1.0).toList)
  }

  // Punto 2 -------------------------------------------------------------------

  test("energiaSecuencial: media masa por velocidad al cuadrado") {
    assert(cerca(energiaSecuencial(s), 25.0))
  }

  test("energiaSecuencial: el sistema vacío no tiene energía") {
    assert(energiaSecuencial(vacio) == 0.0)
  }

  test("energiaSecuencial: una partícula quieta no aporta energía") {
    assert(cerca(energiaSecuencial(Array(s(1))), 0.0))
  }

  test("masaTotal: suma las masas") {
    assert(cerca(masaTotal(s), 4.0))
  }

  test("masaTotal: el sistema vacío no tiene masa") {
    assert(masaTotal(vacio) == 0.0)
  }

  test("centroDeMasaSecuencial: el punto medio de dos masas iguales") {
    assert(cerca(centroDeMasaSecuencial(s), Vector2(2.0, 0.0)))
  }

  test("centroDeMasaSecuencial: sin masa el centro es el origen") {
    assert(centroDeMasaSecuencial(vacio) == Vector2(0, 0))
  }

  test("centroDeMasaSecuencial: la masa mayor arrastra el centro") {
    val pesado: Sistema = Array(
      Particula(Vector2(0, 0), Vector2(0, 0), 3.0),
      Particula(Vector2(4, 0), Vector2(0, 0), 1.0))
    assert(cerca(centroDeMasaSecuencial(pesado), Vector2(1.0, 0.0)))
  }

  // Punto 3 -------------------------------------------------------------------

  test("energiaParalela: coincide con la secuencial en el ejemplo") {
    assert(cerca(energiaParalela(s, 1), 25.0))
  }

  test("energiaParalela: coincide con la secuencial para varios umbrales") {
    val esperado = energiaSecuencial(grande)
    assert(List(1, 3, 64, 999, 5000).forall(u => cerca(energiaParalela(grande, u), esperado)))
  }

  test("energiaParalela: sobre el sistema vacío da cero") {
    assert(energiaParalela(vacio, 1) == 0.0)
  }

  test("centroDeMasaParalelo: coincide con el secuencial en el ejemplo") {
    assert(cerca(centroDeMasaParalelo(s, 1), Vector2(2.0, 0.0)))
  }

  test("centroDeMasaParalelo: coincide con el secuencial para varios umbrales") {
    val esperado = centroDeMasaSecuencial(grande)
    assert(List(1, 3, 64, 999, 5000).forall(u => cerca(centroDeMasaParalelo(grande, u), esperado)))
  }

  test("centroDeMasaParalelo: promediar promedios daría otra cosa") {
    // Tres partículas: dos en el origen y una lejos. El promedio de los
    // promedios de las dos mitades no es el centro de masa del conjunto.
    val tres: Sistema = Array(
      Particula(Vector2(0, 0), Vector2(0, 0), 1.0),
      Particula(Vector2(0, 0), Vector2(0, 0), 1.0),
      Particula(Vector2(9, 0), Vector2(0, 0), 1.0))
    assert(cerca(centroDeMasaParalelo(tres, 1), Vector2(3.0, 0.0)))
  }

  test("centroDeMasaParalelo: sin masa el centro es el origen") {
    assert(centroDeMasaParalelo(vacio, 1) == Vector2(0, 0))
  }

  // Punto 4 -------------------------------------------------------------------

  test("trabajo: una operación por partícula") { assert(trabajo(1024) == 1024) }

  test("profundidad: si el umbral cubre todo, no se parte") {
    assert(profundidad(1024, 1024) == 1024)
  }

  test("profundidad: una partición y el tramo que queda") {
    assert(profundidad(1024, 512) == 513)
  }

  test("profundidad: diez particiones y un elemento") {
    assert(profundidad(1024, 1) == 11)
  }

  test("tiempoEstimado: profundidad más trabajo entre procesadores") {
    assert(cerca(tiempoEstimado(1024, 4, 1), 267.0))
  }

  test("tiempoEstimado: con un procesador no hay ganancia") {
    assert(cerca(tiempoEstimado(1024, 1, 1), 11.0 + 1024.0))
  }

  test("profundidad: nunca supera el trabajo") {
    val casos = for (n <- List(1, 7, 64, 1024); u <- List(1, 8, 64)) yield (n, u)
    assert(casos.forall { case (n, u) => profundidad(n, u) <= trabajo(n) })
  }

  // Punto 5 -------------------------------------------------------------------

  test("aceleracion: cuatro veces más rápido da 4.0") {
    assert(cerca(aceleracion(100.0, 25.0), 4.0))
  }

  test("aceleracion: el mismo tiempo da 1.0") {
    assert(cerca(aceleracion(100.0, 100.0), 1.0))
  }

  test("umbralConveniente: sin candidatos devuelve el tamaño del sistema") {
    assert(umbralConveniente(grande, 0.5, Nil) == grande.length)
  }

  test("umbralConveniente: devuelve uno de los candidatos") {
    val candidatos = List(16, 128, 1024)
    assert(candidatos.contains(umbralConveniente(grande, 0.5, candidatos)))
  }
}
