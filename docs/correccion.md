# Informe de Corrección — Taller 4: Simulación de Partículas en Paralelo

## 1. Fundamentación Teórica y Corrección Formal

Un programa funcional $P_f : A \to B$ es correcto con respecto a una especificación matemática $f : A \to B$ si se cumple:

$$\forall a \in A : P_f(a) = f(a)$$

---

## 2. Punto 2: Magnitudes Globales (Secuencial)

### 2.1. Especificación Matemática

Sea un sistema de partículas $S = \langle p_0, p_1, \ldots, p_{n-1} \rangle$, donde cada partícula $p_i$ posee:
* Posición: $\vec{r}_i = (x_i, y_i) \in \mathbb{R}^2$.
* Velocidad: $\vec{v}_i = (v_{x,i}, v_{y,i}) \in \mathbb{R}^2$ con norma $\|\vec{v}_i\| = \sqrt{v_{x,i}^2 + v_{y,i}^2}$.
* Masa: $m_i \in \mathbb{R}_{\geq 0}$.

Se definen formalmente las siguientes funciones:

1. **Energía Cinética Total ($E$)**:
   $$E(S) = \sum_{i=0}^{n-1} \frac{1}{2} m_i \|\vec{v}_i\|^2$$
   Con $E(\emptyset) = 0$.

2. **Masa Total ($M$)**:
   $$M(S) = \sum_{i=0}^{n-1} m_i$$
   Con $M(\emptyset) = 0$.

3. **Centro de Masa ($\vec{R}_{cm}$)**:
   $$\vec{R}_{cm}(S) = \begin{cases} (0, 0) & \text{si } M(S) = 0 \\ \frac{1}{M(S)} \sum_{i=0}^{n-1} m_i \vec{r}_i & \text{si } M(S) > 0 \end{cases}$$

---

### 2.2. Demostración de Corrección

#### A. Corrección de `energiaSecuencial`
La implementación en Scala es:
```scala
def energiaSecuencial(s: Sistema): Double =
  s.map(p => 0.5 * p.masa * (p.vel.norma * p.vel.norma)).sum
```

* **Lema 1 (Evaluación escalar por partícula)**:
  Para cada partícula $p_i$, `p.vel.norma * p.vel.norma` evalúa $(\sqrt{v_{x,i}^2 + v_{y,i}^2})^2 = v_{x,i}^2 + v_{y,i}^2 = \|\vec{v}_i\|^2$.
  Al multiplicar por `0.5 * p.masa`, se obtiene exactamente $\frac{1}{2} m_i \|\vec{v}_i\|^2$.
* **Lema 2 (Preservación del homomorfismo aditivo)**:
  La operación `.sum` sobre el vector resultante efectúa $\sum_{i=0}^{n-1} e_i$. Para el caso base $n = 0$ (sistema vacío), la suma sobre una secuencia vacía retorna neutro aditivo $0.0$, coincidiendo con la especificación $E(\emptyset) = 0$.
  Por lo tanto, `energiaSecuencial(s)` computa exactamente $E(S)$.

#### B. Corrección de `masaTotal`
La implementación es:
```scala
def masaTotal(s: Sistema): Double =
  s.map(_.masa).sum
```
* Para cualquier sistema $S$, la proyección `s.map(_.masa)` genera la secuencia $\langle m_0, m_1, \ldots, m_{n-1} \rangle$.
* La invocación de `.sum` calcula la sumatoria $\sum_{i=0}^{n-1} m_i$. Para $S = \emptyset$, `.sum` retorna $0.0 = M(\emptyset)$.

#### C. Corrección de `centroDeMasaSecuencial`
La implementación es:
```scala
def centroDeMasaSecuencial(s: Sistema): Vector2 = {
  val mTotal = masaTotal(s)
  if (mTotal == 0.0) Vector2(0.0, 0.0)
  else {
    val sumaPonderada = s.foldLeft(Vector2(0.0, 0.0)) { (acc, p) =>
      acc + (p.pos * p.masa)
    }
    Vector2(sumaPonderada.x / mTotal, sumaPonderada.y / mTotal)
  }
}
```

* **Caso 1: $M(S) = 0$ (sin masa o sistema vacío)**:
  La condición `if (mTotal == 0.0)` se evalúa como verdadera y retorna `Vector2(0.0, 0.0)`. Esto satisface directamente la cláusula de frontera de la definición matemática y previene divisiones por cero.
* **Caso 2: $M(S) > 0$**:
  Demostramos por inducción que tras procesar el prefijo $S_k = \langle p_0, \ldots, p_{k-1} \rangle$, el acumulador de `foldLeft` equivale a $\vec{P}_k = \sum_{i=0}^{k-1} m_i \vec{r}_i$:
  - **Base ($k = 0$)**: El acumulador inicial es `Vector2(0.0, 0.0)` $= \sum_{i=0}^{-1} m_i \vec{r}_i = \vec{0}$.
  - **Paso inductivo ($k \to k+1$)**: Si el acumulador contiene $\vec{P}_k$, al procesar $p_k$ se suma $\vec{P}_k + (p_k.\text{pos} \cdot p_k.\text{masa}) = \sum_{i=0}^{k-1} m_i \vec{r}_i + m_k \vec{r}_k = \sum_{i=0}^{k} m_i \vec{r}_i = \vec{P}_{k+1}$.
  - Al completar los $n$ elementos, `sumaPonderada` contiene $\sum_{i=0}^{n-1} m_i \vec{r}_i$.
  - Finalmente, al dividir las componentes $x$ e $y$ entre $mTotal$, se obtiene:
    $$\left( \frac{\sum m_i x_i}{M}, \frac{\sum m_i y_i}{M} \right) = \frac{1}{M} \sum_{i=0}^{n-1} m_i \vec{r}_i = \vec{R}_{cm}(S)$$

---

### 2.3. Propiedad de Asociatividad y Reparto
Una propiedad esencial exigida en el taller es que la suma vectorial y escalar es **asociativa y conmutativa**:

$$\sum_{p \in A \cup B} m_p \vec{r}_p = \left( \sum_{p \in A} m_p \vec{r}_p \right) + \left( \sum_{p \in B} m_p \vec{r}_p \right)$$
$$M(A \cup B) = M(A) + M(B)$$

Por esta razón, la suma ponderada y la masa se pueden repartir en mitades arbitrarias sin alterar el resultado. En contraste, el promedio directo de centros de masa parciales no es asociativo cuando las masas de los segmentos difieren:

$$\frac{\vec{R}_{cm}(A) + \vec{R}_{cm}(B)}{2} \neq \frac{M_A \vec{R}_{cm}(A) + M_B \vec{R}_{cm}(B)}{M_A + M_B} = \vec{R}_{cm}(A \cup B)$$

Nuestra implementación acumula explícitamente la suma ponderada y la masa total, garantizando la consistencia tanto en la versión secuencial como en su posterior extensión concurrente.
