package taller

import org.scalatest.funsuite.AnyFunSuite
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import scala.util.Random

@RunWith(classOf[JUnitRunner])
class SimulacionParticulasExtraTest extends AnyFunSuite {

  val sim = new SimulacionParticulas()
  import sim._

  def cerca(a: Double, b: Double): Boolean = Math.abs(a - b) < 1e-6

  def cerca(a: Vector2, b: Vector2): Boolean =
    Math.abs(a.x - b.x) < 1e-6 && Math.abs(a.y - b.y) < 1e-6

  def iguales(a: Sistema, b: Sistema): Boolean = a.sameElements(b)

  // Cuatro partículas con posiciones, velocidades y masas distintas.
  val cuatro: Sistema = Array(
    Particula(Vector2(0, 0), Vector2(1, 0), 1.0),
    Particula(Vector2(1, 1), Vector2(-1, 2), 2.0),
    Particula(Vector2(5, -2), Vector2(0.5, 0.5), 3.0),
    Particula(Vector2(-3, 4), Vector2(2, -2), 4.0))

  val azar = new Random(11)
  val aleatorio: Sistema = Array.fill(1001)(Particula(
    Vector2(azar.nextDouble() * 20 - 10, azar.nextDouble() * 20 - 10),
    Vector2(azar.nextDouble() * 4 - 2, azar.nextDouble() * 4 - 2),
    azar.nextDouble() * 9 + 0.5))

  // Cien mil partículas: la i-ésima en (i, -i) con velocidad (1, 2).
  val enormes: Sistema = Array.tabulate(100000)(i =>
    Particula(Vector2(i, -i), Vector2(1, 2), 1.0))

  // El sistema de cinco partículas del informe de proceso (e5).
  val cinco: Sistema = Array(
    Particula(Vector2(0, 0), Vector2(1, 0), 1.0),
    Particula(Vector2(1, 1), Vector2(-1, 2), 2.0),
    Particula(Vector2(5, -2), Vector2(0.5, 0.5), 3.0),
    Particula(Vector2(-3, 4), Vector2(2, -2), 4.0),
    Particula(Vector2(2, 2), Vector2(0, -1), 2.0))

  // Sistema aleatorio propio de los puntos 3 y 4: 777 partículas (impar),
  // otra semilla, para no duplicar los casos de `aleatorio`.
  val azar2 = new Random(2026)
  val disperso: Sistema = Array.fill(777)(Particula(
    Vector2(azar2.nextDouble() * 40 - 20, azar2.nextDouble() * 40 - 20),
    Vector2(azar2.nextDouble() * 6 - 3, azar2.nextDouble() * 6 - 3),
    azar2.nextDouble() * 8 + 0.5))

  // Punto 1 -------------------------------------------------------------------

  test("mover: con dt negativo la partícula retrocede") {
    val q = Particula(Vector2(1, 1), Vector2(2, -3), 1.0)
    assert(mover(q, -0.5) == Particula(Vector2(0.0, 2.5), Vector2(2, -3), 1.0))
  }

  test("pasoSecuencial: cada partícula usa su propia velocidad") {
    val esperado = List(Vector2(2.0, 0.0), Vector2(-1.0, 5.0), Vector2(6.0, -1.0), Vector2(1.0, 0.0))
    assert(pasoSecuencial(cuatro, 2.0).toList.map(_.pos) == esperado)
  }

  test("pasoSecuencial: no modifica el sistema original") {
    val copia = cuatro.clone()
    pasoSecuencial(cuatro, 2.0)
    assert(iguales(cuatro, copia))
  }

  test("pasoSecuencial: dos pasos seguidos equivalen a un paso de la suma") {
    val dosPasos = pasoSecuencial(pasoSecuencial(aleatorio, 0.25), 0.75)
    val unPaso = pasoSecuencial(aleatorio, 1.0)
    assert(dosPasos.zip(unPaso).forall { case (a, b) => cerca(a.pos, b.pos) })
  }

  test("pasoSegmento: el tramo inicial entrega solo las primeras partículas") {
    val tramo = pasoSegmento(cuatro, 2.0, 0, 2)
    assert(tramo.toList.map(_.pos) == List(Vector2(2.0, 0.0), Vector2(-1.0, 5.0)))
  }

  test("pasoSegmento: un tramo del medio respeta el orden y los extremos") {
    val tramo = pasoSegmento(cuatro, 2.0, 1, 3)
    assert(tramo.toList.map(_.pos) == List(Vector2(-1.0, 5.0), Vector2(6.0, -1.0)))
  }

  test("pasoSegmento: el tramo completo coincide con pasoSecuencial") {
    assert(iguales(pasoSegmento(aleatorio, 0.5, 0, aleatorio.length), pasoSecuencial(aleatorio, 0.5)))
  }

  test("pasoSegmento: con desde mayor que hasta no devuelve nada") {
    assert(pasoSegmento(cuatro, 1.0, 3, 1).isEmpty)
  }

  test("pasoSegmento: dos tramos contiguos reconstruyen el paso completo") {
    val unidos = pasoSegmento(aleatorio, 0.5, 0, 400) ++ pasoSegmento(aleatorio, 0.5, 400, aleatorio.length)
    assert(iguales(unidos, pasoSecuencial(aleatorio, 0.5)))
  }

  test("pasoParalelo: sistema de tamaño impar coincide exactamente con el secuencial") {
    val umbrales = List(1, 2, 3, 10, 500, 1001, 5000)
    assert(umbrales.forall(u => iguales(pasoParalelo(aleatorio, 0.5, u), pasoSecuencial(aleatorio, 0.5))))
  }

  test("pasoParalelo: conserva velocidad y masa de cada partícula en su lugar") {
    val resultado = pasoParalelo(aleatorio, 0.5, 10)
    assert(resultado.zip(aleatorio).forall { case (n, o) => n.vel == o.vel && n.masa == o.masa })
  }

  test("pasoParalelo: sobre el sistema vacío devuelve vacío con cualquier umbral") {
    assert(List(0, 1, 100).forall(u => pasoParalelo(Array[Particula](), 1.0, u).isEmpty))
  }

  test("pasoParalelo: con umbral mayor que el tamaño no hay reparto y da lo mismo") {
    assert(iguales(pasoParalelo(cuatro, 2.0, 100), pasoSecuencial(cuatro, 2.0)))
  }

  test("pasoParalelo: cien mil partículas con umbral 1024 quedan en su sitio") {
    val resultado = pasoParalelo(enormes, 0.5, 1024)
    assert(resultado.length == 100000 &&
      resultado.zipWithIndex.forall { case (q, i) => q.pos == Vector2(i + 0.5, -i + 1.0) })
  }

  // Punto 2 -------------------------------------------------------------------

  test("energiaSecuencial: suma las energías de partículas en movimiento") {
    val dos: Sistema = Array(
      Particula(Vector2(0, 0), Vector2(1, 1), 1.0),
      Particula(Vector2(0, 0), Vector2(0, 3), 4.0))
    assert(cerca(energiaSecuencial(dos), 19.0))
  }

  test("energiaSecuencial: cuatro partículas con velocidades y masas distintas") {
    assert(cerca(energiaSecuencial(cuatro), 22.25))
  }

  test("energiaSecuencial: el signo de la velocidad no cambia la energía") {
    val invertido = cuatro.map(q => Particula(q.pos, q.vel * -1.0, q.masa))
    assert(cerca(energiaSecuencial(invertido), energiaSecuencial(cuatro)))
  }

  test("energiaSecuencial: duplicar las velocidades multiplica la energía por cuatro") {
    val rapidas = aleatorio.map(q => Particula(q.pos, q.vel * 2.0, q.masa))
    val esperado = 4.0 * energiaSecuencial(aleatorio)
    assert(Math.abs(energiaSecuencial(rapidas) - esperado) < 1e-6 * esperado)
  }

  test("energiaSecuencial: no depende de dónde estén las partículas") {
    val movidas = aleatorio.map(q => Particula(q.pos + Vector2(100, -50), q.vel, q.masa))
    assert(cerca(energiaSecuencial(movidas), energiaSecuencial(aleatorio)))
  }

  test("energiaSecuencial: cien mil partículas iguales") {
    val iguales100k: Sistema = Array.fill(100000)(Particula(Vector2(0, 0), Vector2(3, 4), 2.0))
    assert(cerca(energiaSecuencial(iguales100k), 2500000.0))
  }

  test("masaTotal: suma masas con decimales") {
    val tres: Sistema = Array(
      Particula(Vector2(0, 0), Vector2(0, 0), 0.5),
      Particula(Vector2(1, 1), Vector2(0, 0), 1.25),
      Particula(Vector2(2, 2), Vector2(0, 0), 2.25))
    assert(cerca(masaTotal(tres), 4.0))
  }

  test("masaTotal: una sola partícula da su propia masa") {
    assert(masaTotal(Array(cuatro(2))) == 3.0)
  }

  test("masaTotal: cien mil partículas de masa uno") {
    assert(masaTotal(enormes) == 100000.0)
  }

  test("masaTotal: no cambia al mover el sistema") {
    assert(cerca(masaTotal(pasoSecuencial(aleatorio, 3.0)), masaTotal(aleatorio)))
  }

  test("centroDeMasaSecuencial: tres partículas con masas distintas") {
    val tres: Sistema = Array(
      Particula(Vector2(1, 2), Vector2(9, 9), 1.0),
      Particula(Vector2(3, 4), Vector2(0, 0), 3.0),
      Particula(Vector2(-2, 0), Vector2(1, 1), 2.0))
    assert(cerca(centroDeMasaSecuencial(tres), Vector2(1.0, 14.0 / 6.0)))
  }

  test("centroDeMasaSecuencial: cuatro partículas con masas uno a cuatro") {
    assert(cerca(centroDeMasaSecuencial(cuatro), Vector2(0.5, 1.2)))
  }

  test("centroDeMasaSecuencial: una sola partícula tiene su posición como centro") {
    assert(cerca(centroDeMasaSecuencial(Array(cuatro(3))), Vector2(-3.0, 4.0)))
  }

  test("centroDeMasaSecuencial: partículas sin masa dan el origen") {
    val sinMasa: Sistema = Array(
      Particula(Vector2(5, 5), Vector2(1, 1), 0.0),
      Particula(Vector2(-7, 2), Vector2(0, 0), 0.0))
    assert(centroDeMasaSecuencial(sinMasa) == Vector2(0, 0))
  }

  test("centroDeMasaSecuencial: multiplicar todas las masas no mueve el centro") {
    val pesadas = aleatorio.map(q => Particula(q.pos, q.vel, q.masa * 7.0))
    assert(cerca(centroDeMasaSecuencial(pesadas), centroDeMasaSecuencial(aleatorio)))
  }

  test("centroDeMasaSecuencial: trasladar las posiciones traslada el centro igual") {
    val d = Vector2(10, -5)
    val movidas = aleatorio.map(q => Particula(q.pos + d, q.vel, q.masa))
    assert(cerca(centroDeMasaSecuencial(movidas), centroDeMasaSecuencial(aleatorio) + d))
  }

  test("centroDeMasaSecuencial: las velocidades no influyen") {
    val quietas = aleatorio.map(q => Particula(q.pos, Vector2(0, 0), q.masa))
    assert(cerca(centroDeMasaSecuencial(quietas), centroDeMasaSecuencial(aleatorio)))
  }

  test("centroDeMasaSecuencial: queda dentro del rectángulo que encierra al sistema") {
    val c = centroDeMasaSecuencial(aleatorio)
    val xs = aleatorio.map(_.pos.x)
    val ys = aleatorio.map(_.pos.y)
    assert(c.x >= xs.min && c.x <= xs.max && c.y >= ys.min && c.y <= ys.max)
  }

  // Punto 3 -------------------------------------------------------------------

  test("energiaParalela: coincide con la secuencial en el sistema del informe") {
    assert(cerca(energiaParalela(cinco, 2), energiaSecuencial(cinco)))
    assert(cerca(energiaParalela(cinco, 2), 23.25))
    assert(cerca(energiaParalela(cinco, 1), energiaSecuencial(cinco)))
  }

  test("energiaParalela: coincide con la secuencial en un sistema aleatorio impar") {
    val esperado = energiaSecuencial(disperso)
    assert(List(1, 2, 10, 500, 777, 5000).forall(u =>
      cerca(energiaParalela(disperso, u), esperado)))
  }

  test("energiaParalela: agrupar las sumas de otra forma no cambia el valor") {
    // Umbral = tamaño: un solo grupo. Umbral 1: 777 grupos de uno. La suma
    // es asociativa, así que salvo redondeo dan lo mismo.
    assert(cerca(energiaParalela(disperso, disperso.length), energiaParalela(disperso, 1)))
  }

  test("energiaParalela: una sola partícula y velocidades duplicadas") {
    val una = Array(cinco(3)) // masa 4, velocidad (2, -2): 16.0
    assert(cerca(energiaParalela(una, 1), 16.0))
    val rapidas = disperso.map(q => Particula(q.pos, q.vel * 2.0, q.masa))
    val esperado = 4.0 * energiaSecuencial(disperso)
    assert(Math.abs(energiaParalela(rapidas, 32) - esperado) < 1e-6 * esperado)
  }

  test("energiaParalela: cero partículas da cero, con cualquier umbral") {
    assert(List(0, 1, 100).forall(u => energiaParalela(Array[Particula](), u) == 0.0))
  }

  test("centroDeMasaParalelo: coincide con la secuencial en el sistema del informe") {
    assert(cerca(centroDeMasaParalelo(cinco, 2), centroDeMasaSecuencial(cinco)))
    assert(cerca(centroDeMasaParalelo(cinco, 2), Vector2(0.75, 4.0 / 3.0)))
    assert(cerca(centroDeMasaParalelo(cinco, 1), centroDeMasaSecuencial(cinco)))
  }

  test("centroDeMasaParalelo: coincide con la secuencial en el sistema aleatorio impar") {
    val esperado = centroDeMasaSecuencial(disperso)
    assert(List(1, 2, 10, 500, 777, 5000).forall(u =>
      cerca(centroDeMasaParalelo(disperso, u), esperado)))
  }

  test("centroDeMasaParalelo: con mitades de masas 3 y 9, la suma da el centro y el promedio de promedios no") {
    // Mitad [0,2): centro (2/3, 2/3) con masa 3. Mitad [2,5): centro
    // (7/9, 14/9) con masa 9. El promedio de los dos promedios es
    // (13/18, 10/9); el centro correcto es ((2,2)+(7,14))/12 = (3/4, 4/3).
    val suma = centroDeMasaParalelo(cinco, 2)
    assert(cerca(suma, Vector2(0.75, 4.0 / 3.0)))
    val promedioDePromedios = Vector2(13.0 / 18.0, 10.0 / 9.0)
    assert(!cerca(suma, promedioDePromedios))
  }

  test("centroDeMasaParalelo: sistema no vacío sin masa da el origen") {
    val sinMasa: Sistema = Array(
      Particula(Vector2(9, 9), Vector2(1, 1), 0.0),
      Particula(Vector2(-4, 3), Vector2(1, 1), 0.0),
      Particula(Vector2(15, 1), Vector2(1, 1), 0.0))
    assert(centroDeMasaParalelo(sinMasa, 1) == Vector2(0, 0))
  }

  test("centroDeMasaParalelo: trasladar las partículas traslada el centro") {
    val d = Vector2(-7, 12)
    val movidas = disperso.map(q => Particula(q.pos + d, q.vel, q.masa))
    assert(cerca(centroDeMasaParalelo(movidas, 64), centroDeMasaSecuencial(disperso) + d))
  }

  // Punto 4 -------------------------------------------------------------------

  test("trabajo: una operación por partícula, también en los extremos") {
    assert(trabajo(1) == 1 && trabajo(0) == 0 && trabajo(2048) == 2048)
  }

  test("profundidad: tamaños que no son potencia de dos") {
    assert(profundidad(1000, 1000) == 1000)
    assert(profundidad(1000, 500) == 501)
    assert(profundidad(1000, 250) == 252)
    assert(profundidad(64, 8) == 11)
  }

  test("profundidad: con tamaño impar sigue el camino de la mitad mayor") {
    // 7 -> 4 -> 2 -> 1: tres particiones y un tramo de uno. Y para el árbol
    // de pasoParalelo(e5, ., 2) del informe, que tiene 3 hojas y 2
    // particiones con hoja mayor de tamaño 2: 2 + 2 = 4.
    assert(profundidad(7, 1) == 4 && profundidad(5, 2) == 4)
  }

  test("profundidad: con umbral cero no se queda sin fondo") {
    // 4 -> 2 -> 1: dos particiones y un tramo de uno.
    assert(profundidad(4, 0) == 3 && profundidad(1, 0) == 1)
  }

  test("tiempoEstimado: profundidad más trabajo entre procesadores") {
    assert(cerca(tiempoEstimado(1000, 2, 500), 1001.0))
    assert(cerca(tiempoEstimado(1000, 10, 1), 111.0))
    assert(cerca(tiempoEstimado(1000, 1, 1), 1011.0))
  }

  test("tiempoEstimado: mejora al agregar procesadores y nunca baja de la profundidad") {
    val estimados = List(1, 2, 4, 8, 16).map(p => tiempoEstimado(1000, p, 1))
    assert(estimados.zip(estimados.tail).forall { case (a, b) => a >= b })
    assert(estimados.forall(_ >= profundidad(1000, 1)))
  }
  //puto 5--------------------------------------------------------------------
  val sistema: Sistema = Array.tabulate(2000)(i => Particula(
    Vector2(i.toDouble, (i % 7).toDouble),
    Vector2((i % 3).toDouble, 1.0),
    1.0 + (i % 4)))


  test("aceleracion: un paralelo más lento da menos de 1") {
    assert(cerca(aceleracion(50.0, 100.0), 0.5))
  }

  test("aceleracion: un tiempo secuencial de cero da cero") {
    assert(cerca(aceleracion(0.0, 10.0), 0.0))
  }

  test("aceleracion: escalar los dos tiempos no cambia el cociente") {
    assert(cerca(aceleracion(7.5, 2.5), aceleracion(75.0, 25.0)))
  }

  test("umbralConveniente: con un solo candidato devuelve ese candidato") {
    assert(umbralConveniente(sistema, 0.5, List(256)) == 256)
  }

  test("umbralConveniente: con la lista vacía y el sistema vacío devuelve cero") {
    assert(umbralConveniente(Array[Particula](), 0.5, Nil) == 0)
  }

  test("control: con umbral igual al tamaño, pasoParalelo da lo mismo que pasoSecuencial") {
    val par = pasoParalelo(sistema, 0.5, sistema.length)
    val seq = pasoSecuencial(sistema, 0.5)
    assert(par.sameElements(seq))
  }

  test("umbralConveniente: el umbral elegido produce el mismo resultado que el secuencial") {
    val u = umbralConveniente(sistema, 0.5, List(8, 64, 512))
    assert(pasoParalelo(sistema, 0.5, u).sameElements(pasoSecuencial(sistema, 0.5)))
  }

}
