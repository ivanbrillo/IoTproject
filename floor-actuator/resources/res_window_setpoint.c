#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"
#include <string.h>
#include <stdlib.h> // for strtof

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_APP

static void res_post_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

/* A simple actuator example that sets a window opening percentage */
RESOURCE(res_window_setpoint,
         "title=\"WindowSetpoint: ?setpoint=FLOAT, POST\";rt=\"window-setpoint\"",
         NULL,
         res_post_handler,
         NULL,
         NULL);

static void
res_post_handler(coap_message_t *request, coap_message_t *response,
                 uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
  size_t len = 0;
  const char *query;

  LOG_INFO("Received window setpoint POST request\n");

  if ((len = coap_get_query_variable(request, "setpoint", &query))) {
    char temp_str[16];
    memcpy(temp_str, query, len);
    temp_str[len] = '\0';

    char *endptr;
    float value = strtof(temp_str, &endptr);

    if (endptr == temp_str || *endptr != '\0') {
      coap_set_status_code(response, BAD_REQUEST_4_00);
      snprintf((char *)buffer, preferred_size, "Invalid input: not a float");
      coap_set_payload(response, buffer, strlen((char *)buffer));
      return;
    }

    if (value >= 0.0f && value <= 100.0f) {
      // Set global or hardware window opening value here
      snprintf((char *)buffer, preferred_size, "Window setpoint updated to %.2f%%", value);
      coap_set_payload(response, buffer, strlen((char *)buffer));
      coap_set_status_code(response, CHANGED_2_04);
    } else {
      coap_set_status_code(response, BAD_REQUEST_4_00);
      snprintf((char *)buffer, preferred_size, "Invalid value: %.2f (allowed 0–100)", value);
      coap_set_payload(response, buffer, strlen((char *)buffer));
    }

  } else {
    coap_set_status_code(response, BAD_REQUEST_4_00);
    snprintf((char *)buffer, preferred_size, "Missing 'setpoint' parameter");
    coap_set_payload(response, buffer, strlen((char *)buffer));
  }
}
