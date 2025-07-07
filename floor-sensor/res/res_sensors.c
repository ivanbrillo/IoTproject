
#include <stdio.h>
#include <string.h>
#include "coap-engine.h"
#include "coap.h"
#include <math.h>
#include "sys/log.h"
#include "float_helper.h"

// Helper lambda/function to check >5% change (absolute relative difference)
#define CHANGE_ABOVE_5_PERCENT(old, new) (fabsf((new) - (old)) / ((old) != 0 ? fabsf(old) : 1.0f) > 0.05f)

#define LOG_MODULE "SENSOR_RES"
#define LOG_LEVEL LOG_LEVEL_APP

#define INITIAL_LIGHT 200.0f
float current_light = INITIAL_LIGHT;
float last_sended_light = INITIAL_LIGHT;

#define INITIAL_TEMP 22.0f
float current_temperature = INITIAL_TEMP;
float last_sended_temperature = INITIAL_TEMP;

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_event_handler(void);

EVENT_RESOURCE(res_sensors,
               "title=\"Sensors\";obs",
               res_get_handler,
               NULL,
               NULL,
               NULL,
               res_event_handler);

static int32_t event_counter = 0;

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
  coap_set_header_content_format(response, APPLICATION_JSON);

  char buff1[10];
  char buff2[10];

  float_to_string(buff1, current_light);
  float_to_string(buff2, current_temperature);

  coap_set_payload(response, buffer, snprintf((char *)buffer, preferred_size, "{\"light\": %s, \"temp\": %s, \"v\":%ld}", buff1, buff2, event_counter));
  // coap_set_payload(response, buffer, snprintf((char *)buffer, preferred_size, "{\"light\": %.3f, \"temp\": %.3f, \"v\":%ld}", current_light, current_temperature, event_counter));
  event_counter++;
}

static void res_event_handler(void)
{
  // send only if one of the two changes between the last sended value is above 5%
  if (CHANGE_ABOVE_5_PERCENT(last_sended_temperature, current_temperature) || CHANGE_ABOVE_5_PERCENT(last_sended_light, current_light))
  {
    LOG_INFO("LIGHT %d\n", (int)current_light);
    LOG_INFO("TEMP %d\n", (int)current_temperature);

    last_sended_light = current_light;
    last_sended_temperature = current_temperature;

    coap_notify_observers(&res_sensors);
  }
}
