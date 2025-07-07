
#include <stdio.h>
#include <string.h>
#include "coap-engine.h"
#include "coap.h"

/* Log configuration */
#include "sys/log.h"
#include "ml-prediction.h"
#include "float_helper.h"

#define LOG_MODULE "POWER_RES"
#define LOG_LEVEL LOG_LEVEL_INFO

float last_prediction = 0.0f;
float last_reading[N_FEATURES] = {0.0f, 0.0f};

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_event_handler(void);


EVENT_RESOURCE(res_power,
               "title=\"Power\";obs",
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
  char buff3[10];
  
  float_to_string(buff1, last_prediction);
  float_to_string(buff2, last_reading[0]);
  float_to_string(buff3, last_reading[1]);

  coap_set_payload(response, buffer, snprintf((char *)buffer, preferred_size, "{\"pred\": %s, \"last\": [%s, %s], \"v\": %ld}", buff1, buff2, buff3, event_counter));
  event_counter++;
}

static void res_event_handler(void)
{
  LOG_INFO("PREDICTION %ld: %d LAST: {%d, %d}\n", event_counter, (int)last_prediction, (int)last_reading[0], (int)last_reading[1]);
  // always sent, for logging reason in the cloud application
  coap_notify_observers(&res_power);
}
