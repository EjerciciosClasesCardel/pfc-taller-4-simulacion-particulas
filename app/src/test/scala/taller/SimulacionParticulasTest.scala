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

  // Punto 2

  def particula(x: Double, y: Double, vx: Double, vy: Double, m: Double): Particula =
    Particula(Vector2(x, y), Vector2(vx, vy), m)

  test("energiaSecuencial: velocidad (6, 8) de norma 10 y masa 3 da 150") {
    assert(cerca(energiaSecuencial(Array(particula(0, 0, 6, 8, 3.0))), 150.0))
  }

  test("energiaSecuencial: suma las energías de partículas distintas") {
    val sistema: Sistema = Array(particula(0, 0, 1, 0, 4.0), particula(5, 5, 0, 2, 2.0))
    assert(cerca(energiaSecuencial(sistema), 6.0))
  }

  test("energiaSecuencial: no depende de la posición") {
    val enElOrigen = energiaSecuencial(Array(particula(0, 0, 5, 12, 2.0)))
    val lejos = energiaSecuencial(Array(particula(100, -50, 5, 12, 2.0)))
    assert(cerca(enElOrigen, 169.0) && cerca(lejos, 169.0))
  }

  test("energiaSecuencial: el orden de las partículas no cambia el resultado") {
    val a = particula(1, 1, 3, 4, 2.0)
    val b = particula(2, 0, -1, 2, 5.0)
    val c = particula(0, 7, 0, -6, 1.5)
    assert(cerca(energiaSecuencial(Array(a, b, c)), energiaSecuencial(Array(c, b, a))))
  }

  test("masaTotal: suma masas decimales") {
    val sistema: Sistema = Array(
      particula(0, 0, 0, 0, 0.5), particula(1, 1, 0, 0, 1.5), particula(2, 2, 0, 0, 2.25)
    )
    assert(cerca(masaTotal(sistema), 4.25))
  }

  test("masaTotal: con una sola partícula es su masa") {
    assert(cerca(masaTotal(Array(particula(3, 4, 5, 6, 7.5))), 7.5))
  }

  test("centroDeMasaSecuencial: con una sola partícula es su posición") {
    assert(cerca(centroDeMasaSecuencial(Array(particula(3, -2, 1, 1, 5.0))), Vector2(3.0, -2.0)))
  }

  test("centroDeMasaSecuencial: tres partículas en el plano con masas distintas") {
    val sistema: Sistema = Array(
      particula(0, 0, 0, 0, 1.0), particula(4, 0, 0, 0, 1.0), particula(0, 4, 0, 0, 2.0)
    )
    assert(cerca(centroDeMasaSecuencial(sistema), Vector2(1.0, 2.0)))
  }

  test("centroDeMasaSecuencial: una partícula sin masa no mueve el centro") {
    val sistema: Sistema = Array(
      particula(0, 0, 0, 0, 2.0), particula(100, 100, 0, 0, 0.0), particula(4, 0, 0, 0, 2.0)
    )
    assert(cerca(centroDeMasaSecuencial(sistema), Vector2(2.0, 0.0)))
  }

  test("centroDeMasaSecuencial: con posiciones negativas") {
    val sistema: Sistema = Array(particula(-2, -2, 0, 0, 1.0), particula(2, 2, 0, 0, 3.0))
    assert(cerca(centroDeMasaSecuencial(sistema), Vector2(1.0, 1.0)))
  }

  test("centroDeMasaSecuencial: un sistema no vacío de masa cero tiene el centro en el origen") {
    assert(centroDeMasaSecuencial(Array(particula(5, 5, 1, 1, 0.0))) == Vector2(0.0, 0.0))
  }

  // Punto 4

  test("trabajo: sistema vacío y una partícula") {
    assert(trabajo(0) == 0 && trabajo(1) == 1)
  }

  test("trabajo: un millón de partículas son un millón de operaciones") {
    assert(trabajo(1000000) == 1000000)
  }

  test("profundidad: ocho partículas con umbral 1 son tres particiones y un elemento") {
    assert(profundidad(8, 1) == 4)
  }

  test("profundidad: dos particiones y un tramo de 256") {
    assert(profundidad(1024, 256) == 258)
  }

  test("profundidad: tamaño impar, 5 con umbral 2") {
    assert(profundidad(5, 2) == 4)
  }

  test("profundidad: la mitad menor puede ser la que domina, 11 con umbral 5") {
    assert(profundidad(11, 5) == 6)
  }

  test("profundidad: un umbral cero se comporta como uno") {
    assert(profundidad(8, 0) == profundidad(8, 1))
  }

  test("tiempoEstimado: 1024 partículas, 8 procesadores y umbral 128") {
    assert(cerca(tiempoEstimado(1024, 8, 128), 259.0))
  }

  test("tiempoEstimado: con tantos procesadores como partículas queda la profundidad más uno") {
    assert(cerca(tiempoEstimado(1024, 1024, 1), 12.0))
  }

  test("tiempoEstimado: más procesadores lo reducen") {
    assert(tiempoEstimado(4096, 2, 64) > tiempoEstimado(4096, 8, 64))
  }

  test("tiempoEstimado: nunca baja de la profundidad ni del trabajo entre procesadores") {
    val casos = List(10, 100, 1000).flatMap(n =>
      List(1, 4, 16).flatMap(p => List(1, 10, 100).map(u => (n, p, u))))
    assert(casos.forall { case (n, p, u) =>
      tiempoEstimado(n, p, u) >= profundidad(n, u) &&
        tiempoEstimado(n, p, u) >= trabajo(n).toDouble / p
    })

  }

}
