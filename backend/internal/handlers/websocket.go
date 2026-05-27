package handlers

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"sync"
	"time"
	"pathword/backend/internal/models"
	"pathword/backend/internal/repository"

	"github.com/gorilla/websocket"
)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		return true // Allow all origins for dev
	},
}

type Client struct {
	ID   uint
	Conn *websocket.Conn
	Send chan []byte
}

type WsManager struct {
	clients    map[uint]*Client
	mutex      sync.RWMutex
	redisRepo  *repository.RedisRepo
}

func NewWsManager(redisRepo *repository.RedisRepo) *WsManager {
	return &WsManager{
		clients:   make(map[uint]*Client),
		redisRepo: redisRepo,
	}
}

func (m *WsManager) HandleConnections(w http.ResponseWriter, r *http.Request) {
	ws, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Fatal(err)
	}

	var userID uint
	fmt.Sscanf(r.URL.Query().Get("user_id"), "%d", &userID)
	if userID == 0 {
		userID = uint(len(m.clients) + 1)
	}

	client := &Client{
		ID:   userID,
		Conn: ws,
		Send: make(chan []byte, 256),
	}

	m.mutex.Lock()
	m.clients[userID] = client
	m.mutex.Unlock()

	log.Printf("User %d connected", userID)

	go m.writePump(client)
	m.readPump(client)
}

func (m *WsManager) readPump(client *Client) {
	defer func() {
		m.mutex.Lock()
		delete(m.clients, client.ID)
		m.mutex.Unlock()
		client.Conn.Close()
		close(client.Send)
	}()

	for {
		var msg models.WsMessage
		err := client.Conn.ReadJSON(&msg)
		if err != nil {
			log.Printf("error reading from client %d: %v", client.ID, err)
			break
		}

		m.handleMessage(client.ID, msg)
	}
}

func (m *WsManager) writePump(client *Client) {
	defer func() {
		client.Conn.Close()
	}()
	for {
		message, ok := <-client.Send
		if !ok {
			client.Conn.WriteMessage(websocket.CloseMessage, []byte{})
			return
		}

		w, err := client.Conn.NextWriter(websocket.TextMessage)
		if err != nil {
			return
		}
		w.Write(message)

		if err := w.Close(); err != nil {
			return
		}
	}
}

func (m *WsManager) handleMessage(senderID uint, msg models.WsMessage) {
	switch msg.Type {
	case "gps_update":
		payloadBytes, _ := json.Marshal(msg.Payload)
		var update models.GPSUpdate
		json.Unmarshal(payloadBytes, &update)

		update.DriverID = senderID
		err := m.redisRepo.UpdateDriverLocation(context.Background(), senderID, update.Location)
		if err != nil {
			log.Printf("Failed to update GPS for driver %d: %v", senderID, err)
		}

	case "create_order":
		payloadBytes, _ := json.Marshal(msg.Payload)
		var req models.OrderRequest
		json.Unmarshal(payloadBytes, &req)

		order := models.Order{
			ID:          fmt.Sprintf("order_%d", time.Now().Unix()),
			PassengerID: senderID,
			Destination: req.Destination,
			Price:       req.Price,
			Status:      "pending",
		}

		driversStr, err := m.redisRepo.FindNearbyDrivers(context.Background(), req.Destination, 5.0)

		wsMsg := models.WsMessage{
			Type:    "new_order",
			Payload: order,
		}

		if err == nil && len(driversStr) > 0 {
			for _, driverIDStr := range driversStr {
				var driverID uint
				fmt.Sscanf(driverIDStr, "%d", &driverID)
				m.sendToUser(driverID, wsMsg)
			}
		} else {
			m.broadcastToAll(wsMsg)
		}

	case "bid":
		payloadBytes, _ := json.Marshal(msg.Payload)
		var bid models.Bid
		json.Unmarshal(payloadBytes, &bid)
		bid.DriverID = senderID

		m.broadcastToAll(models.WsMessage{
			Type: "bid",
			Payload: bid,
		})

	case "accept_bid":
		payloadBytes, _ := json.Marshal(msg.Payload)
		var bid models.Bid
		json.Unmarshal(payloadBytes, &bid)

		m.sendToUser(bid.DriverID, models.WsMessage{
			Type: "ride_started",
			Payload: bid,
		})

		m.sendToUser(senderID, models.WsMessage{
			Type: "ride_started",
			Payload: bid,
		})
	}
}

func (m *WsManager) broadcastToAll(msg models.WsMessage) {
	bytes, err := json.Marshal(msg)
	if err != nil {
		return
	}
	m.mutex.RLock()
	defer m.mutex.RUnlock()
	for _, client := range m.clients {
		select {
		case client.Send <- bytes:
		default:
			log.Printf("Client %d queue is full, skipping message", client.ID)
		}
	}
}

func (m *WsManager) sendToUser(userID uint, msg models.WsMessage) {
	bytes, err := json.Marshal(msg)
	if err != nil {
		return
	}
	m.mutex.RLock()
	defer m.mutex.RUnlock()
	if client, ok := m.clients[userID]; ok {
		select {
		case client.Send <- bytes:
		default:
			log.Printf("Client %d queue is full, skipping message", client.ID)
		}
	}
}
