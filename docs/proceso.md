# Informe de Proceso — Taller 4: Simulación de Partículas en Paralelo

## 1. Punto 1: Un paso de la simulación
*(Implementado por DilanG26)*
- `mover(p: Particula, dt: Double)`: Actualiza la posición de una partícula según $\vec{r}_{nuevo} = \vec{r} + \vec{v} \cdot dt$. La velocidad y la masa permanecen invariantes.
- `pasoSecuencial(s: Sistema, dt: Double)`: Aplica `pasoSegmento` sobre el rango total $[0, n)$.
- `pasoSegmento(s: Sistema, dt: Double, desde: Int, hasta: Int)`: Transforma funcionalmente el subrango $[\text{desde}, \text{hasta})$ generando un nuevo arreglo con cada partícula movida.
- `pasoParalelo(s: Sistema, dt: Double, umbral: Int)`: Divide recursivamente el sistema en dos mitades con `parallel` mientras el tamaño del segmento sea mayor al umbral dado; por debajo de dicho umbral, resuelve el tramo en secuencial con `pasoSegmento`.

---

## 2. Punto 2: Magnitudes Globales (Secuencial)
*(Implementado por iRenly / Georsuans Giraldo)*

### 2.1. Definición y Enfoque Funcional
El Punto 2 calcula las magnitudes físicas fundamentales que resumen el estado global del sistema de partículas, garantizando pureza referencial, inmutabilidad y ausencia de efectos colaterales:

1. **`energiaSecuencial(s: Sistema): Double`**:
   Calcula la energía cinética total del sistema mediante la suma de la energía cinética de cada cuerpo:
   $$E = \sum_{p \in s} \frac{1}{2} m_p \|\vec{v}_p\|^2$$
   Se implementa transformando funcionalmente cada partícula con `map` a su energía escalar $\frac{1}{2} m v^2$ y agregando los valores con `.sum`. Si el sistema está vacío, la suma sobre la colección vacía retorna naturalmente `0.0`.

2. **`masaTotal(s: Sistema): Double`**:
   Calcula la masa escalar agregada de todo el sistema:
   $$M = \sum_{p \in s} m_p$$
   Se implementa proyectando la masa de cada partícula mediante `s.map(_.masa)` y calculando la suma escalar. Para un sistema sin partículas, retorna `0.0`.

3. **`centroDeMasaSecuencial(s: Sistema): Vector2`**:
   Calcula la posición promedio ponderada por la masa de todo el conjunto de partículas:
   $$\vec{R}_{cm} = \frac{1}{M} \sum_{p \in s} m_p \vec{r}_p$$
   - Si la masa total $M = 0$, retorna directamente el vector origen $\vec{0} = (0.0, 0.0)$, previniendo indeterminaciones o divisiones por cero.
   - Si $M > 0$, acumula vectorialmente la suma ponderada $\sum m_p \vec{r}_p$ mediante `foldLeft` partiendo del neutro aditivo `Vector2(0.0, 0.0)` y divide el vector acumulado entre la masa escalar total $M$.

### 2.2. Traza de Ejecución Paso a Paso
Consideremos el sistema físico $S = [p_1, p_2]$:
* $p_1$: $\vec{r}_1 = (0.0, 0.0)$, $\vec{v}_1 = (3.0, 4.0)$, $m_1 = 2.0$ (con $\|\vec{v}_1\| = \sqrt{3^2 + 4^2} = 5.0$).
* $p_2$: $\vec{r}_2 = (4.0, 0.0)$, $\vec{v}_2 = (0.0, 0.0)$, $m_2 = 2.0$ (con $\|\vec{v}_2\| = 0.0$).

#### Traza de `energiaSecuencial(S)`:
1. Elemento 0 ($p_1$): $\frac{1}{2} \cdot 2.0 \cdot (5.0)^2 = 1.0 \cdot 25.0 = 25.0$.
2. Elemento 1 ($p_2$): $\frac{1}{2} \cdot 2.0 \cdot (0.0)^2 = 0.0$.
3. Suma agregada: $25.0 + 0.0 = 25.0$.

#### Traza de `masaTotal(S)`:
1. Elemento 0 ($p_1$): $m_1 = 2.0$.
2. Elemento 1 ($p_2$): $m_2 = 2.0$.
3. Suma agregada: $2.0 + 2.0 = 4.0$.

#### Traza de `centroDeMasaSecuencial(S)`:
1. Cálculo de $M = \text{masaTotal}(S) = 4.0 \neq 0.0$.
2. Inicialización del acumulador en `foldLeft`: `acc = Vector2(0.0, 0.0)`.
3. Paso 1 ($p_1$): `acc = Vector2(0.0, 0.0) + (Vector2(0.0, 0.0) * 2.0) = Vector2(0.0, 0.0)`.
4. Paso 2 ($p_2$): `acc = Vector2(0.0, 0.0) + (Vector2(4.0, 0.0) * 2.0) = Vector2(8.0, 0.0)`.
5. Normalización final: `Vector2(8.0 / 4.0, 0.0 / 4.0) = Vector2(2.0, 0.0)`.

### 2.3. Diagrama de Flujo del Proceso
```mermaid
flowchart TD
    A["Sistema S: Array[Particula]"] --> B["Calcular masaTotal(S)"]
    B --> C{"¿masaTotal == 0.0?"}
    C -- Sí --> D["Retornar Vector2(0.0, 0.0)"]
    C -- No --> E["foldLeft con neutro Vector2(0,0)"]
    E --> F["Acumular: acc + (pos * masa)"]
    F --> G["Dividir componentes entre masaTotal"]
    G --> H["Vector2(x_cm, y_cm)"]
```
# Punto 3: las mismas magnitudes en paralelo

## 1. `energiaParalela`

La función recibe el sistema de partículas y un umbral. Primero establece un umbral mínimo de 1 para garantizar que la recursión avance.

La función auxiliar `energia(desde, hasta)` trabaja sobre un segmento del arreglo:

1. Si la cantidad de partículas del segmento es menor o igual al umbral, recorre sus índices y calcula la energía cinética de cada partícula. Luego suma esos valores.
2. Si el segmento supera el umbral, calcula el índice medio y divide el segmento en dos partes: `[desde, mitad)` y `[mitad, hasta)`.
3. Las dos llamadas recursivas se ejecutan mediante `parallel`.
4. Cuando ambas terminan, sus energías parciales se combinan mediante una suma.

Finalmente, se llama a la función auxiliar con el segmento completo, desde `0` hasta `s.length`.

### Ejemplo de ejecución

Considérese un sistema de ocho partículas y un umbral igual a dos.

```mermaid
graph TD
    A["energia(0, 8)"] -->|parallel| B["energia(0, 4)"]
    A -->|parallel| C["energia(4, 8)"]
    B -->|parallel| D["energia(0, 2)"]
    B -->|parallel| E["energia(2, 4)"]
    C -->|parallel| F["energia(4, 6)"]
    C -->|parallel| G["energia(6, 8)"]
    D --> H["Cálculo secuencial"]
    E --> I["Cálculo secuencial"]
    F --> J["Cálculo secuencial"]
    G --> K["Cálculo secuencial"]
```

Cada hoja calcula la energía de su segmento. Los resultados suben por el árbol y se suman hasta obtener la energía total.

## 2. `centroDeMasaParalelo`

Esta función utiliza una estrategia similar de división por mitades, pero cada segmento devuelve una pareja:

- Un `Vector2` con la suma de las posiciones ponderadas por sus masas.
- Un `Double` con la masa total del segmento.

En el caso base, `foldLeft` recorre el segmento y acumula ambos valores. Cuando el segmento supera el umbral, se divide en dos y las llamadas se ejecutan con `parallel`.

Al terminar, se combinan las parejas sumando sus vectores ponderados y sus masas. Después de obtener el resultado del sistema completo, se divide una sola vez la suma ponderada entre la masa total. Si la masa total es cero, se devuelve el origen `Vector2(0, 0)`.

### Ejemplo de combinación

Supóngase que las dos mitades producen:

```text
Mitad izquierda: (Vector2(6, 2), 3)
Mitad derecha:   (Vector2(8, 4), 5)
```

La combinación produce:

```text
(Vector2(14, 6), 8)
```

Y el centro de masa final es:

```text
Vector2(14 / 8, 6 / 8)
```

No se promedian los centros de las dos mitades, porque sus masas pueden ser diferentes.

## Punto 4: Trabajo y profundidad

### Definición del algoritmo - `trabajo`

```scala
def trabajo(n: Int): Int = n
```

La función `trabajo` recibe un entero `n` y devuelve el mismo valor. Representa el número total de operaciones, considerando una operación por partícula.

Recibe un parámetro:

- n: Número de partículas u operaciones.

### Definición del algoritmo - `profundidad`

```scala
def profundidad(n: Int, umbral: Int): Int = {
  require(n >= 0, "El # de operaciones no puede ser negativo.")
  require(umbral > 0, "El umbral debe ser minimo 1.")

  val piso = math.max(umbral, 1)

  def aux(n: Int): Int = {
    if (n <= piso) n
    else {
      val mitad = n / 2
      math.max(aux(mitad), aux(n - mitad)) + 1
    }
  }

  aux(n)
}
```

La función `profundidad` recibe el número de operaciones `n` y un `umbral`. Divide el problema en dos partes hasta que cada parte tenga un tamaño menor o igual al umbral. Devuelve la profundidad máxima de las divisiones, incluyendo el tramo final que queda sin dividir.

Recibe dos parámetros:

- n: Número de operaciones que se deben procesar.

- umbral: Tamaño máximo permitido para dejar de dividir.

### Definición del algoritmo - `tiempoEstimado`

```scala
def tiempoEstimado(n: Int, p: Int, umbral: Int): Double = {
  require(p > 0, "El # de procesadores debe ser mayor a 0.")
  profundidad(n, umbral) + (trabajo(n)/p)
}
```

La función tiempoEstimado recibe el número de operaciones `n`, la cantidad de procesadores `p` y un `umbral`. Devuelve una estimación del tiempo mediante la suma de la profundidad y el trabajo dividido entre los procesadores.

Recibe tres parámetros:

- `n`: Número de operaciones.

- `p`: Cantidad de procesadores disponibles.

- `umbral`: Tamaño máximo de cada tramo antes de dejar de dividir.

### Explicación paso a paso

#### `profundidad( )`

- Se comprueba que n sea mayor o igual a cero y que umbral sea mayor que cero mediante require.

- Se calcula piso con math.max(umbral, 1). Como el umbral ya debe ser positivo, piso tendrá el mismo valor que umbral.

- Se llama a aux(n) para iniciar el cálculo.

- Si n <= piso, se devuelve n, porque el tramo ya no necesita dividirse.

- Si n > piso, se calcula mitad = n / 2.

- Se realizan dos llamadas recursivas:

  - aux(mitad): procesa la primera mitad.

  - aux(n - mitad): procesa la segunda mitad.

- Se toma el mayor resultado de ambas llamadas y se suma uno para contar la división actual.

#### `tiempoEstimado( )`
- Se comprueba que `p` sea mayor que cero mediante require.

- Se llama a `profundidad(n, umbral)` para calcular la profundidad del proceso.

- Se llama a `trabajo(n)` para obtener el número total de operaciones.

- Se divide el `trabajo` entre `p`.

- Se suman ambos resultados y se devuelve el valor como Double.

Importante: Como `trabajo(n)` y `p` son de tipo Int, la división `trabajo(n)/p` es una división entera. Por lo tanto, se descarta la parte decimal antes de sumarla a la profundidad.

### Diagrama de llamados

```mermaid
sequenceDiagram
    participant M as Main
    participant T as tiempoEstimado
    participant P as profundidad
    participant A as aux
    participant W as trabajo

    M->>T: tiempoEstimado(n, p, umbral)
    T->>P: profundidad(n, umbral)
    P->>A: aux(n)

    loop Mientras n > piso
        A->>A: Calcular mitad = n / 2
        A->>A: aux(mitad)
        A->>A: aux(n - mitad)
        A->>A: Obtener máximo + 1
    end

    A-->>P: Profundidad calculada
    P-->>T: Resultado de profundidad

    T->>W: trabajo(n)
    W-->>T: n

    T->>T: Calcular trabajo(n) / p
    T->>T: Sumar profundidad y trabajo dividido
    T-->>M: Double
```
