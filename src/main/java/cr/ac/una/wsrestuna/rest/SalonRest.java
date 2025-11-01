package cr.ac.una.wsrestuna.rest;

import cr.ac.una.wsrestuna.model.Mesa;
import cr.ac.una.wsrestuna.model.Salon;
import cr.ac.una.wsrestuna.service.OrdenService;
import cr.ac.una.wsrestuna.service.SalonService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/salones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SalonRest {

    private static final Logger LOG = Logger.getLogger(SalonRest.class.getName());

    @EJB
    private SalonService salonService;
    @EJB
    private OrdenService ordenService;

    // ==================== ENDPOINTS DE SALONES ====================

    @GET
    public Response findAll() {
        try {
            List<Salon> salones = salonService.findActivos();
            
            // ⭐ CONVERTIR A DTOs SIN IMAGEN (para listar en tabla)
            List<Map<String, Object>> salonesDTO = new ArrayList<>();
            for (Salon s : salones) {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", s.getId());
                dto.put("nombre", s.getNombre());
                dto.put("tipo", s.getTipo());
                dto.put("cobraServicio", s.getCobraServicio());
                dto.put("estado", s.getEstado());
                dto.put("version", s.getVersion());
                // NO incluir imagenMesa ni tipoImagen
                salonesDTO.add(dto);
            }
            
            return Response.ok(createResponse(true, "Salones obtenidos", salonesDTO)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener salones", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Long id) {
        try {
            Optional<Salon> salon = salonService.findById(id);
            
            if (salon.isPresent()) {
                // ⭐ AQUÍ SÍ incluye la imagen completa
                return Response.ok(createResponse(true, "Salón encontrado", salon.get())).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Salón no encontrado", null))
                        .build();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar salón", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
    @Path("/tipo/salon")
    public Response findSalones() {
        try {
            List<Salon> salones = salonService.findSalones();
            // ⭐ AQUÍ SÍ incluye imágenes porque se usa en VistaSalones
            return Response.ok(createResponse(true, "Salones obtenidos", salones)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener salones", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @POST
    public Response create(Salon salon) {
        try {
            if (salon == null || salon.getNombre() == null || salon.getNombre().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createResponse(false, "El nombre del salón es obligatorio", null))
                        .build();
            }

            if (salon.getTipo() == null || (!salon.getTipo().equals("SALON") && !salon.getTipo().equals("BARRA"))) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createResponse(false, "Tipo inválido", null))
                        .build();
            }

            Salon created = salonService.create(salon);
            return Response.status(Response.Status.CREATED)
                    .entity(createResponse(true, "Salón creado exitosamente", created))
                    .build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al crear salón", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, Salon salon) {
        try {
            if (!salonService.findById(id).isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Salón no encontrado", null))
                        .build();
            }

            salon.setId(id);
            Salon updated = salonService.update(salon);
            return Response.ok(createResponse(true, "Salón actualizado exitosamente", updated)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al actualizar salón", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        try {
            if (!salonService.findById(id).isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Salón no encontrado", null))
                        .build();
            }

            salonService.delete(id);
            return Response.ok(createResponse(true, "Salón desactivado exitosamente", null)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al eliminar salón", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    // ==================== ENDPOINTS DE MESAS (SIN CAMBIOS) ====================

    @GET
    @Path("/{salonId}/mesas")
    public Response findMesasBySalon(@PathParam("salonId") Long salonId) {
        try {
            if (!salonService.findById(salonId).isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Salón no encontrado", null))
                        .build();
            }

            List<Mesa> mesas = salonService.findMesasBySalon(salonId);

            // ⭐ SINCRONIZAR ESTADO CON ÓRDENES ACTIVAS
            List<Map<String, Object>> mesasDTO = new ArrayList<>();
            for (Mesa mesa : mesas) {
                // Verificar si realmente tiene orden activa
                boolean tieneOrdenActiva = ordenService.mesaTieneOrdenActiva(mesa.getId());

                // Actualizar estado si es necesario
                String estadoReal = tieneOrdenActiva ? "OCUPADA" : "LIBRE";

                // ⚠️ IMPORTANTE: Si el estado en BD no coincide, actualizarlo
                if (!estadoReal.equals(mesa.getEstado())) {
                    LOG.log(Level.WARNING,
                            "⚠️ Mesa {0} desincronizada. BD={1}, Real={2}. Corrigiendo...",
                            new Object[]{mesa.getIdentificador(), mesa.getEstado(), estadoReal});

                    mesa.setEstado(estadoReal);
                    salonService.updateMesa(mesa); // Actualizar en BD
                }

                Map<String, Object> dto = new HashMap<>();
                dto.put("id", mesa.getId());
                dto.put("salonId", mesa.getSalonId());
                dto.put("identificador", mesa.getIdentificador());
                dto.put("posicionX", mesa.getPosicionX());
                dto.put("posicionY", mesa.getPosicionY());
                dto.put("estado", estadoReal); // ⭐ Estado sincronizado
                dto.put("version", mesa.getVersion());

                mesasDTO.add(dto);
            }

            return Response.ok(createResponse(true, "Mesas obtenidas", mesasDTO)).build();

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener mesas", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @POST
    @Path("/{salonId}/mesas")
    public Response createMesa(@PathParam("salonId") Long salonId, Map<String, Object> mesaData) {
        try {
            Optional<Salon> salon = salonService.findById(salonId);
            if (!salon.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Salón no encontrado", null))
                        .build();
            }

            if (!mesaData.containsKey("identificador")
                    || mesaData.get("identificador").toString().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createResponse(false, "El identificador es obligatorio", null))
                        .build();
            }

            Mesa mesa = new Mesa();
            mesa.setSalon(salon.get());
            mesa.setIdentificador(mesaData.get("identificador").toString().trim());
            mesa.setPosicionX(mesaData.containsKey("posicionX")
                    ? Double.parseDouble(mesaData.get("posicionX").toString()) : 0.0);
            mesa.setPosicionY(mesaData.containsKey("posicionY")
                    ? Double.parseDouble(mesaData.get("posicionY").toString()) : 0.0);
            mesa.setEstado("LIBRE");

            Mesa created = salonService.createMesa(mesa);

            Map<String, Object> dto = new HashMap<>();
            dto.put("id", created.getId());
            dto.put("salonId", created.getSalonId());
            dto.put("identificador", created.getIdentificador());
            dto.put("posicionX", created.getPosicionX());
            dto.put("posicionY", created.getPosicionY());
            dto.put("estado", created.getEstado());
            dto.put("version", created.getVersion());

            return Response.status(Response.Status.CREATED)
                    .entity(createResponse(true, "Mesa creada exitosamente", dto))
                    .build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al crear mesa", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @PUT
    @Path("/mesas/{mesaId}")
    public Response updateMesa(@PathParam("mesaId") Long mesaId, Map<String, Object> mesaData) {
        try {
            Optional<Mesa> existente = salonService.findMesaById(mesaId);
            if (!existente.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Mesa no encontrada", null))
                        .build();
            }

            Mesa mesa = existente.get();
            if (mesaData.containsKey("identificador")) {
                mesa.setIdentificador(mesaData.get("identificador").toString().trim());
            }
            if (mesaData.containsKey("posicionX")) {
                mesa.setPosicionX(Double.parseDouble(mesaData.get("posicionX").toString()));
            }
            if (mesaData.containsKey("posicionY")) {
                mesa.setPosicionY(Double.parseDouble(mesaData.get("posicionY").toString()));
            }

            Mesa updated = salonService.updateMesa(mesa);
            
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", updated.getId());
            dto.put("salonId", updated.getSalonId());
            dto.put("identificador", updated.getIdentificador());
            dto.put("posicionX", updated.getPosicionX());
            dto.put("posicionY", updated.getPosicionY());
            dto.put("estado", updated.getEstado());
            dto.put("version", updated.getVersion());
            
            return Response.ok(createResponse(true, "Mesa actualizada exitosamente", dto)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al actualizar mesa", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @DELETE
    @Path("/mesas/{mesaId}")
    public Response deleteMesa(@PathParam("mesaId") Long mesaId) {
        try {
            if (!salonService.findMesaById(mesaId).isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Mesa no encontrada", null))
                        .build();
            }

            salonService.deleteMesa(mesaId);
            return Response.ok(createResponse(true, "Mesa eliminada exitosamente", null)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al eliminar mesa", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @PUT
    @Path("/mesas/posiciones")
    public Response actualizarPosiciones(List<Map<String, Object>> mesasData) {
        try {
            if (mesasData == null || mesasData.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createResponse(false, "No se enviaron mesas", null))
                        .build();
            }

            List<Mesa> mesas = new ArrayList<>();
            for (Map<String, Object> data : mesasData) {
                if (!data.containsKey("id")) continue;
                
                Long id = Long.parseLong(data.get("id").toString());
                Optional<Mesa> mesaOpt = salonService.findMesaById(id);
                
                if (mesaOpt.isPresent()) {
                    Mesa mesa = mesaOpt.get();
                    if (data.containsKey("posicionX")) {
                        mesa.setPosicionX(Double.parseDouble(data.get("posicionX").toString()));
                    }
                    if (data.containsKey("posicionY")) {
                        mesa.setPosicionY(Double.parseDouble(data.get("posicionY").toString()));
                    }
                    mesas.add(mesa);
                }
            }

            salonService.actualizarPosicionesMesas(mesas);
            return Response.ok(createResponse(true, "Posiciones actualizadas", null)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al actualizar posiciones", e);
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