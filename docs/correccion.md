# Informe de corrección — Taller 4: simulación de partículas

## 0. Notación y especificación

Sea $s = (p_0, p_1, \dots, p_{n-1})$ un sistema de $n \ge 0$ partículas, con $p_k = (r_k, v_k, m_k)$: posición,
velocidad y masa. Sea $dt$ el paso de tiempo. Se define la partícula movida

$$
q_k \;=\; \mathrm{mover}(p_k, dt) \;=\; (\,r_k + v_k\,dt,\; v_k,\; m_k\,)
$$

y, para $0 \le d \le h \le n$, la **lista de las partículas movidas del tramo $[d, h)$**, definida por recursión:

$$
T(d, h) \;=\;
\begin{cases}
[\,] & \text{si } d = h \\
q_d \;::\; T(d+1, h) & \text{si } d < h
\end{cases}
$$

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

$$
\mathrm{Inv}(i, acc) \;\equiv\; d - 1 \le i \le h - 1 \;\wedge\; acc = T(i+1, h)
$$

**1.** $\mathrm{Inv}(s_0)$. Con $i = h-1$: $d-1 \le h-1 \le h-1$ se cumple porque $d \le h$, y $T(h, h) = [\,]$ = `Nil`, que es
el valor inicial de $acc$.

**2.** La invariante se conserva. Sea $(i, acc)$ un estado no final con $\mathrm{Inv}$, es decir, $i \ge d$. Entonces
$s(i)$ es un índice válido, porque $0 \le d \le i \le h-1 \le n-1$. El nuevo estado es $(i-1,\; q_i :: acc)$ y

$$
q_i :: acc \;=\; q_i :: T(i+1, h) \;=\; T(i, h)
$$

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

$$
T(d, h) \;=\; T(d, m) \;{+}{+}\; T(m, h)
$$

*Demostración*, por inducción sobre $m - d$.

- **Base**, $m = d$: $T(d, d) \,{+}{+}\, T(d, h) = [\,] \,{+}{+}\, T(d, h) = T(d, h)$.
- **Paso**, $m > d$ (y por tanto $d < h$): por la definición de $T$ y la hipótesis de inducción sobre $T(d+1, h)$,

$$
T(d,h) = q_d :: T(d+1,h) = q_d :: \bigl(T(d+1,m) \,{+}{+}\, T(m,h)\bigr) = \bigl(q_d :: T(d+1,m)\bigr) \,{+}{+}\, T(m,h) = T(d,m) \,{+}{+}\, T(m,h)
$$

$\blacksquare$

Lo que dice el lema en términos de índices: $[d, h) = [d, m) \,\cup\, [m, h)$ y $[d, m) \cap [m, h) = \varnothing$, de modo que
ningún índice queda fuera ni aparece en las dos mitades, y los tamaños suman: $(m - d) + (h - m) = h - d$.

**Asociatividad.** La concatenación de listas (y de arreglos) es asociativa y tiene como neutro la lista vacía:

$$
(a \,{+}{+}\, b) \,{+}{+}\, c \;=\; a \,{+}{+}\, (b \,{+}{+}\, c), \qquad [\,] \,{+}{+}\, a = a \,{+}{+}\, [\,] = a
$$

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

$$
\mathrm{pasoParalelo}(s, dt, u) \;=\; \mathrm{aux}(0, n) \;=\; T(0, n) \;=\; \mathrm{pasoSecuencial}(s, dt)
$$

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

$$
\mathrm{Inv}(i, acc) \;\equiv\; 0 \le i \le n \;\wedge\; acc = \sum_{k=0}^{i-1} e_k
$$

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

$$
\mathrm{Inv}(i, w, M) \;\equiv\; 0 \le i \le n \;\wedge\; w = \sum_{k=0}^{i-1} m_k\, r_k \;\wedge\; M = \sum_{k=0}^{i-1} m_k
$$

**1.** El estado inicial cumple la invariante con sumas vacías. **2.** Cada paso produce
$(i+1,\; w + m_i r_i,\; M + m_i)$, que extiende ambas sumas con el término $i$. **3.** En $i = n$, $w = \sum_k m_k r_k$ y
$M = \sum_k m_k$. La función termina con:

$$
\text{resultado} \;=\;
\begin{cases}
(0, 0) & \text{si } M = 0 \\[4pt]
\left(\dfrac{w_x}{M},\; \dfrac{w_y}{M}\right) & \text{si } M \ne 0
\end{cases}
$$

que es la especificación. La **división se hace una sola vez, al final**, sobre las sumas completas, y la rama
$M = 0$ (sistema vacío o todas las masas en cero) evita dividir $0/0$. **4.** Terminación: $n$ pasos.

*Comprobación independiente con valores ejecutados:* para el sistema de ejemplo del informe de proceso, la
ejecución dio $w = (9, 16)$ y $M = 12$, y la división da $(0.75,\; 1.3333\ldots)$, igual que el cálculo hecho
aparte en Python.

## 6. Corrección del punto 3: las magnitudes en paralelo

Las dos funciones reparten con el mismo esquema de `pasoParalelo` (sección 4). Para argumentarlas se
extiende a los tramos la especificación que la sección 0 dio para el sistema completo, igual que se hizo con
$T(d, h)$:

$$
E(d, h) = \sum_{k=d}^{h-1} e_k, \quad e_k = \tfrac{1}{2}\, m_k \lVert v_k \rVert^2
$$

$$
P(d, h) = \sum_{k=d}^{h-1} m_k\, r_k, \qquad M(d, h) = \sum_{k=d}^{h-1} m_k, \qquad CM(d, h) = P(d, h) / M(d, h)
$$

con $CM = (0, 0)$ si $M(d, h) = 0$. La especificación del punto 3 es, para todo umbral $u$:
$\mathrm{energiaParalela}(s, u) = E(0, n)$ y $\mathrm{centroDeMasaParalelo}(s, u) = CM(0, n)$.

El argumento general es el que anticipa el enunciado: el resultado **no depende de cómo se agrupen las
sumas**, porque la suma es asociativa. Ese es el permiso para repartir; el promedio no lo es, y esa es la
trampa.

### 6.1 `energiaParalela`

```Scala
def energiaParalela(s: Sistema, umbral: Int): Double = {
  @tailrec
  def hoja(i: Int, hasta: Int, acc: Double): Double =
    if (i >= hasta) acc
    else {
      val v = s(i).vel.norma
      hoja(i + 1, hasta, acc + 0.5 * s(i).masa * v * v)
    }

  def aux(desde: Int, hasta: Int): Double =
    if (hasta - desde <= umbral || hasta - desde <= 1)
      hoja(desde, hasta, 0.0)
    else {
      val m = desde + (hasta - desde) / 2
      val (e1, e2) = parallel(aux(desde, m), aux(m, hasta))
      e1 + e2
    }

  aux(0, s.length)
}
```

**Afirmación:** para todo umbral $u$ y todo $0 \le d \le h \le n$, $\mathrm{aux}(d, h) = E(d, h)$.

*Demostración*, por inducción fuerte sobre el tamaño $h - d$, con la misma estructura de la sección 4.

- **Caso hoja** ($h - d \le u$ o $h - d \le 1$): devuelve `hoja(desde, hasta, 0.0)`. El recorrido `hoja` es el
  de `energiaSecuencial` (sección 5.1) sobre el tramo, con el invariante
  $\mathrm{Inv}(i, acc) \equiv i \le h \wedge acc = \sum_{k=d}^{i-1} e_k$: el estado inicial $(d, 0.0)$ cumple
  (suma vacía), cada paso suma $e_i$ y el estado final $i = h$ da $acc = E(d, h)$. Termina tras $h - d$ pasos.
- **Caso partición** ($h - d > u$ y $h - d \ge 2$): como se verificó en la sección 4, $d < m < h$ y los dos
  tramos son **estrictamente menores**, así que la hipótesis de inducción da
  $\mathrm{aux}(d, m) = E(d, m)$ y $\mathrm{aux}(m, h) = E(m, h)$. El resultado es

$$
E(d, m) + E(m, h) \;=\; \sum_{k=d}^{m-1} e_k \,+\, \sum_{k=m}^{h-1} e_k \;=\; \sum_{k=d}^{h-1} e_k \;=\; E(d, h)
$$

porque los tramos son contiguos y disjuntos (lema de partición, sección 3): la suma de las energías de las dos
mitades, en ese orden, es la energía del tramo completo. $\blacksquare$

**Teorema.** $\mathrm{energiaParalela}(s, u) = \mathrm{aux}(0, n) = E(0, n) = \mathrm{energiaSecuencial}(s)$
para todo sistema y todo umbral $u$; termina por el mismo argumento de la sección 4 (cada partición produce
tramos estrictamente menores y no vacíos).

**Sobre el punto flotante** (como se anticipó en la sección 0). En los reales, la asociatividad de
la suma es exacta y el argumento anterior cierra tal cual. En `Double` cada `+` redondea, de modo que el
resultado es la suma **con el agrupamiento que hizo el programa**, y agrupar de otra manera puede cambiar el
último bit: lo que el reparto conserva es la igualdad **hasta el redondeo**, no la bit a bit. Por eso las
pruebas comparan con tolerancia (`cerca`, $10^{-6}$) y no con `==`. En el sistema de ejemplo del informe de
proceso las dos agrupaciones dieron la misma cifra ejecutada, `23.250000000000004`: el árbol sumó
$5.500000000000001 + (0.7500000000000001 + 17.000000000000004)$ y el secuencial sumó de izquierda a derecha,
y en este caso el redondeo coincidió; eso es una coincidencia agradable, no una garantía.

### 6.2 `centroDeMasaParalelo` y la trampa del promedio

```Scala
def centroDeMasaParalelo(s: Sistema, umbral: Int): Vector2 = {
  @tailrec
  def hoja(i: Int, hasta: Int, ponderada: Vector2, masa: Double): (Vector2, Double) =
    if (i >= hasta) (ponderada, masa)
    else hoja(i + 1, hasta, ponderada + s(i).pos * s(i).masa, masa + s(i).masa)

  def aux(desde: Int, hasta: Int): (Vector2, Double) =
    if (hasta - desde <= umbral || hasta - desde <= 1)
      hoja(desde, hasta, Vector2(0, 0), 0.0)
    else {
      val m = desde + (hasta - desde) / 2
      val ((pi, mi), (pd, md)) = parallel(aux(desde, m), aux(m, hasta))
      (pi + pd, mi + md)
    }

  val (ponderada, masa) = aux(0, s.length)
  if (masa == 0.0) Vector2(0, 0)
  else Vector2(ponderada.x / masa, ponderada.y / masa)
}
```

Lo que viaja por el árbol **no es el promedio**: cada llamada `aux(d, h)` devuelve la **pareja de sumas** del
tramo, $\bigl(P(d, h),\ M(d, h)\bigr)$, y la combinación es componente a componente:

$$
(P_1, M_1) \oplus (P_2, M_2) \;=\; (P_1 + P_2,\; M_1 + M_2)
$$

- **Caso hoja:** `hoja(desde, hasta, Vector2(0, 0), 0.0)` es el recorrido de dos acumuladores de
  `centroDeMasaSecuencial` (sección 5.3) sobre el tramo, con el invariante
  $\mathrm{Inv}(i, w, M) \equiv i \le h \wedge w = \sum_{k=d}^{i-1} m_k r_k \wedge M = \sum_{k=d}^{i-1} m_k$;
  el estado inicial cumple con las sumas vacías, cada paso extiende ambas, y en $i = h$ devuelve
  $\bigl(P(d, h), M(d, h)\bigr)$.
- **Caso partición:** por hipótesis de inducción, las dos mitades devuelven sus parejas, y el resultado es
  $\bigl(P(d, m), M(d, m)\bigr) \oplus \bigl(P(m, h), M(m, h)\bigr)$. Como $+$ es asociativa y conmutativa en
  cada componente, $\oplus$ también lo es, y tiene neutro $((0, 0), 0)$: el orden en que terminen las mitades
  y el número de particiones no cambian el resultado.

$$
\bigl((P_1, M_1) \oplus (P_2, M_2)\bigr) \oplus (P_3, M_3)
= (P_1, M_1) \oplus \bigl((P_2, M_2) \oplus (P_3, M_3)\bigr)
= \Bigl(\sum_{k=d}^{h-1} m_k r_k,\; \sum_{k=d}^{h-1} m_k\Bigr) \;=\; \bigl(P(d, h), M(d, h)\bigr)
$$

$\blacksquare$

**La combinación es correcta para la especificación.** Al final, la función hace la misma división única de
`centroDeMasaSecuencial`: si $M(0, n) = 0$ devuelve el origen sin dividir (cubre el sistema vacío y el de
todas las masas en cero), y si no devuelve $\bigl(P(0,n)_x / M(0,n),\ P(0,n)_y / M(0,n)\bigr)$, que por la
definición de $P$ y $M$ es $CM(0, n)$. Termina por el mismo argumento del caso partición.

**Por qué el promedio no se puede llevar por el árbol.** Si cada mitad devolviera su promedio
$C(d, h) = P(d, h)/M(d, h)$ y la raíz combinara promediando los dos, el resultado sería

$$
\frac{C(d, m) + C(m, h)}{2}
= \frac{1}{2}\Bigl(\frac{P(d, m)}{M(d, m)} + \frac{P(m, h)}{M(m, h)}\Bigr)
\;\stackrel{?}{=}\; \frac{P(d, m) + P(m, h)}{M(d, m) + M(m, h)}
$$

El lado izquierdo da a las dos mitades **el mismo peso** $\tfrac12$; el lado derecho, que es la
especificación, las pondera con su masa. La igualdad falla en cuanto $M(d, m) \neq M(m, h)$: con promedios,
la operación de combinación no es asociativa y el resultado depende de cómo se agrupe el árbol, así que el
reparto ya no está justificado. Contraejemplo **ejecutado**, con el sistema de cinco partículas del informe
de proceso y umbral 2: la mitad $[0, 2)$ tiene centro $(2/3,\ 2/3)$ con masa $3$ y la mitad $[2, 5)$ tiene
centro $(7/9,\ 14/9)$ con masa $9$; el promedio de los dos promedios da $(13/18,\ 10/9) \approx
(0.7222,\ 1.1111)$, mientras que el centro correcto es $((9, 16))/12 = (0.75,\ 1.3333)$, que es lo que la
versión de sumas entrega. La prueba
`centroDeMasaParalelo: con mitades de masas 3 y 9, la suma da el centro y el promedio de promedios no`
ejecuta exactamente este caso, y las demás pruebas de la suite propia verifican la igualdad con la
secuencial sobre el mismo sistema y sobre un sistema aleatorio impar de 777 partículas, con umbrales de 1 al
tamaño del sistema.

## 7. Corrección del punto 4

**`trabajo(n) = n`.** Es la definición del enunciado: una operación por partícula; repartir el trabajo no lo
reduce, y por eso la fórmula de estimación lo conserva entero.

**`profundidad(n, u)`.** Con el mismo criterio de guarda de `pasoParalelo`:

```Scala
def profundidad(n: Int, umbral: Int): Int =
  if (n <= umbral || n <= 1) n
  else 1 + profundidad(n - n / 2, umbral)
```

Es la definición que pide el enunciado: el número de veces que se parte a la mitad, más el tramo que queda
sin partir. El lazo con el árbol de `pasoParalelo` es directo: cada llamada del árbol que particiona suma 1
en el camino, y la mitad que se sigue partiendo es la **mayor**, $n - n/2 = \lceil n/2 \rceil$, porque con
$m = d + \lfloor (h - d)/2 \rfloor$ el tramo derecho $[m, h)$ tiene $h - m = \lceil (h - d)/2 \rceil$
elementos y es el que fija el camino más largo. El tope $n \le 1$ es el mismo de la guarda: con $u \le 0$
vale 1, igual que en `pasoParalelo`, y evita el contraejemplo de terminación de la sección 4.

*Verificación con valores ejecutados:* $\mathrm{prof}(1024, 1024) = 1024$ (el umbral cubre todo, no se
parte), $\mathrm{prof}(1024, 512) = 1 + 512 = 513$ (una partición y el tramo que queda),
$\mathrm{prof}(1024, 1) = 10 + 1 = 11$ (con $1024 = 2^{10}$, diez particiones y un elemento) y
$\mathrm{prof}(5, 2) = 1 + 1 + 2 = 4$, que coincide con el árbol de `pasoParalelo(e5, 2.0, 2)` del informe de
proceso, que se ejecutó con 3 hojas y 2 particiones: el camino más largo cruza dos particiones y termina en
una hoja de tamaño 2.

**`tiempoEstimado(n, p, u) = prof(n, u) + n / p`.** La suma de la profundidad — el camino del cálculo que no
se puede acortar con más procesadores — y del trabajo repartido entre $p$. Con $p = 1$ vale
$prof(n, u) + n \ge trabajo(n)$: sin procesadores extra no hay ganancia. Al crecer $p$, el segundo término
baja monótonamente y la profundidad queda como piso, así que la estimación mejora al agregar procesadores y
nunca baja de $prof(n, u)$. *Verificación ejecutada:*
$\mathrm{tiempoEstimado}(1024, 4, 1) = 11 + 1024/4 = 267.0$, el valor del enunciado.

## 8. Qué cubre este informe y qué falta

Los puntos que el enunciado pide en concreto quedan así:

- **Punto 1** (secciones 3 y 4): que **partir el sistema no pierde ni repite partículas** (lema de
  partición y caso partición de la sección 4), que la operación con que se combinan los parciales
  (**concatenación**) **es asociativa**, y que la versión paralela da lo mismo que la secuencial, de forma
  **exacta**.
- **Punto 2** (sección 5): las tres magnitudes secuenciales, con su invariante de recorrido.
- **Punto 3** (sección 6): la operación con que se combinan los parciales (**suma**, y **parejas de sumas**
  para el centro de masa) **es asociativa**, que es por lo que se puede repartir, y el **promedio no lo es**:
  promediar promedios da otra cosa cuando las mitades no pesan igual, con el contraejemplo ejecutado. El
  tratamiento del redondeo de la suma en `Double`, que estaba anotado como pendiente, quedó en la sección
  6.1: la igualdad entre versiones vale hasta el redondeo, y las pruebas comparan con tolerancia.
- **Punto 4** (sección 7): `trabajo`, `profundidad` y `tiempoEstimado` contra sus definiciones, con la
  correspondencia entre la profundidad y el camino más largo del árbol real de `pasoParalelo`.
- **Punto 5** (sección 9): `aceleracion` como cociente de tiempos, `umbralConveniente` devuelve un candidato
  que minimiza el tiempo medido, el resultado no depende del umbral y la fila de control ejecuta la misma
  llamada que `pasoSecuencial`.

## 9. Corrección del punto 5

**Especificación.** Sea $T_s$ el tiempo medido de `pasoSecuencial` y $T_p(u)$ el de `pasoParalelo` con umbral $u$. Se pide:

$$
\mathrm{aceleracion}(T_s, T_p) = \frac{T_s}{T_p}, \qquad
\mathrm{umbralConveniente}(s, dt, U) =
\begin{cases}
n & \text{si } U = [\,] \\
\arg\min_{u \in U} \; T_p(u) & \text{si } U \ne [\,]
\end{cases}
$$

con $n$ el tamaño del sistema y $U$ la lista de candidatos.

### 9.1 `aceleracion`

```Scala
def aceleracion(tSecuencial: Double, tParalelo: Double): Double =
  tSecuencial / tParalelo
```

Es la definición del enunciado, sin ningún caso adicional. Con $T_p > 0$ se cumplen las propiedades que las pruebas verifican:

- $T_s = T_p \Rightarrow S = 1$.
- $T_p < T_s \Rightarrow S > 1$, y $T_p > T_s \Rightarrow S < 1$.
- Escalar los dos tiempos por $c > 0$ no cambia el cociente: $\dfrac{c\,T_s}{c\,T_p} = \dfrac{T_s}{T_p}$.
- $T_s = 0$ con $T_p > 0$ da $0$.

Si $T_p = 0$ la división en `Double` da `Infinity` (con $T_s > 0$) o `NaN` (con $T_s = 0$). Esto no ocurre en la práctica, porque ScalaMeter
mide tiempos estrictamente positivos sobre un millón de partículas, así que no se agrega una rama para ese caso.

### 9.2 `umbralConveniente`

```Scala
def umbralConveniente(s: Sistema, dt: Double, umbrales: List[Int]): Int =
  umbrales
    .map(u => (u, medir(3)(pasoParalelo(s, dt, u))))
    .minByOption(_._2)
    .map(_._1)
    .getOrElse(s.length)
```

Sea $t_u$ el tiempo que `medir` obtiene para el candidato $u$. Se argumenta por casos sobre $U$.

- **$U = [\,]$.** `map` sobre la lista vacía da la lista vacía, y no se mide nada. `minByOption` de una lista vacía es `None`, `None.map(_._1)` es `None` y `None.getOrElse(s.length)` es $n$. Es lo que el enunciado pide: un umbral igual al tamaño no reparte nada. La función es total: usar `minByOption` evita la excepción que lanzaría `minBy` sobre una lista vacía.
- **$U \ne [\,]$.** `map` conserva el orden y no pierde ni repite elementos, así que la lista intermedia es $[(u_1, t_{u_1}), \dots, (u_k, t_{u_k})]$, con un elemento por candidato. `minByOption(_._2)` devuelve `Some` de un elemento $(u^*, t_{u^*})$ **de esa lista**, tal que $t_{u^*} \le t_{u_j}$ para todo $j$. `.map(_._1)` extrae $u^*$ y `getOrElse` lo entrega sin usar el valor por defecto. Por tanto:
  1. **Pertenencia:** $u^* \in U$. La prueba `devuelve uno de los candidatos` lo verifica.
  2. **Minimalidad:** $u^*$ minimiza el tiempo medido entre los candidatos. Con un solo candidato la lista tiene un elemento y se devuelve ese mismo.
  3. **Empates:** si varios candidatos alcanzan el mínimo, la biblioteca devuelve el primero, así que el resultado está bien definido.
- **Terminación.** `map` y `minByOption` recorren una lista finita, y cada `medir` ejecuta un número finito de repeticiones. No hay recursión.

**Qué se garantiza y qué no.** La minimalidad es sobre los tiempos **medidos**, no sobre los tiempos «verdaderos» de la máquina:
dos corridas pueden elegir candidatos distintos cuando los tiempos son parecidos. Por eso `medir` toma la mediana de
varias muestras, que es un elemento de la propia muestra ordenada y es poco sensible a una corrida atípica. Esto es una propiedad
de la medición y no del algoritmo, y por eso las pruebas no comparan tiempos.

### 9.3 La elección del umbral no cambia el resultado

Lo que mide `umbralConveniente` es `pasoParalelo(s, dt, u)`. Por el teorema de la sección 4, para todo $u$:

$$
\mathrm{pasoParalelo}(s, dt, u) = \mathrm{pasoSecuencial}(s, dt)
$$

Los candidatos solo se diferencian en **cuánto tardan**, no en qué calculan. Por eso elegir uno cualquiera es correcto, y
elegir el más rápido es una decisión de rendimiento.

### 9.4 La fila de control

Sea $u \ge n$. En `aux(0, n)` se cumple $n - 0 \le u$, así que es una hoja y la función devuelve
`pasoSegmento(s, dt, 0, n)`, que es exactamente el cuerpo de `pasoSecuencial`. No se llama a `parallel` ni a `++`.
Las dos versiones ejecutan la misma llamada, así que la diferencia de tiempos debe ser solo ruido de medición y la
aceleración debe quedar cerca de 1. Si no es así, la medición está contaminada, por ejemplo por poca memoria o por el
recolector de basura, y no hay un error de cálculo. Esto se observó en las mediciones: con $n = 1.000.000$ el control dio 0.98 y 0.96,
y con $n = 2.000.000$ dio 1.09 y 0.56, por lo que esas últimas se descartaron (informe de paralelización, sección 3).

### 9.5 Resumen del punto 5

- `aceleracion` es el cociente de tiempos.
- `umbralConveniente` devuelve un candidato de la lista que minimiza el tiempo medido, y $n$ con la lista vacía.
- El resultado de `pasoParalelo` no depende del umbral elegido.
- La fila de control ejecuta la misma llamada que `pasoSecuencial`, y su aceleración debe dar cerca de 1.