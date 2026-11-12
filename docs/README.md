# Informes

Aquí van los dos informes que pide el enunciado, en Markdown:

- `proceso.md`: el informe de proceso, cómo se ejecuta cada función paso a
  paso. Para este taller, cómo se parte el sistema en `pasoParalelo` y qué
  tramos resuelve cada llamado, y la comparación entre el tiempo estimado del
  punto 4 y el medido del punto 5: en qué se parecen, en qué no y a qué lo
  atribuye.
- `correccion.md`: el informe de corrección, la argumentación de que cada
  función hace lo que el enunciado pide. Hay dos que se piden en concreto: que
  partir el sistema en segmentos no pierde ni repite partículas, y que la
  operación con que se combinan los resultados parciales es asociativa.

Los dos archivos ya existen y traen un ejemplo de lo que se espera:
`proceso.md` sigue la traza de `factorial(5)` con recursión de cola y dibuja la
pila con `mermaid`; `correccion.md` argumenta por inducción estructural la
corrección del factorial y del máximo de una lista, y con estado, invariante y
transformación la de sus versiones iterativas. El informe del taller reemplaza
ese contenido y sigue la misma forma sobre las funciones del enunciado.
