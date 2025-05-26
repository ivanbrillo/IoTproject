
#include <stdio.h>
#include <string.h>
#include "coap-engine.h"
#include "coap.h"

/* Log configuration */
#include "sys/log.h"
#include "ml-prediction.h"

#define LOG_MODULE "BATTERY_SOC_RES"
#define LOG_LEVEL LOG_LEVEL_INFO

float last_sended_soc = 0.0f;
float current_soc = 0.0f;

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_event_handler(void);

EVENT_RESOURCE(res_battery_soc,
               "title=\"Battery SOC\";obs",
               res_get_handler,
               NULL,
               NULL,
               NULL,
               res_event_handler);

static int32_t event_counter = 0;

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
  coap_set_header_content_format(response, APPLICATION_JSON);
  coap_set_payload(response, buffer, snprintf((char *)buffer, preferred_size, "{\"soc\": %.3f, \"v\": %d}", current_soc, event_counter));
  event_counter++;
}

static void res_event_handler(void)
{
  if ((last_sended_soc - current_soc) * (last_sended_soc - current_soc) > 0.5 * 0.5) // if the change is more than 0.5%
  {
    last_sended_soc = current_soc; // update last soc
    LOG_INFO("SOC changed to %.3f\n", current_soc);
    coap_notify_observers(&res_battery_soc);
  }
}
