# Easy Game Transfer

### Introducción:
Aplicativo para transferir, de forma sencilla, un juego pesado a través de un dispositivo externo como un pendrive en varias tandas.
El motivo por el que se desarrolló el aplicativo es para evitar el enorme trabajo manual que había que realizar para poder pasar un juego de, por ejemplo, 60GB, a través de un pendrive de 16GB. Esto implicaba generar un listado de los archivos, e ir revisando qué archivos podía pasar según su peso al pendrive, reiteradas veces hasta pasar el juego completo.

### Instrucciones:
Ejecutar el aplicativo y cargar los campos requeridos, luego, presionar el botón **INICIAR**
Una vez finalizada la ejecución, pasar los archivos del dispositivo externo a la otra computadora a la cuál se quiera pasar, y volver a ejecutar la aplicación las veces que sea necesaria para poder pasar el juego completo (el aplicativo indica una vez que ya no haga falta pasar ningún archivo)

![image](https://user-images.githubusercontent.com/37516465/146694047-0229eaf5-42b3-44ba-ae9e-34c8483c6067.png)

* *Dispositivo al cuál transferir*: Este campo representa el dispositivo al cuál se van a transferir los archivos del juego. Se busca en el sistema los discos o dispositivos disponbles, los cuales se cargan en el combo para la selección del usuario
* *Path del juego*: En este campo se debe ingresar el path base del juego a pasar. Por ejemplo: *E:\Battle.net\Games\Call of Duty Modern Warfare*

En el campo *Log* se podrá ver la hora y fecha de cada paso del aplicativo, junto a una descripción tales como el espacio en disco faltante, el archivo que se está transfiriendo, si se pudo o no transferir, la finalización de la ejecución, entre otras cosas.

### ¿Cómo funciona?
* Verifica si existe el archivo *missing.txt* en el directorio, para saber si es la primera ejecución o no. (Explicación más adelante)
  * En caso de que **no** exista: Lee todos los archivos (incluyendo los archivos en subcarpetas) del path ingresado y el peso de cada uno, para poder guardarlos en un mapa en memoria
  * En caso de que **sí** exista: Lee el archivo *missing.txt* y por cada línea, guarda el path del archivo y su peso en un mapa en memoria
* Por cada archivo almacenado en el mapa:
  * Obtiene el espacio libre en el dispositivo
    * En caso de que **no** haya más espacio, guarda el path del archivo y el peso del mismo en una lista en memoria
    * En caso de que haya más espacio, copia el archivo al dispositivo
* Una vez que termina de procesar todos los archivos:
  * Borra el archivo *missing.txt*, en caso de que exista
  * Si hay datos en la lista de archivos que no se pudieron pasar, crea el archivo *missing.txt* y escribe en dicho archivo una línea por cada archivo incluyendo su path y el espacio que ocupa.

Este archivo *missing.txt* sirve para poder saber cuales son los archivos que aún faltan pasar. En caso de que al ejecutar la aplicación este archivo ya exista, es un indicador de que no es la primera ejecución, y en lugar de volver a leer todos los archivos en el path del juego, sólo leo los archivos que faltan pasar de esa lista.
