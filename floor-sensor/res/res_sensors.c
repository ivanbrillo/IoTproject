
#include <stdio.h>
#include <string.h>
#include "coap-engine.h"
#include "coap.h"

/* Log configuration */
#include "sys/log.h"

#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_APP

#define INITIAL_LIGHT 200.0f
float last_light = INITIAL_LIGHT;

#define INITIAL_TEMP 22.0f
float last_temperature = INITIAL_TEMP;

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_event_handler(void);

/*
 * Example for an event resource.
 * Additionally takes a period parameter that defines the interval to call [name]_periodic_handler().
 * A default post_handler takes care of subscriptions and manages a list of subscribers to notify.
 */
EVENT_RESOURCE(res_sensors,
               "title=\"Sensors\";obs",
               res_get_handler,
               NULL,
               NULL,
               NULL,
               res_event_handler);

static int32_t event_counter = 0;

static void
res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
  coap_set_header_content_format(response, APPLICATION_JSON);
  coap_set_payload(response, buffer, snprintf((char *)buffer, preferred_size, "{\"light\": %.3f, \"temp\": %.3f, \"v\":%d}", last_light, last_temperature, event_counter));
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

  /* Usually a condition is defined under with subscribers are notified, e.g., event was above a threshold. */
  if (1)
  {
    printf("LIGHT %.3f\n", last_light);
    printf("TEMP %.3f\n", last_temperature);

    /* Notify the registered observers which will trigger the res_get_handler to create the response. */
    coap_notify_observers(&res_sensors);
  }
}
