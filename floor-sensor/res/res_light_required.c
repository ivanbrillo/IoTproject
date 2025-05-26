#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"
#include <string.h>
#include <stdlib.h> // for strtof
#include "sys/log.h"

#define LOG_MODULE "LIGHT_RES"
#define LOG_LEVEL LOG_LEVEL_APP

#define INITIAL_LIGHT 250.0f
float light_required = INITIAL_LIGHT;

static void res_post_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

/* A simple actuator example that sets a window opening percentage */
RESOURCE(res_light_required,
         "title=\"LightRequired: ?setpoint=FLOAT, POST\";rt=\"window-setpoint\"",
         NULL,
         res_post_handler,
         NULL,
         NULL);

static void
res_post_handler(coap_message_t *request, coap_message_t *response,
                 uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
  size_t len = 0;
  const char *query;

  LOG_INFO("Received light setpoint POST request\n");

  if ((len = coap_get_query_variable(request, "setpoint", &query)))
  {
    char light_str[16];
    memcpy(light_str, query, len);
    light_str[len] = '\0';

    char *endptr;
    float value = strtof(light_str, &endptr);

    if (endptr == light_str || *endptr != '\0')
    {
      coap_set_status_code(response, BAD_REQUEST_4_00);
      snprintf((char *)buffer, preferred_size, "{\"error_code\":\"INVALID_INPUT\",\"message\":\"not a float\"}");
      coap_set_header_content_format(response, APPLICATION_JSON);
      coap_set_payload(response, buffer, strlen((char *)buffer));
      return;
    }

    if (value >= 0.0f && value <= 1000.0f)
    {
      // Set global or hardware light setpoint value here
      snprintf((char *)buffer, preferred_size, "{\"status\":\"success\", \"setpoint\":%.2f}", value);
      coap_set_header_content_format(response, APPLICATION_JSON);
      coap_set_payload(response, buffer, strlen((char *)buffer));
      coap_set_status_code(response, CHANGED_2_04);
      light_required = value; // Update the global variable
    }
    else
    {
      coap_set_status_code(response, BAD_REQUEST_4_00);
      snprintf((char *)buffer, preferred_size, "{\"error_code\":\"INVALID_VALUE\"}");
      coap_set_header_content_format(response, APPLICATION_JSON);
      coap_set_payload(response, buffer, strlen((char *)buffer));
    }
  }
  else
  {
    coap_set_status_code(response, BAD_REQUEST_4_00);
    snprintf((char *)buffer, preferred_size, "{\"error_code\":\"MISSING_SETPOINT\"}");
    coap_set_header_content_format(response, APPLICATION_JSON);
    coap_set_payload(response, buffer, strlen((char *)buffer));
  }
}