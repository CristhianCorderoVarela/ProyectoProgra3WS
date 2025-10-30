window.onload = function() {
  const ui = SwaggerUIBundle({
    url: "/ProyectoProgra3WS/api/openapi.json",
    dom_id: '#swagger-ui',
    deepLinking: true,
    presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
    layout: "StandaloneLayout"
  });
  window.ui = ui;
};