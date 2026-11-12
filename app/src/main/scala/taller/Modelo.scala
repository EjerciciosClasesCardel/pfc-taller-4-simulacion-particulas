package taller

/** Modelo de datos del taller 4, tal como lo entrega el material. */
case class Vector2(x: Double, y: Double) {
  def +(o: Vector2): Vector2 = Vector2(x + o.x, y + o.y)
  def *(k: Double): Vector2 = Vector2(x * k, y * k)
  def norma: Double = Math.sqrt(x * x + y * y)
}

case class Particula(pos: Vector2, vel: Vector2, masa: Double)
