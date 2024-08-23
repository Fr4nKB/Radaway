#include "contiki.h"
#include "coap-engine.h"
#include "os/dev/leds.h"
#include <string.h>
#include <stdio.h>
#include <stdlib.h>

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_APP

static int coolant_flow_perc = 0;

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

EVENT_RESOURCE(
	res_coolant_flow,
	"title=\"Coolant Flow \" GET, PUT mode=0|1|2;rt=\"coolant_flow_perc\"",
	res_get_handler,
	NULL,
	NULL,
	res_put_handler,
	NULL
);

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	coap_set_header_content_format(response, APPLICATION_JSON);
	sprintf((char *)buffer, "{\"status\": %d}", coolant_flow_perc);
	coap_set_payload(response, buffer, strlen((char *)buffer));
}

static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	size_t len = 0;
	const char *mode = NULL;
	bool success = false;

	if ((len = coap_get_query_variable(request, "mode", &mode))) {

		if (strncmp(mode, "0", len) == 0) {   // reactor off
			coap_set_status_code(response, CHANGED_2_04);
			coolant_flow_perc = 0;
			success = true;
			leds_set(LEDS_RED);
		}

		else if (strncmp(mode, "1", len) == 0) {   // min coolant flow
			coap_set_status_code(response, CHANGED_2_04);
			coolant_flow_perc = 70;
			success = true;
			leds_set(LEDS_YELLOW);
		}

		else if (strncmp(mode, "2", len) == 0) {  // max coolant flow
			coap_set_status_code(response, CHANGED_2_04);
			coolant_flow_perc = 100;
			success = true;
			leds_set(LEDS_BLUE);
		}
		else coap_set_status_code(response, BAD_OPTION_4_02);
	}
	
	if (!success){
		coap_set_status_code(response, BAD_REQUEST_4_00);
	}

	LOG_INFO("Coolant flow at %d\n", coolant_flow_perc);
}
