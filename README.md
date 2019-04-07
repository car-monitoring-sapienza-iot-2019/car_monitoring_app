# Car Monitoring POC - App

The Car Monitoring App is a POC developed as part of a group project of the IoT 2018-2019 course at Sapienza University.
This app is written in Kotlin and requires at least a device with API level 26+.

## Features
The Car Monitoring App comes with a small number of features.
The features that are supported are:
- Setup bluetooth connection for OBD2/ELM3737 connector
- Read data from the car using the bluetooth interface
- Send the car's data to a remote IBM Watson instance
- Support customization of OBD2/ELM3737's behavior


## Dependencies
- [Car Monitoring POC - Edgent Library](https://github.com/car-monitoring-sapienza-iot-2019/car_monitoring_poc_edgent)
- [Dexter](https://github.com/Karumi/Dexter)
- [OBD-Java-API](https://github.com/pires/obd-java-api)
- [MaterialDrawer](https://github.com/mikepenz/MaterialDrawer)
