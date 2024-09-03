#include "contiki.h"
#include "net/routing/routing.h"
#include "mqtt.h"
#include "net/ipv6/uip.h"
#include "net/ipv6/uip-icmp6.h"
#include "net/ipv6/sicslowpan.h"
#include "sys/etimer.h"
#include "sys/ctimer.h"
#include "lib/sensors.h"
#include "dev/button-hal.h"
#include "dev/leds.h"
#include "os/sys/log.h"

#include <string.h>
#include <strings.h>

#include <sys/node-id.h>
#include <time.h>

#define LOG_MODULE "neutron_flux"
#ifdef  MQTT_CLIENT_CONF_LOG_LEVEL
#define LOG_LEVEL MQTT_CLIENT_CONF_LOG_LEVEL
#else
#define LOG_LEVEL LOG_LEVEL_DBG
#endif

/* MQTT broker address. */
#define MQTT_CLIENT_BROKER_IP_ADDR "fd00::1"

static const char *broker_ip = MQTT_CLIENT_BROKER_IP_ADDR;

// Default config values
#define DEFAULT_BROKER_PORT         1883
#define DEFAULT_PUBLISH_INTERVAL    3 * CLOCK_SECOND
#define PUBLISH_INTERVAL	        CLOCK_SECOND
#define RECONNECTION_INTERVAL	    5 * CLOCK_SECOND

// We assume that the broker does not require authentication

/* Various states */
static uint8_t state;

#define STATE_INIT    		    0	// initial state
#define STATE_NET_OK    	    1	// Network is initialized
#define STATE_CONNECTING      	2	// Connecting to MQTT broker
#define STATE_CONNECTED       	3	// Connection successful
#define STATE_SUBSCRIBED      	4	// Topics subscription done
#define STATE_DISCONNECTED    	5	// Disconnected from MQTT broker

/* Maximum TCP segment size for outgoing segments of our socket */
#define MAX_TCP_SEGMENT_SIZE    32
#define CONFIG_IP_ADDR_STR_LEN  64

/*
 * Buffers for Client ID and Topics.
 */
#define BUFFER_SIZE 64

static char client_id[BUFFER_SIZE];
static char pub_topic[BUFFER_SIZE];
static char sub_topic[BUFFER_SIZE];

// Periodic timer to check the state of the MQTT client
#define STATE_MACHINE_PERIODIC     (CLOCK_SECOND >> 1)
static struct etimer periodic_timer;
static struct etimer sleep_timer;

/*
 * The main MQTT buffers.
 * We will need to increase if we start publishing more data.
 */
#define APP_BUFFER_SIZE 512
static char app_buffer[APP_BUFFER_SIZE];

static struct mqtt_message *msg_ptr = 0;

static struct mqtt_connection conn;

#define MIN_FLUX 0
#define MAX_FLUX 45

PROCESS(sensor, "sensor_neutron_flux");
AUTOSTART_PROCESSES(&sensor);

static bool increase_inserted_control_rods = true;
static int neutron_flux = 0;  // "Tera" neutrons/cm^2/s
static int variation = 0;

// handle incoming messages
static void pub_handler(const char *topic, uint16_t topic_len, const uint8_t *chunk, uint16_t chunk_len) {
	LOG_INFO("MESSAGE RECEIVED: topic='%s' (len=%u), chunk_len=%u\n", topic, topic_len, chunk_len);

	if(strcmp(topic, "control_rods") != 0) {
		LOG_ERR("INVALID TOPIC\n");
		return;
	}
	
	LOG_INFO("RECEIVED ACTUATOR COMMAND\n");
	if(strcmp((const char*) chunk, "INC") == 0) {
		LOG_INFO("INCREASING INSERTED CONTROL RODS\n");
		increase_inserted_control_rods = true;
	}
    else if(strcmp((const char*) chunk, "DEC") == 0) {
		LOG_INFO("DECREASING INSERTED CONTROL RODS\n");	
		increase_inserted_control_rods = false;
	}
}

// function called each time a MQTT event occurs
static void mqtt_event(struct mqtt_connection *m, mqtt_event_t event, void *data) {
	switch(event) {
		case MQTT_EVENT_CONNECTED: 
			LOG_INFO("CONNECTED\n");
			state = STATE_CONNECTED;
			break;

		case MQTT_EVENT_DISCONNECTED: 
			printf("MQTT DISCONNECTED. REASON: %u\n", *((mqtt_event_t *)data));
			state = STATE_DISCONNECTED;
			process_poll(&sensor);
			break;

		case MQTT_EVENT_PUBLISH: 
			msg_ptr = data;
			pub_handler(msg_ptr->topic, strlen(msg_ptr->topic), msg_ptr->payload_chunk, msg_ptr->payload_length);
			break;

		case MQTT_EVENT_SUBACK: 
			#if MQTT_311
				mqtt_suback_event_t *suback_event = (mqtt_suback_event_t *)data;
				if(suback_event->success) 
					LOG_INFO("APPLICATION SUBSCRIBED TO TOPIC\n");
				else
					LOG_ERR("APPLICATION FAILED TO SUBSCRIBE TO TOPIC (RET CODE %x)\n", suback_event->return_code);
			#else
				LOG_INFO("APPLICATION SUBSCRIBED TO TOPIC\n");
			#endif
			break;

		case MQTT_EVENT_UNSUBACK: 
			LOG_INFO("APPLICATION UNSUBSCRIBED FROM TOPIC\n");
			break;

		case MQTT_EVENT_PUBACK: 
			LOG_INFO("PUBLISHING COMPLETE\n");
			break;

		default:
			LOG_INFO("APPLICATION GOT A UNHANDLED MQTT EVENT: %i\n", event);
			break;
	}
}

static bool have_connectivity(void) {
	return !(uip_ds6_get_global(ADDR_PREFERRED) == NULL
        || uip_ds6_defrt_choose() == NULL);
}

PROCESS_THREAD(sensor, ev, data) {

	PROCESS_BEGIN();

	static mqtt_status_t status;
	static char broker_address[CONFIG_IP_ADDR_STR_LEN];
    static button_hal_button_t *btn;
	
	// Initialize the ClientID as MAC address
	snprintf(client_id, BUFFER_SIZE, "%02x%02x%02x%02x%02x%02x",
		     linkaddr_node_addr.u8[0], linkaddr_node_addr.u8[1],
		     linkaddr_node_addr.u8[2], linkaddr_node_addr.u8[5],
		     linkaddr_node_addr.u8[6], linkaddr_node_addr.u8[7]);

	// Broker registration					 
	mqtt_register(&conn, &sensor, client_id, mqtt_event, MAX_TCP_SEGMENT_SIZE);
	state = STATE_INIT;
				    
	// Initialize periodic timer to check the status 
	etimer_set(&periodic_timer, PUBLISH_INTERVAL);

	//button initialization
	btn = button_hal_get_by_id(0);
	leds_set(LEDS_RED);
    	
	while(true) {
		PROCESS_YIELD();
		
		if(ev == button_hal_press_event) {
            btn = (button_hal_button_t *)data;
            if(btn->unique_id == 0) {
				neutron_flux -= 3 + rand() % 3;
                LOG_INFO("NEUTRON FLUX DECREASED AT: %d\n", neutron_flux);
            }
        }

		if(neutron_flux == MIN_FLUX) leds_set(LEDS_RED);
		else leds_set(LEDS_BLUE);

        if(!((ev == PROCESS_EVENT_TIMER && data == &periodic_timer) || ev == PROCESS_EVENT_POLL) || (ev == PROCESS_EVENT_TIMER && data == &sleep_timer))
			continue;
                            
        if(state == STATE_INIT && have_connectivity()) state = STATE_NET_OK;
        
        if(state == STATE_NET_OK) {
            LOG_INFO("CONNECTING TO MQTT BROKER: ");
            
            memcpy(broker_address, broker_ip, strlen(broker_ip));
            
            mqtt_connect(&conn, broker_address, DEFAULT_BROKER_PORT,
                        RECONNECTION_INTERVAL,
                        MQTT_CLEAN_SESSION_ON);

            state = STATE_CONNECTING;
        }
        
        if(state == STATE_CONNECTED) {
            strcpy(sub_topic, "control_rods");
            status = mqtt_subscribe(&conn, NULL, sub_topic, MQTT_QOS_LEVEL_0);

            LOG_INFO("SUBSCRIBING TO control_rods TOPIC\n");
            if(status == MQTT_STATUS_OUT_QUEUE_FULL) {
                LOG_ERR("SUBSCRIBE FAILED: QUEUE FULL\n");
                PROCESS_EXIT();
            }
            
            state = STATE_SUBSCRIBED;
        }
            
        if(state == STATE_SUBSCRIBED) {
            if(increase_inserted_control_rods) {
            	variation = neutron_flux * (rand() % 4)/100.0;
            	if(rand() % 4 == 1) neutron_flux += variation;
            	else neutron_flux -= variation;
            }
            else {
            	if(neutron_flux == MIN_FLUX) neutron_flux = 10;	// jump start
            	else neutron_flux += neutron_flux * (rand() % 3)/100.0;
            }

			if(neutron_flux < MIN_FLUX) neutron_flux = MIN_FLUX;
			else if(neutron_flux > MAX_FLUX) neutron_flux = MAX_FLUX;

			LOG_INFO("NEW NEUTRON FLUX: %d\n", neutron_flux);
			
			sprintf(pub_topic, "%s", "neutron_flux");
			sprintf(app_buffer, "{\"value\": %d}", neutron_flux);
			mqtt_publish(&conn, NULL, pub_topic, (uint8_t *)app_buffer, strlen(app_buffer),
                MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);
        }

        else if(state == STATE_DISCONNECTED ) {
            LOG_ERR("DISCONNECTED FROM MQTT BROKER, ATTEMPTING RECONNECTION: ");
            state = STATE_INIT;
            etimer_set(&sleep_timer, RECONNECTION_INTERVAL);
            continue;
        }
        
        etimer_set(&periodic_timer, STATE_MACHINE_PERIODIC);
    }

	PROCESS_END();
}
