pushd .

# BORDER ROUTER
cd ~/contiki-ng/examples/rpl-border-router
make -j4 TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM0 border-router.dfu-upload


# SENSORS
cd ~/radaway/mqtt

# temperature
make -j4 TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM1 temperature.dfu-upload

# pressure
make -j4 TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM1 pressure.dfu-upload

# neutron flux
make -j4 TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM1 neutron_flux.dfu-upload


# ACTUATORS
cd ~/radaway/coap

# control rods
make -j4 TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM2 actuator_controlrods.dfu-upload

# coolant flow
make -j4 TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM2 actuator_coolantflow.dfu-upload

popd
