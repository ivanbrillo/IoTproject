
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"
#include <string.h>
#include "sys/log.h"
#include <stdlib.h> // For strtol

#define LOG_MODULE "BATTERY_RES"
#define LOG_LEVEL LOG_LEVEL_INFO

#define MIN_KW_BATTERY -10.0f
#define MAX_KW_BATTERY 10.0f

float last_battery_setpoint = 0.0f;
extern float current_soc;

static void res_post_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_battery_setpoint,
         "title=\"BatteryControl: setpoint=FLOAT], POST\";rt=\"battery-control\"",
         NULL,
         res_post_handler,
         NULL,
         NULL);

static void
res_post_handler(coap_message_t *request, coap_message_t *response,
                 uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
  size_t len = 0;
  const char *setpoint_query = NULL;
  float setpoint = 0.0f;

  LOG_INFO("Received Battery control POST request\n");

  if ((len = coap_get_query_variable(request, "setpoint", &setpoint_query)))
  {
    char temp_str[16]; 
    memcpy(temp_str, setpoint_query, len);
    temp_str[len] = '\0';

    char *endptr;
    setpoint = strtof(temp_str, &endptr);
    if (endptr != temp_str && *endptr == '\0' && setpoint >= MIN_KW_BATTERY && setpoint <= MAX_KW_BATTERY) // suppose the battery is inside this functional limit
    {

      if (current_soc <= 0.1f && setpoint < 0.0f)
      {
        // Battery is empty, cannot set negative setpoint
        snprintf((char *)buffer, preferred_size, "{\"error_code\":\"BATTERY_EMPTY\",\"message\":\"cannot set negative setpoint\"}");
        coap_set_status_code(response, BAD_REQUEST_4_00);
        coap_set_header_content_format(response, APPLICATION_JSON);
        coap_set_payload(response, buffer, strlen((char *)buffer));
        return;
      }

      if (current_soc >= 99.0f && setpoint > 0.0f)
      {
        // Battery is full, cannot set positive setpoint
        snprintf((char *)buffer, preferred_size, "{\"error_code\":\"BATTERY_FULL\",\"message\":\"cannot set positive setpoint\"}");
        coap_set_status_code(response, BAD_REQUEST_4_00);
        coap_set_header_content_format(response, APPLICATION_JSON);
        coap_set_payload(response, buffer, strlen((char *)buffer));
        return;
      }

      // Apply setpoint and power ON
      snprintf((char *)buffer, preferred_size, "{\"setpoint\":%.2f}", setpoint);
      coap_set_status_code(response, CHANGED_2_04);
      coap_set_header_content_format(response, APPLICATION_JSON);
      coap_set_payload(response, buffer, strlen((char *)buffer));
      last_battery_setpoint = setpoint; // Update the last setpoint
      return;
    }

    coap_set_status_code(response, BAD_REQUEST_4_00);
    snprintf((char *)buffer, preferred_size, "{\"error_code\":\"INVALID_SETPOINT\"}");
    coap_set_header_content_format(response, APPLICATION_JSON);
    coap_set_payload(response, buffer, strlen((char *)buffer));
    return;
  }
}
