// Función para alternar apartados sin recargar la página
function cambiarVista(vistaId, tituloSeccion, elementoBoton) {
  // 1. Ocultar todas las secciones disponibles
  const vistas = document.querySelectorAll('.app-view');
  vistas.forEach(vista => {
    vista.classList.remove('active-view');
  });

  // 2. Mostrar únicamente la sección seleccionada
  const vistaSeleccionada = document.getElementById(vistaId);
  if (vistaSeleccionada) {
    vistaSeleccionada.classList.add('active-view');
  }

  // 3. Actualizar el título en la cabecera superior
  const tituloHeader = document.getElementById('seccion-titulo');
  if (tituloHeader) {
    tituloHeader.innerText = tituloSeccion;
  }

  // 4. Resaltar el botón activo en el menú lateral
  const botonesMenu = document.querySelectorAll('.menu-item');
  botonesMenu.forEach(btn => {
    btn.classList.remove('active');
  });
  if (elementoBoton) {
    elementoBoton.classList.add('active');
  }
}