
#include <stdio.h>
#include <string.h>
#include "coap-engine.h"
#include "coap.h"

/* Log configuration */
#include "sys/log.h"
#include "ml-prediction.h"

#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_INFO


float last_prediction = 0.0f;
float last_reading[N_FEATURES] = {0.0f, 0.0f};

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_event_handler(void);

/*
 * Example for an event resource.
 * Additionally takes a period parameter that defines the interval to call [name]_periodic_handler().
 * A default post_handler takes care of subscriptions and manages a list of subscribers to notify.
 */
EVENT_RESOURCE(res_power,
               "title=\"Power\";obs",
               res_get_handler,
               NULL,
               NULL,
               NULL,
               res_event_handler);

/*
 * Use local resource state that is accessed by res_get_handler() and altered by res_event_handler() or PUT or POST.
 */
static int32_t event_counter = 0;

static void
res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
  coap_set_header_content_format(response, APPLICATION_JSON);
  coap_set_payload(response, buffer, snprintf((char *)buffer, preferred_size, "{\"pred\": %.3f, \"last\": [%.3f, %.3f], \"v\": %d}", last_prediction, last_reading[0], last_reading[1], event_counter));
  event_counter++;
  /* A post_handler that handles subscriptions/observing will be called for periodic resources by the framework. */
}
/*
 * Additionally, res_event_handler must be implemented for each EVENT_RESOURCE.
 * It is called through <res_name>.trigger(), usually from the server process.
 */
static void
res_event_handler(void)
{
  /* Do the update triggered by the event here, e.g., sampling a sensor. */

  /* Usually a condition is defined under with subscribers are notified, e.g., event was above a threshold. */
  if (1)
  {
    LOG_INFO("PREDICTION %u: %.3f LAST: {%.3f, %.3f}\n", (unsigned)event_counter, last_prediction, last_reading[0], last_reading[1]);

    /* Notify the registered observers which will trigger the res_get_handler to create the response. */
    coap_notify_observers(&res_power);
  }
}
