#include "contiki.h"
#include "coap-engine.h"
#include "sys/log.h"
#include <string.h>

#define LOG_MODULE "DYNAMIC_CTRL"
#define LOG_LEVEL LOG_LEVEL_APP

int dynamic_control = 1;

static void res_dynamic_post_handler(coap_message_t *request, coap_message_t *response,
                                     uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_dynamic_control,
         "title=\"DynamicControl: ?on=0|1 POST\";rt=\"dynamic-control\"",
         NULL,
         res_dynamic_post_handler,
         NULL,
         NULL);

static void
res_dynamic_post_handler(coap_message_t *request, coap_message_t *response,
                         uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
  size_t len = 0;
  const char *on_query = NULL;

  LOG_INFO("Received dynamic control POST request\n");

  if ((len = coap_get_query_variable(request, "on", &on_query))) {
    if (len == 1 && (on_query[0] == '0' || on_query[0] == '1')) {
      dynamic_control = (on_query[0] == '1') ? 1 : 0;
      LOG_INFO("Dynamic control set to %d\n", dynamic_control);

      snprintf((char *)buffer, preferred_size, "{\"status\":\"Dynamic Control %s\"}", dynamic_control ? "enabled" : "disabled");
      coap_set_status_code(response, CHANGED_2_04);
    } else {
      coap_set_status_code(response, BAD_REQUEST_4_00);
      snprintf((char *)buffer, preferred_size, "{\"error_code\":\"INVALID_ON_VALUE\"}");
    }
  } else {
    coap_set_status_code(response, BAD_REQUEST_4_00);
    snprintf((char *)buffer, preferred_size, "{\"error_code\":\"MISSING_ON_PARAMETER\"}");
  }

  coap_set_header_content_format(response, APPLICATION_JSON);
  coap_set_payload(response, buffer, strlen((char *)buffer));
}
