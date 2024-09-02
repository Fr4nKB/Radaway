#include "contiki.h"
#include "coap-engine.h"
#include "coap-blocking-api.h"
#include "net/ipv6/uip.h"
#include "net/ipv6/uip-ds6.h"
#include "sys/etimer.h"
#include "os/dev/leds.h"
#include <stdio.h>

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_INFO

#define SERVER_EP "coap://[fd00::1]:5683"

static coap_endpoint_t server_ep;
static coap_message_t request[1];       /* This way  the packet can be treated as pointer as usual. */

static char *service_registration_url = "/registration";
static char *registration_payload = "{\"name\":\"actuator_control_rods\",\"sensor_types\":[2]}";
static bool registration_status = false;

extern coap_resource_t res_control_rods;
static struct etimer sleep_timer;

void client_chunk_handler(coap_message_t *response) {
	if(response == NULL || response->code != 65) {
		LOG_INFO("REGISTRATION FAILED\n");
		return;
	}

	registration_status = true;
	LOG_INFO("CONTROL RODS REGISTRATION: DONE\n");
}

PROCESS(actuator, "actuator_control_rods");
AUTOSTART_PROCESSES(&actuator);

PROCESS_THREAD(actuator, ev, data) {
	
	PROCESS_BEGIN();
	leds_set(LEDS_RED);
	
	while(!registration_status) {
		// populate the coap_endpoint_t data structure
		coap_endpoint_parse(SERVER_EP, strlen(SERVER_EP), &server_ep);
		// prepare the message
		coap_init_message(request, COAP_TYPE_CON, COAP_POST, 0);
		coap_set_header_uri_path(request, service_registration_url);

		// set payload
		coap_set_payload(request, (uint8_t *)registration_payload, strlen(registration_payload));
	
		COAP_BLOCKING_REQUEST(&server_ep, request, client_chunk_handler);

		// registration failed, wait before retrying
		if(!registration_status) {
			etimer_set(&sleep_timer, 5 * CLOCK_SECOND);
			PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&sleep_timer));
			LOG_INFO("ATTEMPTING REGISTRATION\n");
		}
	}
    
	// map actuator to coap://ip/name to later send commands
	coap_activate_resource(&res_control_rods, "actuator_control_rods");
	leds_set(LEDS_GREEN);
	
	PROCESS_YIELD();
    PROCESS_END();
}
