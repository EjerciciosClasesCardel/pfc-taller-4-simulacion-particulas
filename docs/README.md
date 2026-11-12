# Informes

Aquí van los tres informes que pide el enunciado, en Markdown:

- `proceso.md`: el informe de proceso, cómo se ejecuta cada función paso a
  paso. Para este taller, cómo se parte el sistema en `pasoParalelo` y qué
  tramos resuelve cada llamado.
- `correccion.md`: el informe de corrección, la argumentación de que cada
  función hace lo que el enunciado pide. Hay dos que se piden en concreto: que
  partir el sistema en segmentos no pierde ni repite partículas, y que la
  operación con que se combinan los resultados parciales es asociativa.
- `paralelizacion.md`: el informe de paralelización, la estrategia de reparto y
  los tiempos que midió con `App.scala`. Lleva la tabla de aceleraciones con
  varios tamaños y varios umbrales, la fila de control con el umbral igual al
  tamaño del sistema, y la comparación entre el tiempo estimado del punto 4 y
  el medido: en qué se parecen, en qué no y a qué lo atribuye.

Los tres archivos ya existen y traen un ejemplo de lo que se espera:
`proceso.md` sigue la traza de `factorial(5)` con recursión de cola y dibuja la
pila con `mermaid`; `correccion.md` argumenta por inducción estructural la
corrección del factorial y del máximo de una lista, y con estado, invariante y
transformación la de sus versiones iterativas; `paralelizacion.md` reparte la
suma de potencias de un arreglo con `parallel` y mide la aceleración en una
máquina de 16 núcleos. El informe del taller reemplaza ese contenido y sigue la
misma forma sobre las funciones del enunciado.
