## Punto 4: Trabajo y profundidad

### `trabajo( )`

La función devuelve el mismo número de operaciones que recibe.

#### Especificación:

Para cualquier entero n:

$$
trabajo(n) = n
$$

#### Demostración.

La función no realiza transformaciones sobre el valor recibido. Simplemente devuelve n.

Por lo tanto, el resultado coincide con el número total de operaciones definido en la especificación.

### `profundidad( )`

La función calcula la profundidad de las divisiones, sumando uno por cada nivel de división y considerando el tamaño del tramo cuando se alcanza el umbral.

#### Especificación.

Sea $D(n,u)$ la profundidad calculada para n operaciones y un umbral u:

$$
D(n,u) =
\begin{cases}
n & \text{si } n \leq u \
1 + \max(D(\lfloor n/2 \rfloor,u),\
\qquad D(\lceil n/2 \rceil,u))
& \text{si } n > u
\end{cases}
$$

Esta definición corresponde a la división entera realizada por n / 2 y a la segunda parte n - mitad.

#### Demostración por inducción sobre n.

**Caso base**: $n \leq u$.

La función ejecuta if (n <= piso) y devuelve n.

Como umbral es positivo, piso es igual a umbral. Por lo tanto, el resultado coincide con la especificación:

$$
D(n,u) = n
$$

#### Hipótesis de inducción (HI):

Suponemos que aux calcula correctamente la profundidad para cualquier valor menor que n.

**Paso inductivo**: $n > u$.

**La función calcula:** `val mitad = n / 2`

**Después realiza las llamadas:**

`aux(mitad)`
`aux(n - mitad)`

Como $n > u \geq 1$, ambas partes son menores que n. Por la hipótesis de inducción, las dos llamadas calculan correctamente sus respectivas profundidades.

**Finalmente, se ejecuta:**

`math.max(aux(mitad), aux(n - mitad)) + 1`

Se toma la mayor profundidad de las dos partes y se suma uno para contar la división actual.

Por lo tanto, el resultado coincide con la especificación.

**Terminación:**

En cada llamada recursiva, n se divide en dos partes menores que el valor original. Como ambas partes son enteros no negativos y menores que n, eventualmente llegan a ser menores o iguales al umbral.

En ese momento se alcanza el caso base y la recursión termina.

Por inducción, profundidad calcula correctamente el resultado para todo n no negativo y umbral positivo.

### `tiempoEstimado( )`

La función suma la profundidad del proceso y el trabajo distribuido entre los procesadores disponibles.

#### Especificación:

Para $n \geq 0$, $p > 0$ y $u > 0$:

$$
tiempoEstimado(n,p,u) =
D(n,u) + \left\lfloor \frac{n}{p} \right\rfloor
$$

Se utiliza la división entera porque trabajo(n) y p son valores de tipo Int.

#### Demostración.

La función realiza las siguientes operaciones:

- Comprueba que p > 0. Si no se cumple, lanza una excepción mediante require.

- Llama a profundidad(n, umbral), que por la demostración anterior devuelve correctamente $D(n,u)$.

- Llama a trabajo(n), que devuelve n.

- Divide el resultado entre p. Como ambos valores son enteros, obtiene $\lfloor n/p \rfloor$.

- Suma ambos resultados y devuelve el valor como Double.

Por lo tanto:

$$
\begin{aligned}
tiempoEstimado(n,p,u)
&= profundidad(n,u) + trabajo(n)/p \
&= D(n,u) + \left\lfloor \frac{n}{p} \right\rfloor
\end{aligned}
$$

El resultado coincide con la especificación.

#### Terminación.

tiempoEstimado realiza una cantidad finita de operaciones y llama a profundidad, cuya terminación ya fue demostrada. La función trabajo devuelve directamente su argumento, por lo que también termina.

En consecuencia, para entradas que cumplen las condiciones de los require, tiempoEstimado termina y devuelve el resultado especificado.