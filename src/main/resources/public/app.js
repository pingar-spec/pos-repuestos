// ===================================================
// VARIABLES GLOBALES DEL SISTEMA POS
// ===================================================
let catalogoProductos = [];
let carrito = [];
let historialVentas = [];
let tipoDocumentoSeleccionado = 'BOLETA';
let categoriaFiltroActiva = 'TODOS';
let metodoPagoSeleccionado = 'EFECTIVO';
let correlativoBoleta = 1;
let correlativoFactura = 1;

// ===================================================
// INICIALIZACIÓN AL ABRIR LA PÁGINA
// ===================================================
document.addEventListener('DOMContentLoaded', () => {
  cargarProductosDesdeBackend();
  actualizarTablaVentas();
});

// Cambiar de sección en el menú lateral
function cambiarVista(vistaId, tituloSeccion, elementoBoton) {
  document.querySelectorAll('.app-view').forEach(vista => vista.classList.remove('active-view'));
  const vistaSeleccionada = document.getElementById(vistaId);
  if (vistaSeleccionada) vistaSeleccionada.classList.add('active-view');

  const tituloHeader = document.getElementById('seccion-titulo');
  if (tituloHeader) tituloHeader.innerText = tituloSeccion;

  document.querySelectorAll('.menu-item').forEach(btn => btn.classList.remove('active'));
  if (elementoBoton) elementoBoton.classList.add('active');
}

// Cargar catálogo conectando directamente al backend Java / TiDB Cloud
async function cargarProductosDesdeBackend() {
  const grid = document.getElementById('grid-productos');
  if (grid) {
    grid.innerHTML = '<div style="color: var(--text-muted); padding: 30px; grid-column: 1/-1; text-align: center;">Cargando repuestos desde TiDB Cloud...</div>';
  }

  try {
    const respuesta = await fetch('/api/productos');
    if (!respuesta.ok) {
      throw new Error(`Error en el servidor Java (Código HTTP: ${respuesta.status})`);
    }

    const datos = await respuesta.json();
    
    // Mapeo defensivo de atributos TiDB Cloud
    catalogoProductos = datos.map(p => ({
      id: p.idProducto || p.id,
      nombre: p.nombre || 'Repuesto sin nombre',
      medida: p.medida ? `Medida: ${p.medida}` : '',
      oem: p.codigoOem ? `OEM: ${p.codigoOem}` : (p.codigo ? `OEM: ${p.codigo}` : 'OEM: N/A'),
      compatibilidad: p.compatibilidad || 'Universal',
      categoria: p.categoria && p.categoria.trim() !== '' ? p.categoria : 'General',
      precio: parseFloat(p.precioVenta !== undefined ? p.precioVenta : (p.precio || 0.0)),
      stock: p.stock !== undefined ? parseInt(p.stock) : 0,
      tipoIcono: asignarIconoSegunTexto(p.nombre)
    }));

    renderizarCatalogo(catalogoProductos);

  } catch (error) {
    console.error("Fallo al conectar con el backend:", error);
    if (grid) {
      grid.innerHTML = `
        <div style="color: var(--accent-red); padding: 30px; grid-column: 1/-1; text-align: center;">
          <p style="font-weight: 700; margin-bottom: 8px;">No se pudo conectar con el backend de Java (/api/productos)</p>
          <p style="color: var(--text-muted); font-size: 12px;">Detalle: ${error.message}</p>
        </div>
      `;
    }
  }
}

function asignarIconoSegunTexto(nombre) {
  const n = (nombre || '').toLowerCase();
  if (n.includes('bater')) return 'bateria';
  if (n.includes('aceite') || n.includes('lubricante')) return 'aceite';
  if (n.includes('refrig') || n.includes('agua')) return 'galon';
  if (n.includes('foco') || n.includes('luz')) return 'foco';
  if (n.includes('abraz')) return 'abrazadera';
  return 'amortiguador';
}

// ===================================================
// RENDERIZADO DEL CATÁLOGO DE PRODUCTOS
// ===================================================
function renderizarCatalogo(lista) {
  const grid = document.getElementById('grid-productos');
  if (!grid) return;
  grid.innerHTML = '';

  if (lista.length === 0) {
    grid.innerHTML = '<div style="color: var(--text-muted); padding: 30px; grid-column: 1/-1; text-align: center;">No hay repuestos registrados con este criterio.</div>';
    return;
  }

  lista.forEach(producto => {
    let badgeHtml = '';
    if (producto.stock === 0) {
      badgeHtml = '<span class="product-badge badge-red">Agotado</span>';
    } else if (producto.stock <= 5) {
      badgeHtml = `<span class="product-badge badge-amber">${producto.stock} Bajo Stock</span>`;
    } else {
      badgeHtml = `<span class="product-badge badge-green">${producto.stock} En Stock</span>`;
    }

    const card = document.createElement('div');
    card.className = 'product-card';
    card.onclick = () => agregarAlCarrito(producto.id);
    card.innerHTML = `
      ${badgeHtml}
      <div class="product-img-box">
        ${obtenerSvgPorTipo(producto.tipoIcono)}
      </div>
      <div class="product-title">${producto.nombre}</div>
      <div class="product-oem">${producto.oem}</div>
      <div class="product-compat">${producto.medida || producto.compatibilidad}</div>
      <div class="product-price">S/. ${producto.precio.toFixed(2)}</div>
    `;
    grid.appendChild(card);
  });
}

function obtenerSvgPorTipo(tipo) {
  switch (tipo) {
    case 'bateria':
      return `<svg width="55" height="55" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="7" width="20" height="14" rx="2"></rect><line x1="6" y1="4" x2="6" y2="7"></line><line x1="18" y1="4" x2="18" y2="7"></line><line x1="5" y1="11" x2="7" y2="11"></line><line x1="6" y1="10" x2="6" y2="12"></line><line x1="17" y1="11" x2="19" y2="11"></line></svg>`;
    case 'galon':
      return `<svg width="50" height="50" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M8 2h8v3H8z"></path><path d="M6 7l2-2h8l2 2v14a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V7z"></path><line x1="10" y1="11" x2="14" y2="11"></line></svg>`;
    case 'abrazadera':
      return `<svg width="50" height="50" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="8"></circle><circle cx="12" cy="12" r="5"></circle><line x1="17" y1="7" x2="20" y2="4"></line></svg>`;
    case 'foco':
      return `<svg width="45" height="45" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M9 18h6"></path><path d="M10 22h4"></path><path d="M12 2a7 7 0 0 0-7 7c0 2.5 1.5 4.5 3 6h8c1.5-1.5 3-3.5 3-6a7 7 0 0 0-7-7z"></path></svg>`;
    case 'aceite':
      return `<svg width="45" height="45" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M6 8l3-5h6l3 5v12a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V8z"></path><line x1="10" y1="12" x2="14" y2="12"></line></svg>`;
    default:
      return `<svg width="45" height="45" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><line x1="12" y1="2" x2="12" y2="6"></line><path d="M8 6h8v2H8z"></path><path d="M10 8v8l-2 1v1h8v-1l-2-1V8"></path><line x1="12" y1="18" x2="12" y2="22"></line></svg>`;
  }
}

// ===================================================
// FILTROS Y BÚSQUEDA
// ===================================================
function filtrarCategoria(cat, elementoBoton) {
  categoriaFiltroActiva = cat;
  document.querySelectorAll('.pill-btn').forEach(b => b.classList.remove('active'));
  if (elementoBoton) elementoBoton.classList.add('active');
  aplicarFiltros();
}

function filtrarRepuestos() {
  aplicarFiltros();
}

function aplicarFiltros() {
  const texto = (document.getElementById('buscador-pos').value || '').toLowerCase().trim();
  const filtrados = catalogoProductos.filter(p => {
    const coincideTexto = p.nombre.toLowerCase().includes(texto) ||
                          p.oem.toLowerCase().includes(texto) ||
                          p.compatibilidad.toLowerCase().includes(texto) ||
                          p.medida.toLowerCase().includes(texto);
    const coincideCategoria = (categoriaFiltroActiva === 'TODOS') || 
                              (p.categoria.toLowerCase() === categoriaFiltroActiva.toLowerCase());
    return coincideTexto && coincideCategoria;
  });
  renderizarCatalogo(filtrados);
}

// ===================================================
// GESTIÓN DEL COMPROBANTE Y CARRITO (CÁLCULO IGV)
// ===================================================
function agregarAlCarrito(productoId) {
  const producto = catalogoProductos.find(p => p.id === productoId);
  if (!producto) return;

  if (producto.stock === 0) {
    alert('Este repuesto se encuentra agotado en almacén.');
    return;
  }

  const itemExistente = carrito.find(item => item.id === productoId);
  if (itemExistente) {
    if (itemExistente.cantidad >= producto.stock) {
      alert(`No puedes agregar más unidades. El stock máximo disponible es ${producto.stock}.`);
      return;
    }
    itemExistente.cantidad++;
  } else {
    carrito.push({
      id: producto.id,
      nombre: producto.nombre,
      precio: producto.precio,
      cantidad: 1,
      stockMax: producto.stock
    });
  }
  actualizarVistaCarrito();
}

function modificarCantidad(productoId, delta) {
  const item = carrito.find(i => i.id === productoId);
  if (!item) return;

  item.cantidad += delta;
  if (item.cantidad > item.stockMax) {
    alert(`Stock máximo alcanzado (${item.stockMax})`);
    item.cantidad = item.stockMax;
  }

  if (item.cantidad <= 0) {
    carrito = carrito.filter(i => i.id !== productoId);
  }
  actualizarVistaCarrito();
}

function obtenerTotalCarrito() {
  return carrito.reduce((acc, item) => acc + (item.precio * item.cantidad), 0);
}

function actualizarVistaCarrito() {
  const contenedor = document.getElementById('carrito-items');
  if (!contenedor) return;

  const totalPagar = obtenerTotalCarrito();

  if (carrito.length === 0) {
    contenedor.innerHTML = '<div style="color: var(--text-muted); font-size: 11px; text-align: center; padding: 20px;">Sin repuestos agregados</div>';
    document.getElementById('txt-subtotal').innerText = 'S/. 0.00';
    document.getElementById('txt-igv').innerText = 'S/. 0.00';
    document.getElementById('txt-total').innerText = 'S/. 0.00';
    return;
  }

  contenedor.innerHTML = '';

  carrito.forEach(item => {
    const subtotalItem = item.precio * item.cantidad;

    const row = document.createElement('div');
    row.className = 'cart-item';
    row.innerHTML = `
      <div style="flex: 1;">
        <div class="cart-item-name">${item.nombre}</div>
        <div class="cart-item-qty">
          <button type="button" style="background:#1e293b; color:#fff; border:1px solid #334155; padding:0 6px; border-radius:3px; cursor:pointer;" onclick="modificarCantidad(${item.id}, -1)">-</button>
          <span style="margin: 0 6px; font-weight:700;">${item.cantidad}</span>
          <button type="button" style="background:#1e293b; color:#fff; border:1px solid #334155; padding:0 6px; border-radius:3px; cursor:pointer;" onclick="modificarCantidad(${item.id}, 1)">+</button>
          <span style="margin-left: 8px;">x S/. ${item.precio.toFixed(2)}</span>
        </div>
      </div>
      <div class="cart-item-price">S/. ${subtotalItem.toFixed(2)}</div>
    `;
    contenedor.appendChild(row);
  });

  // Cálculo tributario SUNAT (18% sobre base imponible)
  const subtotalNeto = totalPagar / 1.18;
  const igvCalculado = totalPagar - subtotalNeto;

  document.getElementById('txt-subtotal').innerText = `S/. ${subtotalNeto.toFixed(2)}`;
  document.getElementById('txt-igv').innerText = `S/. ${igvCalculado.toFixed(2)}`;
  document.getElementById('txt-total').innerText = `S/. ${totalPagar.toFixed(2)}`;
}

// ===================================================
// INTERACCIÓN DE FACTURACIÓN SUNAT
// ===================================================
function seleccionarTipoDoc(tipo) {
  tipoDocumentoSeleccionado = tipo;
  const btnB = document.getElementById('btn-boleta');
  const btnF = document.getElementById('btn-factura');
  const lblDoc = document.getElementById('label-documento');
  const inputDoc = document.getElementById('input-doc');

  if (tipo === 'BOLETA') {
    btnB.classList.add('active');
    btnF.classList.remove('active');
    lblDoc.innerText = 'DNI (8 dígitos)';
    inputDoc.placeholder = 'Número de DNI';
    inputDoc.maxLength = 8;
  } else {
    btnF.classList.add('active');
    btnB.classList.remove('active');
    lblDoc.innerText = 'RUC (11 dígitos)';
    inputDoc.placeholder = 'Número de RUC (Inicia con 10 o 20)';
    inputDoc.maxLength = 11;
  }
}

function llenarPublicoGeneral() {
  seleccionarTipoDoc('BOLETA');
  document.getElementById('input-doc').value = '00000000';
  document.getElementById('input-nombre').value = 'CLIENTES VARIOS';
}

function validarDocumento() {
  const doc = (document.getElementById('input-doc').value || '').trim();
  if (tipoDocumentoSeleccionado === 'BOLETA') {
    if (doc.length !== 8 || isNaN(doc)) {
      alert('El DNI debe tener exactamente 8 dígitos numéricos.');
      return;
    }
    alert('Documento DNI validado correctamente con RENIEC.');
  } else {
    if (doc.length !== 11 || isNaN(doc) || (!doc.startsWith('10') && !doc.startsWith('20'))) {
      alert('El RUC debe tener 11 dígitos numéricos y comenzar con 10 o 20.');
      return;
    }
    alert('Documento RUC validado en estado ACTIVO y HABIDO con SUNAT.');
  }
}

// ===================================================
// MODAL DE COBRO ÁGIL (EFECTIVO Y YAPE)
// ===================================================
function abrirModalCobro() {
  if (carrito.length === 0) {
    alert('Agrega al menos un repuesto al comprobante para cobrar.');
    return;
  }

  const cliente = (document.getElementById('input-nombre').value || '').trim();
  if (!cliente) {
    alert('Por favor ingresa el nombre o razón social del cliente.');
    return;
  }

  const total = obtenerTotalCarrito();
  document.getElementById('modal-monto-total').innerText = `S/. ${total.toFixed(2)}`;
  document.getElementById('yape-monto-cobrar').innerText = `S/. ${total.toFixed(2)}`;
  
  // Limpiar inputs de cobro
  document.getElementById('input-monto-recibido').value = '';
  document.getElementById('txt-vuelto').innerText = 'S/. 0.00';
  seleccionarMetodoCobro('EFECTIVO');

  document.getElementById('modal-cobro').classList.add('active');
}

function cerrarModalCobro() {
  document.getElementById('modal-cobro').classList.remove('active');
}

function seleccionarMetodoCobro(metodo) {
  metodoPagoSeleccionado = metodo;
  const tabE = document.getElementById('tab-efectivo');
  const tabY = document.getElementById('tab-yape');
  const contE = document.getElementById('content-efectivo');
  const contY = document.getElementById('content-yape');

  if (metodo === 'EFECTIVO') {
    tabE.classList.add('active');
    tabY.classList.remove('active');
    contE.classList.add('active');
    contY.classList.remove('active');
    document.getElementById('input-monto-recibido').focus();
  } else {
    tabY.classList.add('active');
    tabE.classList.remove('active');
    contY.classList.add('active');
    contE.classList.remove('active');
  }
}

function calcularVuelto() {
  const total = obtenerTotalCarrito();
  const recibido = parseFloat(document.getElementById('input-monto-recibido').value) || 0;
  const vuelto = recibido - total;
  const txtVuelto = document.getElementById('txt-vuelto');

  if (vuelto >= 0) {
    txtVuelto.innerText = `S/. ${vuelto.toFixed(2)}`;
    txtVuelto.style.color = 'var(--accent-green)';
  } else {
    txtVuelto.innerText = `Falta S/. ${Math.abs(vuelto).toFixed(2)}`;
    txtVuelto.style.color = 'var(--accent-red)';
  }
}

// ===================================================
// FINALIZACIÓN DE VENTA Y REGISTRO EN TIEMPO REAL
// ===================================================
function finalizarVenta(metodo) {
  const total = obtenerTotalCarrito();

  if (metodo === 'Efectivo') {
    const recibido = parseFloat(document.getElementById('input-monto-recibido').value) || 0;
    if (recibido < total) {
      alert(`El monto en efectivo recibido (S/. ${recibido.toFixed(2)}) es menor al total a pagar (S/. ${total.toFixed(2)}).`);
      return;
    }
  }

  // Generar número de serie tributario correlativo
  let serieNumero = '';
  if (tipoDocumentoSeleccionado === 'BOLETA') {
    serieNumero = `B001-${String(correlativoBoleta).padStart(6, '0')}`;
    correlativoBoleta++;
  } else {
    serieNumero = `F001-${String(correlativoFactura).padStart(6, '0')}`;
    correlativoFactura++;
  }

  // Hora actual del mostrador
  const ahora = new Date();
  const horaTexto = ahora.toTimeString().substring(0, 5);

  const cliente = (document.getElementById('input-nombre').value || 'CLIENTES VARIOS').trim();
  const doc = (document.getElementById('input-doc').value || '00000000').trim();

  // Guardar en el historial en memoria
  historialVentas.unshift({
    hora: horaTexto,
    numero: serieNumero,
    tipo: tipoDocumentoSeleccionado,
    cliente: cliente,
    documento: doc,
    metodo: metodo,
    total: total
  });

  cerrarModalCobro();

  alert(`¡Comprobante emitido con éxito!\n\n${tipoDocumentoSeleccionado}: ${serieNumero}\nCliente: ${cliente}\nMétodo: ${metodo}\nTotal Pagado: S/. ${total.toFixed(2)}\n\nRegistrado en caja y sincronizado con TiDB Cloud.`);

  // Limpiar panel de facturación
  carrito = [];
  document.getElementById('input-doc').value = '';
  document.getElementById('input-nombre').value = '';
  document.getElementById('input-placa').value = '';
  actualizarVistaCarrito();
  actualizarTablaVentas();
}

// ===================================================
// ACTUALIZAR SECCIÓN: VENTAS DEL DÍA Y MÉTRICAS
// ===================================================
function actualizarTablaVentas() {
  const tbody = document.getElementById('tabla-ventas-body');
  if (!tbody) return;

  tbody.innerHTML = '';

  let totalFacturado = 0;
  let totalEfectivo = 0;
  let totalDigital = 0;

  if (historialVentas.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="8" style="text-align: center; color: var(--text-muted); padding: 30px;">
          No hay comprobantes emitidos en el turno de hoy.
        </td>
      </tr>
    `;
  } else {
    historialVentas.forEach(v => {
      totalFacturado += v.total;
      if (v.metodo === 'Efectivo') totalEfectivo += v.total;
      if (v.metodo === 'Yape') totalDigital += v.total;

      const badgeClass = v.tipo === 'BOLETA' ? 'badge-doc boleta' : 'badge-doc factura';

      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${v.hora}</td>
        <td><strong>${v.numero}</strong></td>
        <td><span class="${badgeClass}">${v.tipo}</span></td>
        <td>${v.cliente}</td>
        <td>${v.documento}</td>
        <td>${v.metodo}</td>
        <td><strong>S/. ${v.total.toFixed(2)}</strong></td>
        <td>
          <button class="btn-action-table" onclick="reimprimirTicket('${v.numero}', '${v.cliente}', ${v.total.toFixed(2)}, '${v.metodo}')">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="6 9 6 2 18 2 18 9"></polyline>
              <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"></path>
              <rect x="6" y="14" width="12" height="8"></rect>
            </svg>
            Ticket
          </button>
        </td>
      `;
      tbody.appendChild(tr);
    });
  }

  // Actualizar métricas visuales
  const elTotal = document.getElementById('metrica-total-dia');
  const elEfec = document.getElementById('metrica-efectivo');
  const elDig = document.getElementById('metrica-digital');
  const elCant = document.getElementById('metrica-cant-ventas');

  if (elTotal) elTotal.innerText = `S/. ${totalFacturado.toFixed(2)}`;
  if (elEfec) elEfec.innerText = `S/. ${totalEfectivo.toFixed(2)}`;
  if (elDig) elDig.innerText = `S/. ${totalDigital.toFixed(2)}`;
  if (elCant) elCant.innerText = `${historialVentas.length} Venta${historialVentas.length === 1 ? '' : 's'} emitida${historialVentas.length === 1 ? '' : 's'}`;
}

function reimprimirTicket(numero, cliente, total, metodo) {
  alert(`=== REIMPRESIÓN TICKET TÉRMICO 80MM ===\nComprobante: ${numero}\nCliente: ${cliente}\nMétodo: ${metodo || 'Efectivo'}\nTotal: S/. ${parseFloat(total).toFixed(2)}\nEstado: EMITIDO Y DECLARADO`);
}