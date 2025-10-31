package cr.ac.una.wsrestuna.rest;

import cr.ac.una.wsrestuna.model.DetalleFactura;
import cr.ac.una.wsrestuna.model.Factura;
import cr.ac.una.wsrestuna.service.FacturaService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/facturas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FacturaRest {

    private static final Logger LOG = Logger.getLogger(FacturaRest.class.getName());

    @EJB
    private FacturaService facturaService;

    @GET
    public Response findAll() {
        try {
            List<Factura> facturas = facturaService.findAll();
            return Response.ok(createResponse(true, "Facturas obtenidas", facturas)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener facturas", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Long id) {
        try {
            Optional<Factura> factura = facturaService.findById(id);

            if (factura.isPresent()) {
                return Response.ok(createResponse(true, "Factura encontrada", factura.get())).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Factura no encontrada", null))
                        .build();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar factura", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
    @Path("/{id}/detalles")
    public Response getDetalles(@PathParam("id") Long facturaId) {
        try {
            List<DetalleFactura> detalles = facturaService.findDetallesByFactura(facturaId);
            return Response.ok(createResponse(true, "Detalles obtenidos", detalles)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener detalles", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
    @Path("/cierre/{cierreId}")
    public Response findByCierre(@PathParam("cierreId") Long cierreId) {
        try {
            List<Factura> facturas = facturaService.findByCierre(cierreId);
            return Response.ok(createResponse(true, "Facturas obtenidas", facturas)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar facturas por cierre", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @POST
    @Path("/desde-orden")
    public Response createFromOrden(Map<String, Object> datos) {
        try {
            Long ordenId = Long.valueOf(datos.get("ordenId").toString());
            Long usuarioId = Long.valueOf(datos.get("usuarioId").toString());
            Long clienteId = datos.get("clienteId") != null ? Long.valueOf(datos.get("clienteId").toString()) : null;

            boolean aplicaImpVenta = Boolean.parseBoolean(datos.get("aplicaImpuestoVenta").toString());
            boolean aplicaImpServicio = Boolean.parseBoolean(datos.get("aplicaImpuestoServicio").toString());

            BigDecimal descuento = new BigDecimal(datos.getOrDefault("descuento", "0").toString());
            BigDecimal montoEfectivo = new BigDecimal(datos.get("montoEfectivo").toString());
            BigDecimal montoTarjeta = new BigDecimal(datos.get("montoTarjeta").toString());

            Factura factura = facturaService.createFromOrden(
                    ordenId, clienteId, usuarioId,
                    aplicaImpVenta, aplicaImpServicio,
                    descuento, montoEfectivo, montoTarjeta
            );

            return Response.status(Response.Status.CREATED)
                    .entity(createResponse(true, "Factura creada exitosamente", factura))
                    .build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al crear factura", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @POST
    @Path("/directa")
    public Response createDirecta(Map<String, Object> datos) {
        try {
            Long usuarioId = Long.valueOf(datos.get("usuarioId").toString());
            Long clienteId = datos.get("clienteId") != null ? Long.valueOf(datos.get("clienteId").toString()) : null;

            // Los detalles vienen en el Map
            @SuppressWarnings("unchecked")
            List<DetalleFactura> detalles = (List<DetalleFactura>) datos.get("detalles");

            boolean aplicaImpVenta = Boolean.parseBoolean(datos.get("aplicaImpuestoVenta").toString());
            boolean aplicaImpServicio = Boolean.parseBoolean(datos.get("aplicaImpuestoServicio").toString());

            BigDecimal descuento = new BigDecimal(datos.getOrDefault("descuento", "0").toString());
            BigDecimal montoEfectivo = new BigDecimal(datos.get("montoEfectivo").toString());
            BigDecimal montoTarjeta = new BigDecimal(datos.get("montoTarjeta").toString());

            Factura factura = facturaService.createDirecta(
                    usuarioId, clienteId, detalles,
                    aplicaImpVenta, aplicaImpServicio,
                    descuento, montoEfectivo, montoTarjeta
            );

            return Response.status(Response.Status.CREATED)
                    .entity(createResponse(true, "Factura creada exitosamente", factura))
                    .build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al crear factura directa", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @POST
    @Path("/{id}/anular")
    public Response anular(@PathParam("id") Long id) {
        try {
            facturaService.anular(id);
            return Response.ok(createResponse(true, "Factura anulada exitosamente", null)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al anular factura", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    private Map<String, Object> createResponse(boolean success, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("data", data);
        return response;
    }

    @POST
    public Response create(Map<String, Object> datos) {
        try {
            System.out.println("========== DEBUG FACTURA REST ==========");
            System.out.println("Datos recibidos: " + datos);

            // ===== 1. IDs básicos =====
            Long ordenId = Long.valueOf(datos.get("ordenId").toString());
            System.out.println("Orden ID: " + ordenId);

            Long clienteId = null;
            if (datos.get("clienteId") != null) {
                clienteId = Long.valueOf(datos.get("clienteId").toString());
            }

            // ===== 2. Resumen (totales calculados en frontend) =====
            @SuppressWarnings("unchecked")
            Map<String, Object> resumen = (Map<String, Object>) datos.get("resumen");
            System.out.println("Resumen recibido: " + resumen);

            // Subtotal SIN impuestos
            java.math.BigDecimal subtotal = new java.math.BigDecimal(
                    resumen.get("subtotal").toString()
            );
            System.out.println("Subtotal: " + subtotal);

            // Montos de impuestos YA calculados en frontend
            java.math.BigDecimal impVentaMonto = new java.math.BigDecimal(
                    resumen.getOrDefault("impuestoVentas", "0").toString()
            );
            java.math.BigDecimal impServicioMonto = new java.math.BigDecimal(
                    resumen.getOrDefault("impuestoServicio", "0").toString()
            );
            System.out.println("Impuesto Venta: " + impVentaMonto);
            System.out.println("Impuesto Servicio: " + impServicioMonto);

            boolean aplicaImpVenta = impVentaMonto.compareTo(java.math.BigDecimal.ZERO) > 0;
            boolean aplicaImpServicio = impServicioMonto.compareTo(java.math.BigDecimal.ZERO) > 0;

            // **CRÍTICO: Descuento viene como MONTO ABSOLUTO del frontend**
            java.math.BigDecimal descuentoMonto = new java.math.BigDecimal(
                    resumen.getOrDefault("descuento", "0").toString()
            );
            System.out.println("Descuento MONTO recibido: " + descuentoMonto);

            // **CÁLCULO CORRECTO: Base = SUBTOTAL + TODOS LOS IMPUESTOS**
            java.math.BigDecimal baseParaDescuento = subtotal
                    .add(impVentaMonto)
                    .add(impServicioMonto);

            System.out.println("Base para descuento (subtotal + impuestos): " + baseParaDescuento);

            java.math.BigDecimal descuentoPct = java.math.BigDecimal.ZERO;

            // Solo calcular porcentaje si hay descuento y base > 0
            if (baseParaDescuento.compareTo(java.math.BigDecimal.ZERO) > 0
                    && descuentoMonto.compareTo(java.math.BigDecimal.ZERO) > 0) {

                // Fórmula correcta: descuentoPct = (descuentoMonto / base) * 100
                descuentoPct = descuentoMonto
                        .multiply(new java.math.BigDecimal("100"))
                        .divide(baseParaDescuento, 4, java.math.RoundingMode.HALF_UP);

                System.out.println("Descuento PORCENTAJE calculado: " + descuentoPct + "%");
            }

            // ===== 3. Pagos =====
            @SuppressWarnings("unchecked")
            Map<String, Object> pagos = (Map<String, Object>) datos.get("pagos");

            java.math.BigDecimal montoEfectivo = new java.math.BigDecimal(
                    pagos.getOrDefault("efectivo", "0").toString()
            );
            java.math.BigDecimal montoTarjeta = new java.math.BigDecimal(
                    pagos.getOrDefault("tarjeta", "0").toString()
            );

            System.out.println("Efectivo: " + montoEfectivo);
            System.out.println("Tarjeta: " + montoTarjeta);
            System.out.println("========================================");

            // ===== 4. Crear la factura =====
            Factura factura = facturaService.createFromOrden(
                    ordenId,
                    clienteId,
                    null, // usuarioId: se infiere de la orden
                    aplicaImpVenta,
                    aplicaImpServicio,
                    descuentoPct, // Ahora enviamos el porcentaje CORRECTO
                    montoEfectivo,
                    montoTarjeta
            );

            return Response.status(Response.Status.CREATED)
                    .entity(createResponse(true, "Factura creada exitosamente", factura))
                    .build();

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al crear factura desde payload /facturas", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }
    
}
