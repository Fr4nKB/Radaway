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

static int control_rods_insertion_perc = 100;

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

EVENT_RESOURCE(
	res_control_rods,
	"title=\"Control Rods \" GET, PUT mode=0|1|2;rt=\"actuator\"",
	res_get_handler,
	NULL,
	NULL,
	res_put_handler, 
	NULL
);

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
    coap_set_header_content_format(response, APPLICATION_JSON);
    sprintf((char *)buffer, "{\"status\": %d}", control_rods_insertion_perc);
    coap_set_payload(response, buffer, strlen((char*)buffer));
}

static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	size_t len = 0;
	const char *mode = NULL;
	bool success = false;

	if((len = coap_get_query_variable(request, "mode", &mode))) {
	
		if(strncmp(mode, "0", len) == 0) {   // low -> 10% of control rods fully inserted
			coap_set_status_code(response, CHANGED_2_04);
			control_rods_insertion_perc = 10;
			success = true;
			leds_set(LEDS_BLUE);
		}
		
		else if(strncmp(mode, "1", len) == 0) {   // mid -> 50% of control rods fully inserted
			coap_set_status_code(response, CHANGED_2_04);
			control_rods_insertion_perc = 50;
			success = true;
			leds_set(LEDS_YELLOW);
		}
		
		else if(strncmp(mode, "2", len) == 0) {   // shut down reactor -> 100% of control rods fully inserted
			coap_set_status_code(response, CHANGED_2_04);
			control_rods_insertion_perc = 100;
			success = true;
			leds_set(LEDS_RED);
		}
		else coap_set_status_code(response, BAD_OPTION_4_02);
	} 
	
	if(!success) {
		coap_set_status_code(response, BAD_REQUEST_4_00);
	}
	
	LOG_INFO("Control rods inserted at %d %\n", control_rods_insertion_perc);
}
