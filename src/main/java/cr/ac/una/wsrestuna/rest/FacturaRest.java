package cr.ac.una.wsrestuna.rest;

import cr.ac.una.wsrestuna.model.DetalleFactura;
import cr.ac.una.wsrestuna.model.Factura;
import cr.ac.una.wsrestuna.service.FacturaService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /**
     * **FIX DEFINITIVO: Recibir porcentaje directamente del frontend**
     */
    @POST
    public Response create(Map<String, Object> datos) {
        try {
            // ===== 1. IDs básicos =====
            Long ordenId = Long.valueOf(datos.get("ordenId").toString());

            Long clienteId = null;
            if (datos.get("clienteId") != null) {
                clienteId = Long.valueOf(datos.get("clienteId").toString());
            }

            // ===== 2. Resumen (totales calculados en frontend) =====
            @SuppressWarnings("unchecked")
            Map<String, Object> resumen = (Map<String, Object>) datos.get("resumen");

            // Subtotal
            BigDecimal subtotal = new BigDecimal(
                    resumen.get("subtotal").toString()
            ).setScale(2, RoundingMode.HALF_UP);

            // Montos de impuestos
            BigDecimal impVentaMonto = new BigDecimal(
                    resumen.getOrDefault("impuestoVentas", "0").toString()
            ).setScale(2, RoundingMode.HALF_UP);
            
            BigDecimal impServicioMonto = new BigDecimal(
                    resumen.getOrDefault("impuestoServicio", "0").toString()
            ).setScale(2, RoundingMode.HALF_UP);

            boolean aplicaImpVenta = impVentaMonto.compareTo(BigDecimal.ZERO) > 0;
            boolean aplicaImpServicio = impServicioMonto.compareTo(BigDecimal.ZERO) > 0;

            // **FIX DEFINITIVO: Recibir el porcentaje directamente del frontend**
            BigDecimal descuentoPct = BigDecimal.ZERO;
            if (resumen.containsKey("descuentoPorcentaje")) {
                descuentoPct = new BigDecimal(resumen.get("descuentoPorcentaje").toString())
                    .setScale(2, RoundingMode.HALF_UP);
            }
            
            LOG.log(Level.INFO, String.format(
                "Factura recibida: Subtotal=%.2f, ImpVenta=%.2f, ImpServ=%.2f, DescPct=%.2f%%",
                subtotal, impVentaMonto, impServicioMonto, descuentoPct));

            // ===== 3. Pagos =====
            @SuppressWarnings("unchecked")
            Map<String, Object> pagos = (Map<String, Object>) datos.get("pagos");

            BigDecimal montoEfectivo = new BigDecimal(
                    pagos.getOrDefault("efectivo", "0").toString()
            ).setScale(2, RoundingMode.HALF_UP);
            
            BigDecimal montoTarjeta = new BigDecimal(
                    pagos.getOrDefault("tarjeta", "0").toString()
            ).setScale(2, RoundingMode.HALF_UP);

            // ===== 4. Crear la factura =====
            // usuarioId se resuelve internamente desde la orden
            Factura factura = facturaService.createFromOrden(
                    ordenId,
                    clienteId,
                    null, // usuarioId: se infiere de la orden
                    aplicaImpVenta,
                    aplicaImpServicio,
                    descuentoPct, // **PORCENTAJE CORRECTO RECIBIDO DEL FRONTEND**
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

    private Map<String, Object> createResponse(boolean success, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("data", data);
        return response;
    }
}