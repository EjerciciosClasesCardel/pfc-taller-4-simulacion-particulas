# Informe de corrección — Taller 4: simulación de partículas

## 0. Notación y especificación

Sea $s = (p_0, p_1, \dots, p_{n-1})$ un sistema de $n \ge 0$ partículas, con $p_k = (r_k, v_k, m_k)$: posición,
velocidad y masa. Sea $dt$ el paso de tiempo. Se define la partícula movida

```math
q_k \;=\; \mathrm{mover}(p_k, dt) \;=\; (\,r_k + v_k\,dt,\; v_k,\; m_k\,)
```

y, para $0 \le d \le h \le n$, la **lista de las partículas movidas del tramo $[d, h)$**, definida por recursión:

```math
T(d, h) \;=\;
\begin{cases}
[\,] & \text{si } d = h \\
q_d \;::\; T(d+1, h) & \text{si } d < h
\end{cases}
```

Es decir, $T(d,h) = [q_d, q_{d+1}, \dots, q_{h-1}]$. Con esto la especificación de cada función es:

- $\mathrm{pasoSegmento}(s, dt, d, h) = T(d, h)$ para $0 \le d \le h \le n$.
- $\mathrm{pasoSecuencial}(s, dt) = \mathrm{pasoParalelo}(s, dt, u) = T(0, n)$ para todo umbral $u$.
- $\mathrm{energiaSecuencial}(s) = \sum_{k=0}^{n-1} \tfrac{1}{2}\, m_k \lVert v_k \rVert^2$.
- $\mathrm{masaTotal}(s) = \sum_{k=0}^{n-1} m_k$.
- $\mathrm{centroDeMasaSecuencial}(s) = \dfrac{\sum_k m_k\, r_k}{\sum_k m_k}$ si $\sum_k m_k \ne 0$, y el origen $(0,0)$ en caso contrario.

> **Sobre el punto flotante.** Las demostraciones se hacen en los reales. En `Double`, cada suma se redondea, de modo que
> el programa devuelve la suma hecha de izquierda a derecha con redondeo. Por ejemplo, la energía de un sistema de
> ejemplo ejecutado da `23.250000000000004` y no 23.25. Por eso las pruebas comparan con tolerancia. Para
> las funciones del punto 1 no se acumulan sumas entre partículas: cada partícula se calcula de forma independiente, y la
> igualdad entre versiones es **exacta**.

## 1. Corrección de `pasoSegmento` (recursión de cola)

```Scala
def pasoSegmento(s: Sistema, dt: Double, desde: Int, hasta: Int): Sistema = {
  // Recorre de hasta - 1 hacia desde anteponiendo, así la lista queda en orden.
  @tailrec
  def aux(i: Int, acc: List[Particula]): List[Particula] =
    if (i < desde) acc
    else aux(i - 1, mover(s(i), dt) :: acc)

  aux(hasta - 1, Nil).toArray
}
```

La función se argumenta con el método de la invariante de ciclo, con **precondición** $0 \le d \le h \le n$.

- **Estado:** $(i, acc)$.
- **Estado inicial:** $s_0 = (h-1,\; [\,])$.
- **Estado final:** cuando $i < d$.
- **Transformación:** $\mathrm{transformar}(i, acc) = (i-1,\; q_i :: acc)$.
- **Invariante:**

```math
\mathrm{Inv}(i, acc) \;\equiv\; d - 1 \le i \le h - 1 \;\wedge\; acc = T(i+1, h)
```

**1.** $\mathrm{Inv}(s_0)$. Con $i = h-1$: $d-1 \le h-1 \le h-1$ se cumple porque $d \le h$, y $T(h, h) = [\,]$ = `Nil`, que es
el valor inicial de $acc$.

**2.** La invariante se conserva. Sea $(i, acc)$ un estado no final con $\mathrm{Inv}$, es decir, $i \ge d$. Entonces
$s(i)$ es un índice válido, porque $0 \le d \le i \le h-1 \le n-1$. El nuevo estado es $(i-1,\; q_i :: acc)$ y

```math
q_i :: acc \;=\; q_i :: T(i+1, h) \;=\; T(i, h)
```

por la definición de $T$ (caso $i < h$). Además $d - 1 \le i - 1 \le h - 1$. Luego $\mathrm{Inv}(i-1, q_i :: acc)$.

**3.** Estado final. Si $i < d$ y $\mathrm{Inv}$, entonces $i = d - 1$ y $acc = T(d, h)$. La función devuelve
`acc.toArray`, que conserva el orden y la longitud, así que el resultado es $T(d, h)$ como arreglo.

**4.** Terminación. En cada paso $i$ baja en 1, desde $h - 1$ hasta $d - 1$, así que hay exactamente $h - d$ pasos
y siempre se llega al estado final.

Concluimos $\mathrm{pasoSegmento}(s, dt, d, h) = T(d, h)$. El resultado tiene $h - d$ partículas, con índices
$d, \dots, h-1$, cada una **una sola vez** y en orden. Si $h < d$ (fuera de la especificación), el estado inicial ya es final y
devuelve el arreglo vacío.

## 2. Corrección de `pasoSecuencial`

```Scala
def pasoSecuencial(s: Sistema, dt: Double): Sistema =
  pasoSegmento(s, dt, 0, s.length)
```

Es `pasoSegmento` con $d = 0$ y $h = n$, que cumplen la precondición. Por la sección 1,
$\mathrm{pasoSecuencial}(s, dt) = T(0, n)$: cada partícula movida $dt$ con su velocidad, y velocidad y masa intactas
por la definición de $q_k$. Para $n = 0$, $T(0,0) = [\,]$ y el resultado es vacío.

## 3. Partir el sistema no pierde ni repite partículas

**Lema de partición.** Para $0 \le d \le m \le h \le n$:

```math
T(d, h) \;=\; T(d, m) \;{+}{+}\; T(m, h)
```

*Demostración*, por inducción sobre $m - d$.

- **Base**, $m = d$: $T(d, d) \,{+}{+}\, T(d, h) = [\,] \,{+}{+}\, T(d, h) = T(d, h)$.
- **Paso**, $m > d$ (y por tanto $d < h$): por la definición de $T$ y la hipótesis de inducción sobre $T(d+1, h)$,

```math
T(d,h) = q_d :: T(d+1,h) = q_d :: \bigl(T(d+1,m) \,{+}{+}\, T(m,h)\bigr) = \bigl(q_d :: T(d+1,m)\bigr) \,{+}{+}\, T(m,h) = T(d,m) \,{+}{+}\, T(m,h)
```

$\blacksquare$

Lo que dice el lema en términos de índices: $[d, h) = [d, m) \,\cup\, [m, h)$ y $[d, m) \cap [m, h) = \varnothing$, de modo que
ningún índice queda fuera ni aparece en las dos mitades, y los tamaños suman: $(m - d) + (h - m) = h - d$.

**Asociatividad.** La concatenación de listas (y de arreglos) es asociativa y tiene como neutro la lista vacía:

```math
(a \,{+}{+}\, b) \,{+}{+}\, c \;=\; a \,{+}{+}\, (b \,{+}{+}\, c), \qquad [\,] \,{+}{+}\, a = a \,{+}{+}\, [\,] = a
```

Por eso da igual cómo se agrupe la unión de los tramos en el árbol de llamados: cualquier agrupación entrega la misma lista.
(La concatenación no es conmutativa, así que lo que se respeta es el **orden** izquierda-derecha de las llamadas, y `pasoParalelo` usa
`izq ++ der` en ese orden.)

## 4. Corrección de `pasoParalelo` y su igualdad con la secuencial

```Scala
def pasoParalelo(s: Sistema, dt: Double, umbral: Int): Sistema = {
  def aux(desde: Int, hasta: Int): Sistema =
    if (hasta - desde <= umbral || hasta - desde <= 1)
      pasoSegmento(s, dt, desde, hasta)
    else {
      val m = desde + (hasta - desde) / 2
      val (izq, der) = parallel(aux(desde, m), aux(m, hasta))
      izq ++ der
    }

  aux(0, s.length)
}
```

Sea $\mathrm{aux}(d, h)$ la función interna. **Afirmación:** para todo umbral $u$ y todo $0 \le d \le h \le n$,
$\mathrm{aux}(d, h) = T(d, h)$.

*Demostración*, por inducción fuerte sobre el tamaño $h - d$. Se supone la afirmación para todo tramo de menor tamaño.

- **Caso hoja:** $h - d \le u$ o $h - d \le 1$. La función devuelve `pasoSegmento(s, dt, d, h)`, que por la
  sección 1 es $T(d, h)$.
- **Caso partición:** $h - d > u$ y $h - d \ge 2$. Con $m = d + \lfloor (h - d)/2 \rfloor$ se tiene
  $d < m < h$, porque $\lfloor (h-d)/2 \rfloor \ge 1$ y $\lfloor (h-d)/2 \rfloor < h - d$. Entonces $m - d < h - d$ y
  $h - m < h - d$: los dos tramos son **estrictamente menores**, y se aplica la hipótesis de inducción a cada uno:
  $\mathrm{aux}(d,m) = T(d,m)$ y $\mathrm{aux}(m,h) = T(m,h)$. El valor de $\mathrm{parallel}(a, b)$ es el par
  $(a, b)$ de los valores de sus dos argumentos; lo que cambia es **cuándo y dónde** se evalúan, no su valor.
  El resultado es `izq ++ der` $= T(d,m) \,{+}{+}\, T(m,h) = T(d,h)$ por el lema de partición. $\blacksquare$

**Teorema.** Para todo sistema $s$, todo $dt$ y todo umbral $u$ (incluso $u \le 0$ o $u \ge n$):

```math
\mathrm{pasoParalelo}(s, dt, u) \;=\; \mathrm{aux}(0, n) \;=\; T(0, n) \;=\; \mathrm{pasoSecuencial}(s, dt)
```

La igualdad es **exacta** (no solo con tolerancia), porque cada posición se calcula con la misma operación
$r_k + v_k\,dt$ y esa operación no se reordena. Las pruebas la verifican con `sameElements`.

**Terminación y papel de la guarda `hasta - desde <= 1`.** En cada partición los dos tramos son más pequeños y
no vacíos ($m - d \ge 1$ y $h - m \ge 1$), así que el tamaño decrece hasta llegar al caso hoja.
La guarda es **necesaria**: sin ella, con la condición `hasta - desde <= umbral` sola y $u = 0$, un tramo de
tamaño 1 cumple $1 > 0$, se parte con $m = d + 0 = d$ y su segunda mitad es $[d, h)$, **el mismo tramo**. Esto es un
contraejemplo de terminación: con una sola partícula y $u = 0$, `aux(0, 1)` se llamaría a sí misma sin fin.
Se comprobó ejecutando ese caso exacto (una partícula, umbral 0) con la variante sin la guarda: no terminó en 15 segundos
y agotó los hilos del sistema, mientras que la versión entregada devuelve el resultado correcto al instante y pasa la prueba
`con umbral cero no se queda sin fondo`.

**Ausencia de interferencia.** Las dos ramas de `parallel` solo **leen** el arreglo `s`, que nunca se modifica, y
cada una construye listas y arreglos nuevos. No hay escritura compartida, así que el resultado no depende del orden en que terminen.

## 5. Corrección del punto 2 (recursión de cola)

En las tres funciones el estado contiene el índice $i$, que crece de a uno desde $0$ hasta $n$. El estado final es
$i \ge n$. Como $i$ aumenta en 1 en cada paso, **terminan** tras $n$ pasos.

### 5.1 `energiaSecuencial`

```Scala
def energiaSecuencial(s: Sistema): Double = {
  @tailrec
  def aux(i: Int, acc: Double): Double =
    if (i >= s.length) acc
    else {
      val v = s(i).vel.norma
      aux(i + 1, acc + 0.5 * s(i).masa * v * v)
    }

  aux(0, 0.0)
}
```

Sea $e_k = \tfrac{1}{2}\, m_k \lVert v_k \rVert^2$ y el estado $(i, acc)$, con $s_0 = (0, 0.0)$ e invariante:

```math
\mathrm{Inv}(i, acc) \;\equiv\; 0 \le i \le n \;\wedge\; acc = \sum_{k=0}^{i-1} e_k
```

**1.** $\mathrm{Inv}(0, 0.0)$: la suma vacía vale 0. **2.** Si $i < n$ e $\mathrm{Inv}(i, acc)$, el nuevo estado es
$(i+1,\; acc + e_i)$ y $acc + e_i = \sum_{k=0}^{i} e_k$. **3.** En el estado final $i = n$, por lo que
$acc = \sum_{k=0}^{n-1} e_k$. **4.** Terminación: $i$ llega a $n$ en $n$ pasos. En el sistema vacío, $n = 0$ y devuelve $0.0$.

### 5.2 `masaTotal`

```Scala
def masaTotal(s: Sistema): Double = {
  @tailrec
  def aux(i: Int, acc: Double): Double =
    if (i >= s.length) acc
    else aux(i + 1, acc + s(i).masa)

  aux(0, 0.0)
}
```

Igual con $acc = \sum_{k<i} m_k$: el estado inicial es $(0, 0.0)$, cada paso suma $m_i$ y en $i = n$ devuelve $\sum_{k=0}^{n-1} m_k$.

### 5.3 `centroDeMasaSecuencial`

```Scala
def centroDeMasaSecuencial(s: Sistema): Vector2 = {
  @tailrec
  def aux(i: Int, ponderada: Vector2, masa: Double): (Vector2, Double) =
    if (i >= s.length) (ponderada, masa)
    else aux(i + 1, ponderada + s(i).pos * s(i).masa, masa + s(i).masa)

  val (ponderada, masa) = aux(0, Vector2(0, 0), 0.0)
  if (masa == 0.0) Vector2(0, 0)
  else Vector2(ponderada.x / masa, ponderada.y / masa)
}
```

Estado $(i, w, M)$, con $w$ un vector y $M$ un número, y estado inicial $(0, (0,0), 0.0)$. Invariante:

```math
\mathrm{Inv}(i, w, M) \;\equiv\; 0 \le i \le n \;\wedge\; w = \sum_{k=0}^{i-1} m_k\, r_k \;\wedge\; M = \sum_{k=0}^{i-1} m_k
```

**1.** El estado inicial cumple la invariante con sumas vacías. **2.** Cada paso produce
$(i+1,\; w + m_i r_i,\; M + m_i)$, que extiende ambas sumas con el término $i$. **3.** En $i = n$, $w = \sum_k m_k r_k$ y
$M = \sum_k m_k$. La función termina con:

```math
\text{resultado} \;=\;
\begin{cases}
(0, 0) & \text{si } M = 0 \\[4pt]
\left(\dfrac{w_x}{M},\; \dfrac{w_y}{M}\right) & \text{si } M \ne 0
\end{cases}
```

que es la especificación. La **división se hace una sola vez, al final**, sobre las sumas completas, y la rama
$M = 0$ (sistema vacío o todas las masas en cero) evita dividir $0/0$. **4.** Terminación: $n$ pasos.

*Comprobación independiente con valores ejecutados:* para el sistema de ejemplo del informe de proceso, la
ejecución dio $w = (9, 16)$ y $M = 12$, y la división da $(0.75,\; 1.3333\ldots)$, igual que el cálculo hecho
aparte en Python.

## 6. Qué cubre este informe y qué falta

Los puntos pedidos por el enunciado para el punto 1 quedan así: que **partir el sistema no pierde ni repite
partículas** (sección 3 y caso partición de la sección 4), que la operación con que se combinan los parciales
(**concatenación**) **es asociativa** (sección 3) y que la versión paralela da lo mismo que la secuencial (sección 4).

> **Pendiente, a cargo de los demás integrantes del grupo:** corrección de `energiaParalela` y `centroDeMasaParalelo`
> (punto 3), incluida la explicación pedida de por qué la suma es válida para repartir y el promedio no lo es
> directamente, y de las funciones de los puntos 4 y 5. La asociatividad de la suma en `Double` es solo aproximada
> por el redondeo, y debe tratarse allí.
