

# Simulación de partículas repartida con `parallel`

## El cálculo

`pasoSegmento` recorre un tramo del sistema y actualiza la posición de cada partícula mediante la función `mover`. Para construir el arreglo resultante se utiliza `Array.tabulate`.

```scala
def mover(p: Particula, dt: Double): Particula =
  p.copy(pos = p.pos + p.vel * dt)

def pasoSegmento(s: Sistema, dt: Double, desde: Int, hasta: Int): Sistema =
  Array.tabulate(hasta - desde)(i => mover(s(desde + i), dt))
```

En notación matemática, la posición de cada partícula después de un intervalo de tiempo se calcula mediante:

```math
\vec{r}_i(t+\Delta t)=\vec{r}_i(t)+\vec{v}_i\Delta t
```

El cálculo se realiza sobre el tramo entre `desde` incluido y `hasta` excluido, manteniendo la masa y la velocidad de cada partícula.

Sobre ese cálculo existen dos versiones. La versión secuencial procesa todo el sistema en un solo tramo, mientras que la versión paralela divide el sistema por la mitad mientras su tamaño supere el umbral establecido.

```scala
def pasoSecuencial(s: Sistema, dt: Double): Sistema =
  pasoSegmento(s, dt, 0, s.length)

def pasoParalelo(s: Sistema, dt: Double, umbral: Int): Sistema = {
  def avanzar(desde: Int, hasta: Int): Sistema =
    if (hasta - desde <= umbral || hasta - desde <= 1)
      pasoSegmento(s, dt, desde, hasta)
    else {
      val mitad = desde + (hasta - desde) / 2
      val (izquierda, derecha) = parallel(
        avanzar(desde, mitad),
        avanzar(mitad, hasta))
      izquierda ++ derecha
    }

  avanzar(0, s.length)
}
```

La diferencia entre ambas versiones es la estrategia de ejecución. Mientras que `pasoSecuencial` procesa todas las partículas de manera consecutiva, `pasoParalelo` distribuye el trabajo mediante `parallel` y posteriormente concatena los resultados parciales utilizando el operador `++`.

Ambas versiones producen un sistema con las mismas partículas actualizadas y conservan su orden original.


## La estrategia de reparto

El tramo se parte por la mitad en `mitad`, y cada parte se resuelve de manera independiente, ya que el movimiento de una partícula no depende de las demás. Ningún índice aparece en ambos tramos y ninguno queda por fuera, porque el primero va desde `desde` hasta `mitad` y el segundo desde `mitad` hasta `hasta`, excluyendo el límite superior de cada tramo.

Cuando el tamaño del tramo es menor o igual al umbral, o contiene como máximo una partícula, ya no se divide y se resuelve secuencialmente mediante `pasoSegmento`.

Lo que combina los resultados parciales es la concatenación de arreglos mediante el operador `++`, que es una operación asociativa:

```math
(A \mathbin{++} B) \mathbin{++} C = A \mathbin{++} (B \mathbin{++} C)
```

Por lo tanto, el procesamiento de un tramo completo equivale a procesar sus dos mitades y concatenar los resultados:

```math
P_{[d,h)} = P_{[d,m)} \mathbin{++} P_{[m,h)}
```

Donde `d` representa el índice inicial, `m` el punto medio y `h` el índice final del tramo.

El orden en que terminan las tareas no modifica el resultado, siempre que se concatene primero el tramo izquierdo y después el derecho. De esta manera se conserva el orden original de las partículas.

El árbol de llamados para un sistema de ocho partículas con umbral 2 queda así, con las hojas resolviendo en secuencial:

```mermaid
graph TD
    A["0 a 8: parallel"] --> B["0 a 4: parallel"]
    A --> C["4 a 8: parallel"]
    B --> D["0 a 2: pasoSegmento"]
    B --> E["2 a 4: pasoSegmento"]
    C --> F["4 a 6: pasoSegmento"]
    C --> G["6 a 8: pasoSegmento"]
```

## Cómo se tomaron los tiempos

Cada cifra se obtiene mediante ScalaMeter, la librería utilizada en el curso para medir el rendimiento de los algoritmos. La expresión `withWarmer(new Warmer.Default) measure { ... }` realiza un calentamiento previo de la máquina virtual antes de tomar las mediciones, reduciendo la influencia de las optimizaciones iniciales del compilador.

Cada tiempo registrado corresponde a la mediana de cinco mediciones, con el propósito de reducir la influencia de valores extremos.

```scala
import org.scalameter._

def medir[A](repeticiones: Int)(cuerpo: => A): Double = {
  val muestras = Vector.fill(repeticiones) {
    (withWarmer(new Warmer.Default) measure { cuerpo }).value
  }
  muestras.sorted.apply(muestras.length / 2)
}
```

Para calcular la aceleración se divide el tiempo de ejecución secuencial entre el tiempo de ejecución paralelo:

```scala
def aceleracion(tSecuencial: Double, tParalelo: Double): Double =
  tSecuencial / tParalelo
```

En notación matemática:

```math
S=\frac{T_{secuencial}}{T_{paralelo}}
```

La dependencia ya viene incluida en `app/build.gradle` del repositorio:

```gradle
implementation 'com.storm-enroute:scalameter-core_2.13:0.21'
```

Las dos versiones actualizan las partículas mediante la misma función de movimiento. La diferencia entre sus tiempos corresponde a la estrategia de reparto y al costo de combinar los resultados parciales.

La medición con el umbral igual al tamaño del sistema sirve como control, debido a que `pasoParalelo` no realiza ninguna división y ejecuta directamente `pasoSegmento`, por lo que se espera un tiempo similar al de `pasoSecuencial`.

La máquina utilizada dispone de 12 procesadores lógicos. Las pruebas se realizan con sistemas de 1.000.000 y 1.500.000 partículas, utilizando un intervalo de tiempo de 0.5 y cuatro umbrales diferentes para cada tamaño del sistema.

Las partículas se generan mediante `Array.tabulate`, con posiciones y velocidades determinadas por su índice y masas entre 1 y 3.

Todos los tiempos obtenidos se expresan en milisegundos.


## Las mediciones

Las pruebas se realizaron en una máquina con 12 procesadores lógicos, utilizando dos tamaños del sistema y diferentes umbrales de paralelización. Los tiempos están expresados en milisegundos.

Con el sistema fijo en 1.000.000 de partículas, según el umbral:

| **Umbral** | **Niveles de división** | **Secuencial (ms)** | **Paralela (ms)** | **Aceleración** |
| ---------- | ----------------------- | ------------------- | ----------------- | --------------- |
| 1 000 000  | 0                       | 25,9797             | 27,0795           | 0,959           |
| 125 000    | 3                       | 25,9797             | 16,4118           | 1,583           |
| 15 625     | 6                       | 25,9797             | 14,8497           | **1,750**       |
| 976        | 11                      | 25,9797             | 15,6280           | 1,662           |

El tiempo secuencial de referencia de esta primera tabla es de 25,9797 ms.

Con el sistema fijo en 1.500.000 de partículas, según el umbral:

| **Umbral** | **Niveles de división** | **Secuencial (ms)** | **Paralela (ms)** | **Aceleración** |
| ---------- | ----------------------- | ------------------- | ----------------- | --------------- |
| 1 500 000  | 0                       | 35,5215             | 86,8666           | **0,409**       |
| 187 500    | 3                       | 35,5215             | 121,7970          | 0,292           |
| 23 437     | 7                       | 35,5215             | 99,7153           | 0,356           |
| 1 464      | 11                      | 35,5215             | 110,5460          | 0,321           |

El tiempo secuencial de referencia de esta segunda tabla es de 35,5215 ms.

En la primera tabla, el mejor resultado se obtiene con un umbral de 15.625 partículas, alcanzando una aceleración de 1,75 y una reducción del tiempo de ejecución de aproximadamente el 42,84 %.

En la segunda tabla, ninguna configuración logra superar el rendimiento de la versión secuencial. Incluso la medición de control, donde el umbral es igual al tamaño del sistema, presenta una diferencia considerable.

Este comportamiento podría estar relacionado con la gestión de memoria, la recolección de basura de la máquina virtual o las condiciones de ejecución del equipo. Sin embargo, se necesitan mediciones adicionales para determinar la causa exacta.


## Lo que dicen los números

El control para el sistema de 1 000 000 de partículas responde aproximadamente como se esperaba: con un umbral de 1 000 000, la aceleración es de 0,959, obteniendo un tiempo similar al de la versión secuencial.

Sin embargo, para el sistema de 1 500 000 de partículas, la medición de control presenta una aceleración de 0,409. Esta diferencia indica que pudieron intervenir factores externos a la estrategia de paralelización, como la gestión de memoria o la recolección de basura de la máquina virtual.

Para el sistema de 1 000 000 de partículas, la paralelización mejora el rendimiento cuando se utiliza un umbral adecuado. Con un umbral de 125 000, la aceleración alcanza 1,583, mientras que con un umbral de 15 625 aumenta hasta 1,750.

El umbral que más conviene no es necesariamente el más pequeño. El mejor tiempo se obtiene con un umbral de 15 625, correspondiente a seis niveles de división y 64 tramos para 12 procesadores lógicos. Al reducir el umbral hasta 976, la aceleración disminuye a 1,662, posiblemente debido al costo adicional de crear tareas y concatenar los resultados parciales.

Para el sistema de 1 500 000 de partículas, ninguna de las configuraciones paralelas supera el rendimiento de la versión secuencial. Esto demuestra que aumentar el tamaño del sistema no garantiza una mejora en el rendimiento paralelo.

La estimación teórica de trabajo y profundidad permite estudiar el comportamiento del algoritmo en condiciones idealizadas. Sin embargo, los tiempos reales también incluyen el costo de crear tareas, concatenar arreglos, reservar memoria y acceder a los recursos compartidos del equipo. No se dispone de valores numéricos de aceleración estimada para realizar una comparación directa.

Con la mejor aceleración medida de 1,7495 sobre 12 procesadores lógicos, la ley de Amdahl permite estimar la fracción serial efectiva del cálculo, donde p representa el número de procesadores:

```math
S(p)=\frac{1}{f+\frac{1-f}{p}}
```

Despejando la fracción serial:

```math
f=\frac{\frac{1}{S}-\frac{1}{p}}{1-\frac{1}{p}}
```

Sustituyendo los valores obtenidos:

```math
f=\frac{\frac{1}{1.7495}-\frac{1}{12}}{1-\frac{1}{12}}\approx 0.533
```

La fracción serial efectiva corresponde aproximadamente al 53,3 % del cálculo, mientras que el 46,7 % restante representa la fracción paralelizable según este modelo.

La aceleración máxima teórica, manteniendo constante esta fracción, es:

```math
S_{\max}=\frac{1}{f}\approx 1.88
```

| p | 2 | 4 | 8 | 12 | 16 | 32 | ∞ |
|---|---:|---:|---:|---:|---:|---:|---:|
| Aceleración máxima estimada | 1,30 | 1,54 | 1,69 | 1,75 | 1,78 | 1,83 | 1,88 |

Pasar de 12 a 32 procesadores aumentaría la aceleración estimada de 1,75 a 1,83, lo que representa una mejora pequeña según el modelo de Amdahl.

En conclusión, la paralelización permite mejorar el rendimiento de la simulación cuando se selecciona un umbral adecuado. Sin embargo, los costos asociados a la división del sistema, la creación de tareas, la concatenación de arreglos y la gestión de memoria pueden limitar sus beneficios. Por esta razón, es necesario realizar pruebas experimentales para determinar la configuración más conveniente.
