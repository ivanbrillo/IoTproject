
#include <stdio.h>
#include <string.h>
#include "coap-engine.h"
#include "coap.h"

/* Log configuration */
#include "sys/log.h"
#include "ml-prediction.h"

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
  coap_set_payload(response, buffer, snprintf((char *)buffer, preferred_size, "{\"pred\": %.3f, \"last\": [%.3f, %.3f], \"v\": %d}", last_prediction, last_reading[0], last_reading[1], event_counter));
  event_counter++;
}

static void res_event_handler(void)
{
  LOG_INFO("PREDICTION %u: %.3f LAST: {%.3f, %.3f}\n", (unsigned)event_counter, last_prediction, last_reading[0], last_reading[1]);
  // always sent, for logging reason in the cloud application
  coap_notify_observers(&res_power);
}
