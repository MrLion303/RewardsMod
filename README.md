# RewardsMod

Mod de recompensas para **Minecraft Forge 1.20.1**. No utiliza dependencias externas: las interfaces y la lógica están incluidas en el mismo mod.

## Uso

- Al entrar al servidor se abre la interfaz principal.
- `/rewards` abre la interfaz principal.
- `/rewards admin` abre directamente la administración y requiere OP nivel 2.

Toda la interfaz, los botones, las notificaciones y los mensajes están en español.

## Sistemas de recompensas

### Recompensas diarias
Hay 30 espacios. Cada fecha local permite reclamar únicamente la recompensa correspondiente al día de la rotación. Los días que no se entra no se acumulan. La rotación de 30 días comienza cuando se crea por primera vez el archivo de datos del servidor.

### Recompensas por racha
Entrar al menos una vez por día mantiene la racha. Al perder un día, la siguiente entrada reinicia la racha en el día 1. Al completar el día 30, la secuencia vuelve al día 1.

### Recompensas por horas jugadas
El tiempo se acumula individualmente mientras el jugador permanece conectado. Hay 10 niveles; inicialmente requieren 10, 20, 30... hasta 100 horas. Al alcanzar un nivel aparece una notificación en pantalla. La recompensa se reclama desde la interfaz.

## Administración (OP)

La administración requiere OP nivel 2. Se abre desde el botón **Administración** o con `/rewards admin`.

Selecciona el apartado que quieres editar. En las pantallas de edición puedes colocar objetos desde tu inventario directamente en los espacios de recompensa. Para editar las horas, selecciona un nivel con clic izquierdo y ajusta el requisito con los botones de restar o sumar. El botón final suma cinco horas. En la pantalla de horas, clic derecho sobre un espacio con el cursor vacío retira la recompensa.

Los cambios se guardan en los datos persistentes del mundo.

## Zona horaria y persistencia

Los cambios de día se calculan con `America/Monterrey`. El progreso de cada jugador, las recompensas configuradas y los umbrales se guardan en los datos del mundo y sobreviven a reinicios del servidor.

## Compilación

El workflow **Compilar RewardsMod** compila el proyecto al subir cambios a `main` o al ejecutarlo manualmente desde GitHub Actions. El archivo JAR queda disponible como artefacto de la ejecución.
