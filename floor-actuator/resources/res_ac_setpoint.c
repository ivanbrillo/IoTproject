#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"

#include <string.h>

/* Log configuration */
#include "sys/log.h"
#include <stdlib.h> // For strtof
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_APP

static void res_post_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

/* AC Control Resource */
RESOURCE(res_ac_setpoint,
         "title=\"ACcontrol: ?on=0|1[&setpoint=FLOAT], POST\";rt=\"ac-control\"",
         NULL,
         res_post_handler,
         NULL,
         NULL);

static void
res_post_handler(coap_message_t *request, coap_message_t *response,
                 uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
  size_t len = 0;
  const char *on_query = NULL;
  const char *setpoint_query = NULL;
  int ac_on = -1;
  float setpoint = -1.0f;

  LOG_INFO_("Received AC control POST request\n");

  // Check for 'on' parameter first
  if ((len = coap_get_query_variable(request, "on", &on_query)))
  {
    if (len == 1 && (on_query[0] == '0' || on_query[0] == '1'))
    {
      ac_on = (on_query[0] == '1') ? 1 : 0;

      if (ac_on == 0)
      {
        // AC turned OFF — skip setpoint handling
        snprintf((char *)buffer, preferred_size, "{\"status\":\"AC_OFF\"}");
        coap_set_status_code(response, CHANGED_2_04);
        coap_set_header_content_format(response, APPLICATION_JSON);
        coap_set_payload(response, buffer, strlen((char *)buffer));
        return;
      }

      // If AC is ON, now parse the setpoint
      if ((len = coap_get_query_variable(request, "setpoint", &setpoint_query)))
      {
        char temp_str[16];
        memcpy(temp_str, setpoint_query, len);
        temp_str[len] = '\0';

        char *endptr;
        setpoint = strtof(temp_str, &endptr);
        if (endptr != temp_str && *endptr == '\0' && setpoint >= 16.0f && setpoint <= 30.0f)
        {
          // Apply setpoint and power ON
          snprintf((char *)buffer, preferred_size, "{\"status\":\"AC_ON\",\"setpoint\":%.2f}", setpoint);
          coap_set_status_code(response, CHANGED_2_04);
          coap_set_header_content_format(response, APPLICATION_JSON);
          coap_set_payload(response, buffer, strlen((char *)buffer));
          return;
        }
        else
        {
          coap_set_status_code(response, BAD_REQUEST_4_00);
          snprintf((char *)buffer, preferred_size, "{\"error_code\":\"INVALID_SETPOINT\"}");
          coap_set_header_content_format(response, APPLICATION_JSON);
          coap_set_payload(response, buffer, strlen((char *)buffer));
          return;
        }
      }
      else
      {
        coap_set_status_code(response, BAD_REQUEST_4_00);
        snprintf((char *)buffer, preferred_size, "{\"error_code\":\"MISSING_SETPOINT\"}");
        coap_set_header_content_format(response, APPLICATION_JSON);
        coap_set_payload(response, buffer, strlen((char *)buffer));
        return;
      }
    }
    else
    {
      coap_set_status_code(response, BAD_REQUEST_4_00);
      snprintf((char *)buffer, preferred_size, "{\"error_code\":\"INVALID_ON_VALUE\"}");
      coap_set_header_content_format(response, APPLICATION_JSON);
      coap_set_payload(response, buffer, strlen((char *)buffer));
      return;
    }
  }
  else
  {
    coap_set_status_code(response, BAD_REQUEST_4_00);
    snprintf((char *)buffer, preferred_size, "{\"error_code\":\"MISSING_ON_PARAMETER\"}");
    coap_set_header_content_format(response, APPLICATION_JSON);
    coap_set_payload(response, buffer, strlen((char *)buffer));
    return;
  }
}