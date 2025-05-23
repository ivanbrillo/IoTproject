
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"

#include <string.h>

/* Log configuration */
#include "sys/log.h"
#include <stdlib.h> // For strtol
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_RPL

#define MIN_KW_BATTERY -10.0f
#define MAX_KW_BATTERY 10.0f

float last_battery_setpoint = 0.0f;
extern float last_soc;

static void res_post_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

/* A simple actuator example, depending on the color query parameter and post variable mode, corresponding led is activated or deactivated */
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

  LOG_INFO_("Received AC control POST request\n");

  if ((len = coap_get_query_variable(request, "setpoint", &setpoint_query)))
  {
    char temp_str[16]; // increase buffer to accommodate decimal values
    memcpy(temp_str, setpoint_query, len);
    temp_str[len] = '\0';

    char *endptr;
    setpoint = strtof(temp_str, &endptr);
    if (endptr != temp_str && *endptr == '\0' && setpoint >= MIN_KW_BATTERY && setpoint <= MAX_KW_BATTERY) // suppose the battery is inside this functional limit
    {

      if (last_soc <= 0.1f && setpoint < 0.0f)
      {
        // Battery is empty, cannot set negative setpoint
        snprintf((char *)buffer, preferred_size, "Battery empty, cannot set negative setpoint");
        coap_set_status_code(response, BAD_REQUEST_4_00);
        coap_set_payload(response, buffer, strlen((char *)buffer));
        return;
      }

      if (last_soc >= 99.0f && setpoint > 0.0f)
      {
        // Battery is full, cannot set positive setpoint
        snprintf((char *)buffer, preferred_size, "Battery full, cannot set positive setpoint");
        coap_set_status_code(response, BAD_REQUEST_4_00);
        coap_set_payload(response, buffer, strlen((char *)buffer));
        return;
      }

      // Apply setpoint and power ON
      snprintf((char *)buffer, preferred_size, "BATTERY SOC Setpoint: %.2f, last set: %.2f, last soc: %.2f, ", setpoint, last_battery_setpoint, last_soc);
      coap_set_status_code(response, CHANGED_2_04);
      coap_set_payload(response, buffer, strlen((char *)buffer));
      last_battery_setpoint = setpoint; // Update the last setpoint
      return;
    }

    coap_set_status_code(response, BAD_REQUEST_4_00);
    snprintf((char *)buffer, preferred_size, "Invalid SOC setpoint");
    coap_set_payload(response, buffer, strlen((char *)buffer));
    return;
  }
}
