# Final Project: BLE-for-ecommerce
### Temas Selectos de Ingeniería en Computación III: Internet de las Cosas

#### About this project: 
BLE-for-ecommerce is an Android App that interacts with ESP32 Boards through Bluetooth Low Energy using Client-Server Architecture.
The ESP32 boards defines a service with two characteristics with the properties of Write and Read, the app scans BLE devices filtering by service UUID, once the devices with said UUID are detected, the characteristics are read, which contain a url of a website with the offer of a product and an offer code to enter on the website
