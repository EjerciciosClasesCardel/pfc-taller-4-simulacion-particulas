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

  // Pruebas propias del punto 1: mover, pasoSecuencial, pasoSegmento y pasoParalelo.

  test("punto 1: mover admite un dt negativo") {
    val particula = Particula(Vector2(5, -2), Vector2(2, 4), 3.0)
    assert(mover(particula, -0.5).pos == Vector2(4.0, -4.0))
  }

  test("punto 1: pasoSecuencial conserva el orden de las partículas") {
    val sistema = Array(
      Particula(Vector2(1, 0), Vector2(1, 0), 2.0),
      Particula(Vector2(8, 0), Vector2(-1, 0), 4.0))
    val resultado = pasoSecuencial(sistema, 2.0)
    assert(resultado.map(_.pos).toList == List(Vector2(3, 0), Vector2(6, 0)))
  }

  test("punto 1: pasoSegmento calcula un tramo interior") {
    val sistema = Array.tabulate(5) { i =>
      Particula(Vector2(i, 0), Vector2(0, i), 1.0)
    }
    val resultado = pasoSegmento(sistema, 2.0, 1, 4)
    assert(resultado.map(_.pos).toList ==
      List(Vector2(1, 2), Vector2(2, 4), Vector2(3, 6)))
  }

  test("punto 1: pasoParalelo funciona con umbral cero y cantidad impar") {
    val sistema = Array.tabulate(7) { i =>
      Particula(Vector2(i, i), Vector2(i + 1, -i), 1.0)
    }
    val esperado = pasoSecuencial(sistema, 0.25)
    val obtenido = pasoParalelo(sistema, 0.25, 0)
    assert(obtenido.toList == esperado.toList)
  }

  test("punto 1: pasoParalelo coincide con pasoSecuencial para dt negativo") {
    val sistema = Array.tabulate(13) { i =>
      Particula(Vector2(i * 2, i), Vector2(i, -1), i + 1.0)
    }
    assert(pasoParalelo(sistema, -1.5, -2).toList ==
      pasoSecuencial(sistema, -1.5).toList)
  }

  // Pruebas propias del punto 2: energiaSecuencial, masaTotal y centroDeMasaSecuencial.

  test("punto 2: energiaSecuencial suma velocidades de ambas componentes") {
    val sistema = Array(
      Particula(Vector2(0, 0), Vector2(1, 2), 2.0),
      Particula(Vector2(0, 0), Vector2(0, 3), 1.0))
    assert(cerca(energiaSecuencial(sistema), 9.5))
  }

  test("punto 2: energiaSecuencial ignora la velocidad de una partícula sin masa") {
    val sistema = Array(Particula(Vector2(2, 3), Vector2(20, -8), 0.0))
    assert(energiaSecuencial(sistema) == 0.0)
  }

  test("punto 2: masaTotal suma masas fraccionarias") {
    val sistema = Array(
      Particula(Vector2(0, 0), Vector2(0, 0), 0.25),
      Particula(Vector2(0, 0), Vector2(0, 0), 1.75))
    assert(cerca(masaTotal(sistema), 2.0))
  }

  test("punto 2: centroDeMasaSecuencial pondera dos dimensiones") {
    val sistema = Array(
      Particula(Vector2(-2, 4), Vector2(0, 0), 1.0),
      Particula(Vector2(4, -2), Vector2(0, 0), 2.0))
    assert(cerca(centroDeMasaSecuencial(sistema), Vector2(2.0, 0.0)))
  }

  test("punto 2: centroDeMasaSecuencial devuelve el origen con masa total cero") {
    val sistema = Array(
      Particula(Vector2(10, -5), Vector2(0, 0), 0.0),
      Particula(Vector2(-3, 8), Vector2(1, 1), 0.0))
    assert(centroDeMasaSecuencial(sistema) == Vector2(0, 0))
  }

  // Pruebas propias del punto 3: energiaParalela y centroDeMasaParalelo.

  test("punto 3: energiaParalela coincide con la secuencial en un sistema impar") {
    val sistema = Array.tabulate(9) { i =>
      Particula(Vector2(i, 0), Vector2(i, i + 1), i + 0.5)
    }
    assert(cerca(energiaParalela(sistema, 2), energiaSecuencial(sistema)))
  }

  test("punto 3: energiaParalela trabaja con umbral cero") {
    assert(cerca(energiaParalela(grande.take(31), 0),
      energiaSecuencial(grande.take(31))))
  }

  test("punto 3: centroDeMasaParalelo combina masas desiguales") {
    val sistema = Array(
      Particula(Vector2(-4, 3), Vector2(0, 0), 1.0),
      Particula(Vector2(2, -1), Vector2(0, 0), 5.0),
      Particula(Vector2(8, 2), Vector2(0, 0), 2.0))
    assert(cerca(centroDeMasaParalelo(sistema, 1),
      centroDeMasaSecuencial(sistema)))
  }

  test("punto 3: centroDeMasaParalelo devuelve el origen si toda masa es cero") {
    val sistema = Array(
      Particula(Vector2(5, 9), Vector2(0, 0), 0.0),
      Particula(Vector2(-2, 4), Vector2(0, 0), 0.0))
    assert(centroDeMasaParalelo(sistema, 1) == Vector2(0, 0))
  }

  test("punto 3: centroDeMasaParalelo también coincide sin subdividir") {
    val sistema = Array.tabulate(11) { i =>
      Particula(Vector2(i, -i), Vector2(0, 0), i + 1.0)
    }
    assert(cerca(centroDeMasaParalelo(sistema, sistema.length),
      centroDeMasaSecuencial(sistema)))
  }

  // Pruebas propias del punto 4: trabajo, profundidad y tiempoEstimado.

  test("punto 4: trabajo de un sistema vacío es cero") {
    assert(trabajo(0) == 0)
  }

  test("punto 4: trabajo conserva el número de operaciones") {
    assert(trabajo(257) == 257)
  }

  test("punto 4: profundidad de un tramo vacío es cero") {
    assert(profundidad(0, 0) == 0)
  }

  test("punto 4: profundidad considera mitades de tamaños distintos") {
    assert(profundidad(10, 3) == 5)
  }

  test("punto 4: tiempoEstimado combina profundidad y trabajo") {
    assert(cerca(tiempoEstimado(8, 2, 2), 8.0))
  }

  // Pruebas propias del punto 5: aceleracion y umbralConveniente.

  test("punto 5: aceleracion calcula una razón no entera") {
    assert(cerca(aceleracion(42.0, 14.0), 3.0))
  }

  test("punto 5: aceleracion es cero si el tiempo secuencial es cero") {
    assert(aceleracion(0.0, 5.0) == 0.0)
  }

  test("punto 5: umbralConveniente devuelve cero para candidatos vacíos y sistema vacío") {
    assert(umbralConveniente(vacio, 1.0, Nil) == 0)
  }

  test("punto 5: umbralConveniente puede medir un único candidato") {
    val sistema = Array(
      Particula(Vector2(1, 2), Vector2(3, 4), 1.0),
      Particula(Vector2(5, 6), Vector2(7, 8), 2.0))
    assert(umbralConveniente(sistema, 0.5, List(10)) == 10)
  }

  test("punto 5: umbralConveniente selecciona entre los umbrales entregados") {
    val candidatos = List(1, 4, 20)
    assert(candidatos.contains(umbralConveniente(s.take(1), 0.1, candidatos)))
  }
}
