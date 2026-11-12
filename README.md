# Taller 4 — Simulación de partículas en paralelo

Fundamentos de Programación Funcional y Concurrente
Escuela de Ingeniería de Sistemas y Computación, Universidad del Valle
Carlos Andrés Delgado Saavedra

El enunciado completo, con los ejemplos de cada punto, la rúbrica y la
ecuación de calificación, es el PDF publicado en el campus virtual. Este
archivo dice cómo está armado el proyecto y cómo se entrega.

## Integrantes

Llene esta tabla con el nombre completo y el código de cada integrante. Es
parte de la entrega: si falta alguno, la entrega se sanciona con el 20 % de
la nota.

| Nombre completo | Código |
|---|---|
| Daniela Franco Ibarra | 202477154 |
| Dayan Stefany Marulanda Pulido | 202477427 |
| Juan Alejandro Márquez Varón | 202559853 |
| | |

## Cómo está organizado el proyecto

```
app/src/main/scala/taller/
    Modelo.scala                   los tipos del taller, que no se modifican
    SimulacionParticulas.scala     aquí van los cinco puntos
    App.scala                      programa de arranque

app/src/main/scala/common/
    package.scala                  parallel y task, que no se modifican

app/src/test/scala/taller/
    SimulacionParticulasTest.scala las 36 pruebas dadas, que no se modifican

docs/
    proceso.md                     informe de proceso
    correccion.md                  informe de corrección
    paralelizacion.md              informe de paralelización
```

Su código va en `SimulacionParticulas.scala`. Las 36 pruebas que vienen
escritas no se modifican: son el contrato con que se califica.

La rúbrica pide además **cinco casos de prueba propios por cada punto**,
distintos de los ejemplos del enunciado y de los que trae el material. Esos
van donde le resulte más cómodo: al final de
`SimulacionParticulasTest.scala`, debajo de las que ya están, o en un archivo
nuevo dentro de `app/src/test/scala/taller/`. Las dos formas valen; lo que no
se toca es lo que ya estaba escrito.

Los tres informes que pide el enunciado, de proceso, de corrección y de
paralelización, van en `docs/`, en Markdown, con la notación matemática en
LaTeX y los diagramas en `mermaid`; no se aceptan imágenes insertadas ni
archivos por fuera de esa carpeta. Los tres archivos vienen con un ejemplo
resuelto sobre funciones distintas a las de este taller.

## Cómo se ejecuta

```bash
./gradlew test    # revisa las reglas del curso y corre las pruebas
./gradlew run     # corre el programa
```

La primera vez se demora: Gradle descarga el compilador de Scala y la
versión de Java que necesita. No hay que instalar nada a mano. Al terminar,
`./gradlew test` deja un informe navegable en
`app/build/reports/tests/test/index.html`.

## El punto de partida

Al clonar, las 36 pruebas están en rojo, porque las catorce funciones dicen
`???`. Su trabajo es reemplazar cada `???` y ver las pruebas ponerse en
verde.

`Modelo.scala` trae los tipos ya escritos: `Vector2`, con la suma, el
producto por un número y la norma, y `Particula`, con posición, velocidad y
masa. El paquete `common` trae `parallel` y `task`. Ninguno de los dos se
modifica.

`medir` también viene escrita: toma la mediana de varias corridas en
milisegundos y descarta una de calentamiento, porque la primera corrida
siempre es más lenta mientras la máquina virtual optimiza el código.

## Los cinco puntos

| Punto | Funciones |
|---|---|
| 1 | `mover`, `pasoSecuencial`, `pasoSegmento` y `pasoParalelo` |
| 2 | `energiaSecuencial`, `masaTotal` y `centroDeMasaSecuencial` |
| 3 | `energiaParalela` y `centroDeMasaParalelo` |
| 4 | `trabajo`, `profundidad` y `tiempoEstimado` |
| 5 | `aceleracion`, `umbralConveniente` y la medición en `App.scala` |

El punto 1 reparte el paso de la simulación con `parallel`, partiendo el
sistema a la mitad mientras el tramo pase del umbral. El 2 calcula las
magnitudes globales en secuencial. El 3 las reparte: la energía se combina
sumando, y el centro de masa no, porque promediar los promedios de dos
mitades que no pesan igual da otra cosa. El 4 estima el costo sin ejecutar
nada. El 5 mide los tiempos de verdad y los compara con esa estimación.

Las pruebas dadas no miden tiempos. Un tiempo depende de la máquina y de lo
que esté corriendo al lado, así que lo que comprueban es que la versión
paralela dé lo mismo que la secuencial con cualquier umbral.

## Reglas del código

En este curso no se usa `var`, ni `while`, ni `return`, ni ningún estado que
cambie. Todo se resuelve con `val`, recursión y llamados a funciones. Si
necesita una secuencia de instrucciones, use un bloque `{ ... }`; si necesita
funciones auxiliares, escríbalas dentro de las funciones.

`Array` sí se usa en este taller, porque el reparto por segmentos necesita
acceso por índice. Lo que no entra es modificar una celda: cada función
devuelve un arreglo nuevo.

`./gradlew test` revisa estas reglas **antes** de correr las pruebas. Un
programa con `var`, `while` o `return` no llega a las pruebas: el flujo se
detiene y dice en qué línea está el problema.

No se editan los archivos de configuración del proyecto ni los flujos de
GitHub.

## Cómo se entrega

1. Presione **Fork** en este repositorio. Un solo integrante hace el fork y
   agrega a los demás como colaboradores; todo el equipo trabaja sobre ese
   mismo fork.
2. En su fork, entre a la pestaña **Actions** y habilítelas. GitHub las deja
   apagadas en los forks hasta que el dueño lo autoriza, y sin eso no ve el
   resultado de las pruebas al hacer push.
3. Clone su fork, resuelva, y haga commit y push a `main`. Cada integrante
   hace sus propios commits: la rúbrica de aporte al código se calcula con
   ellos.
4. Registre en la tarea **Entrega del taller 4** del campus virtual la
   dirección de su fork y el hash del último commit, que se obtiene con
   `git rev-parse HEAD`.

**La fecha de entrega es el jueves 26 de noviembre de 2026, a las 23:59.**

El hash se saca **después** del último merge a `main`. Si se toma antes, el
commit que queda registrado no tiene el trabajo que entró en ese merge, y es
ese commit el que se califica.

Las condiciones son estas, y son las del acta de inicio del curso:

- **La entrega es el enlace registrado en el campus dentro del plazo.** No se
  aceptan entregas por correo ni por ningún otro medio. Si el enlace no llega
  a tiempo, la entrega se califica con **0.0**.
- **El repositorio tiene que ser un fork de este.** Un repositorio creado
  aparte, aunque tenga el mismo código, se sanciona con el **30 %** de la
  nota: rompe la estructura con que se califica y la verificación automática.
- **Se califica el último commit anterior a la fecha de entrega.** Lo que se
  suba después no se revisa. El hash que se registra en el campus debe ser el
  de ese commit.
- **El `README.md` lleva el nombre y el código de todos los integrantes.**
  Si falta alguno, la entrega se sanciona con el **20 %** de la nota.
- Se puede trabajar en grupos de hasta cuatro personas.
- El docente puede pedir sustentación del taller. En ese caso la nota de la
  sustentación es individual, entre 0 y 1, y multiplica la nota del taller.
