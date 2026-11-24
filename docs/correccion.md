# Informe de corrección: un paso de la simulación de partículas

## 1. Especificación

Sea $s = (s_0, s_1, \ldots, s_{n-1})$ un sistema de $n$ partículas, donde cada
$s_i = (\text{pos}_i, \text{vel}_i, \text{masa}_i)$, y sea $dt$ el paso de
tiempo. Definimos la función $\text{avanza}$ sobre una partícula:

```math
\text{avanza}(s_i, dt) = (\text{pos}_i + \text{vel}_i \cdot dt,\ \text{vel}_i,\ \text{masa}_i)
```

y, para $0 \leq d \leq h \leq n$, el tramo avanzado entre $d$ (incluido) y $h$
(excluido):

```math
\text{tramo}(s, dt, d, h) = (\text{avanza}(s_d, dt),\ \text{avanza}(s_{d+1}, dt),\ \ldots,\ \text{avanza}(s_{h-1}, dt))
```

Lo que el enunciado pide es:

- $\text{mover}(p, dt) == \text{avanza}(p, dt)$.
- $\text{pasoSegmento}(s, dt, d, h) == \text{tramo}(s, dt, d, h)$.
- $\text{pasoSecuencial}(s, dt) == \text{tramo}(s, dt, 0, n)$.
- $\text{pasoParalelo}(s, dt, u) == \text{tramo}(s, dt, 0, n)$ para **cualquier**
  umbral $u$.

## 2. Corrección de `mover`

```scala
def mover(p: Particula, dt: Double): Particula =
  p.copy(pos = p.pos + p.vel * dt)
```

No hay recursión ni iteración, así que basta evaluar. `p.vel * dt` es el vector
$(\text{vel}_x \cdot dt,\ \text{vel}_y \cdot dt)$ y `p.pos + ...` suma componente
a componente, luego la nueva posición es $\text{pos} + \text{vel} \cdot dt$.
`copy(pos = ...)` crea una partícula con la posición nueva y deja `vel` y
`masa` con los valores de `p`. Esto es $\text{avanza}(p, dt)$.

## 3. Corrección de `pasoSegmento` y de `pasoSecuencial`

```scala
def pasoSegmento(s: Sistema, dt: Double, desde: Int, hasta: Int): Sistema =
  (for (i <- desde until hasta) yield mover(s(i), dt)).toArray
```

La expresión `for ... yield` no tiene bucle ni estado: construye una colección
en la que, para cada $i \in [d, h)$ en orden creciente, aparece
$\text{mover}(s_i, dt)$. Por eso:

- el resultado tiene $h - d$ elementos (ninguno si $h \leq d$), y
- el elemento en la posición $j$ del resultado, con $0 \leq j < h - d$, es
  $\text{mover}(s_{d+j}, dt) = \text{avanza}(s_{d+j}, dt)$.

Esto es exactamente $\text{tramo}(s, dt, d, h)$. Además, `s` solo se lee: el
resultado es un arreglo nuevo.

`pasoSecuencial(s, dt)` es `pasoSegmento(s, dt, 0, s.length)`, es decir
$\text{tramo}(s, dt, 0, n)$.

### Lema de concatenación

Para $d \leq m \leq h$:

```math
\text{tramo}(s, dt, d, m) \mathbin{+\!\!+} \text{tramo}(s, dt, m, h) = \text{tramo}(s, dt, d, h)
```

*Demostración.* El lado izquierdo tiene $(m - d) + (h - m) = h - d$ elementos,
igual que el derecho. Sea $0 \leq j < h - d$.

- Si $j < m - d$, el elemento $j$ del lado izquierdo es el elemento $j$ del
  primer tramo: $\text{avanza}(s_{d+j}, dt)$.
- Si $j \geq m - d$, es el elemento $j - (m - d)$ del segundo tramo:
  $\text{avanza}(s_{m + j - (m - d)}, dt) = \text{avanza}(s_{d+j}, dt)$.

En los dos casos coincide con el elemento $j$ de $\text{tramo}(s, dt, d, h)$. $\square$

## 4. Corrección de `pasoParalelo`

```scala
def pasoParalelo(s: Sistema, dt: Double, umbral: Int): Sistema = {
  val piso = Math.max(umbral, 1)

  def paso(desde: Int, hasta: Int): Sistema =
    if (hasta - desde <= piso) pasoSegmento(s, dt, desde, hasta)
    else {
      val mitad = desde + (hasta - desde) / 2
      val (izquierda, derecha) = parallel(paso(desde, mitad), paso(mitad, hasta))
      izquierda ++ derecha
    }

  paso(0, s.length)
}
```

### Qué pasa con `parallel`

`parallel(a, b)` devuelve la pareja de los valores de `a` y de `b`, en ese
orden. Las dos expresiones solo leen `s` y construyen arreglos nuevos; ninguna
escribe en algo que la otra lea. Por eso el valor de la pareja no depende de
cuál termine primero, y `parallel(a, b)` se comporta, en el resultado, igual
que evaluar `a` y luego `b`.

### Teorema

Para todo $0 \leq d \leq h \leq n$:

```math
\text{paso}(d, h) == \text{tramo}(s, dt, d, h)
```

*Demostración* por inducción fuerte sobre $k = h - d$. Sea $\text{piso} \geq 1$.

**Caso base:** $k \leq \text{piso}$. El condicional toma la primera rama y
`paso(d, h)` es `pasoSegmento(s, dt, d, h)`, que por la sección 3 es
$\text{tramo}(s, dt, d, h)$.

**Caso de inducción:** $k > \text{piso}$. Como $\text{piso} \geq 1$, se tiene
$k \geq 2$. Sea $m = d + \lfloor k / 2 \rfloor$. Como $k \geq 2$:

```math
1 \leq \lfloor k/2 \rfloor \leq k - 1 \implies d < m < h
```

Los dos tramos tienen tamaños $m - d = \lfloor k/2 \rfloor < k$ y
$h - m = k - \lfloor k/2 \rfloor < k$. Por la hipótesis de inducción:

```math
\text{paso}(d, m) == \text{tramo}(s, dt, d, m)
\qquad
\text{paso}(m, h) == \text{tramo}(s, dt, m, h)
```

Entonces `paso(d, h)` devuelve $\text{tramo}(d, m) \mathbin{+\!\!+} \text{tramo}(m, h)$,
que por el lema de concatenación es $\text{tramo}(s, dt, d, h)$. $\square$

### Conclusión

`pasoParalelo(s, dt, u)` es `paso(0, s.length)`, es decir
$\text{tramo}(s, dt, 0, n) == \text{pasoSecuencial}(s, dt)$, **sea cual sea el
umbral** $u$.

## 5. Partir el sistema no pierde ni repite partículas

Los dos tramos de cada partición son $[d, m)$ y $[m, h)$ con $d < m < h$.

- **No se repite ninguna:** un índice $i$ está en el primero si $d \leq i < m$
  y en el segundo si $m \leq i < h$. Las dos condiciones no pueden cumplirse a
  la vez.
- **No se pierde ninguna:** todo $i$ con $d \leq i < h$ cumple $i < m$ o
  $i \geq m$, así que está en uno de los dos.

Aplicado en cada nivel, los tramos que llegan a las hojas son disjuntos y su
unión es $[0, n)$. Por ejemplo, con $n = 7$ y umbral 2 las hojas son
$[0,1)$, $[1,3)$, $[3,5)$ y $[5,7)$, con $1 + 2 + 2 + 2 = 7$ partículas.
Cada una aparece una sola vez y en su lugar, porque el lema de concatenación
conserva el orden de los índices. Esto vale también cuando $k$ es impar, pues
$m$ se calcula con división entera y la mitad derecha absorbe la partícula que
sobra.

## 6. La operación que combina es asociativa

Los resultados parciales se combinan con la concatenación de arreglos, $++$.
Es asociativa: para tres arreglos $A$, $B$, $C$,

```math
(A \mathbin{+\!\!+} B) \mathbin{+\!\!+} C = A \mathbin{+\!\!+} (B \mathbin{+\!\!+} C)
```

*Demostración.* Los dos lados tienen $|A| + |B| + |C|$ elementos. El elemento
$j$ de cualquiera de los dos es el elemento $j$ de $A$ si $j < |A|$; el
elemento $j - |A|$ de $B$ si $|A| \leq j < |A| + |B|$; y el elemento
$j - |A| - |B|$ de $C$ en el resto. $\square$

El arreglo vacío es el elemento neutro, y por eso los tramos vacíos no alteran
el resultado. La concatenación **no** es conmutativa, y no tiene por qué serlo:
el orden lo conserva `paso` al poner siempre `izquierda` antes que `derecha`,
sin importar cuál de las dos mitades termine primero.

## 7. Terminación

En cada llamada recursiva el tamaño del tramo baja estrictamente: de $k$ a
$\lfloor k/2 \rfloor$ y a $k - \lfloor k/2 \rfloor$, ambos menores que $k$ cuando
$k \geq 2$. Como solo se parte con $k > \text{piso} \geq 1$, siempre se cumple
$k \geq 2$, y los tamaños llegan a un valor menor o igual que el piso en un
número finito de pasos.

Sin el piso el argumento falla: con umbral 0 y un tramo de tamaño 1 se
cumpliría $k > 0$, la mitad sería $\lfloor 1/2 \rfloor = 0$ y el tramo derecho
volvería a ser $[d, h)$, con el mismo tamaño, sin terminar nunca. Por eso
`piso = Math.max(umbral, 1)`.

## 8. Casos de prueba propios del punto 1

| Prueba | Qué comprueba |
|---|---|
| `mover` con $dt$ negativo | la partícula retrocede: posición $(1,1) + (-1,2)\cdot(-2) = (3,-3)$ |
| `pasoSecuencial` con velocidades distintas | cada partícula usa su propia velocidad |
| `pasoSegmento` entre 1 y 3 | un tramo del medio respeta los índices |
| `pasoParalelo` con umbral igual al tamaño | coincide con `pasoSecuencial` |
| `pasoParalelo` con umbral $-4$ y tres partículas | conserva orden y tamaño en un sistema impar |