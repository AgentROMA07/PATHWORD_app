package main

import (
	"fmt"
	"log"
	"math/rand"
	"sync"
	"time"

	"github.com/gorilla/websocket"
)

func main() {
	var wg sync.WaitGroup
	driverCount := 1000
	passengerCount := 200

	log.Println("Starting load test...")

	for i := 1; i <= driverCount; i++ {
		wg.Add(1)
		go runDriver(i, &wg)
	}

	for i := 1; i <= passengerCount; i++ {
		wg.Add(1)
		go runPassenger(i+driverCount, &wg)
	}

	wg.Wait()
}

func runDriver(id int, wg *sync.WaitGroup) {
	defer wg.Done()

	url := fmt.Sprintf("ws://localhost:8080/ws?user_id=%d", id)
	var conn *websocket.Conn
	for i := 0; i < 5; i++ {
		c, _, err := websocket.DefaultDialer.Dial(url, nil)
		if err == nil {
			conn = c
			break
		}
		time.Sleep(1 * time.Second)
	}

	if conn == nil {
		log.Printf("Driver %d failed to connect", id)
		return
	}
	defer conn.Close()

	go func() {
		for {
			_, _, err := conn.ReadMessage()
			if err != nil {
				return
			}
		}
	}()

	ticker := time.NewTicker(2 * time.Second)
	defer ticker.Stop()

	for {
		<-ticker.C
		msg := fmt.Sprintf(`{"type":"gps_update","payload":{"driver_id":%d,"location":{"lat":%f,"lng":%f}}}`, id, 43.23+rand.Float64()*0.1, 76.88+rand.Float64()*0.1)
		err := conn.WriteMessage(websocket.TextMessage, []byte(msg))
		if err != nil {
			log.Printf("Driver %d error writing: %v", id, err)
			return
		}
	}
}

func runPassenger(id int, wg *sync.WaitGroup) {
	defer wg.Done()

	url := fmt.Sprintf("ws://localhost:8080/ws?user_id=%d", id)
	var conn *websocket.Conn
	for i := 0; i < 5; i++ {
		c, _, err := websocket.DefaultDialer.Dial(url, nil)
		if err == nil {
			conn = c
			break
		}
		time.Sleep(1 * time.Second)
	}

	if conn == nil {
		log.Printf("Passenger %d failed to connect", id)
		return
	}
	defer conn.Close()

	go func() {
		for {
			_, _, err := conn.ReadMessage()
			if err != nil {
				return
			}
		}
	}()

	ticker := time.NewTicker(5 * time.Second)
	defer ticker.Stop()

	for {
		<-ticker.C
		msg := fmt.Sprintf(`{"type":"NEW_ORDER","payload":{"passenger_id":%d,"destination":{"lat":%f,"lng":%f},"price":"1000 KZT"}}`, id, 43.23+rand.Float64()*0.1, 76.88+rand.Float64()*0.1)
		err := conn.WriteMessage(websocket.TextMessage, []byte(msg))
		if err != nil {
			log.Printf("Passenger %d error writing: %v", id, err)
			return
		}
	}
}
