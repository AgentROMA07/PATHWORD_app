package models

import "time"

type User struct {
	ID        uint   `gorm:"primaryKey" json:"id"`
	Email     string `gorm:"uniqueIndex" json:"email"`
	Name      string `json:"name"`
	Role      string `json:"role"` // "passenger" or "driver"
	CreatedAt time.Time
	UpdatedAt time.Time
}

type Location struct {
	Lat float64 `json:"lat"`
	Lng float64 `json:"lng"`
}

type OrderRequest struct {
	PassengerID uint     `json:"passenger_id"`
	Destination Location `json:"destination"`
	Price       string   `json:"price"` // e.g. "1000 KZT"
}

type Order struct {
	ID          string   `json:"id"`
	PassengerID uint     `json:"passenger_id"`
	PassengerLoc Location `json:"passenger_loc"` // Where they requested from
	Destination Location `json:"destination"`
	Price       string   `json:"price"`
	Status      string   `json:"status"` // "pending", "accepted", "completed"
}

type Bid struct {
	OrderID  string `json:"order_id"`
	DriverID uint   `json:"driver_id"`
	Price    string `json:"price"`
}

type WsMessage struct {
	Type    string      `json:"type"` // "gps_update", "create_order", "new_order", "bid", "accept_bid"
	Payload interface{} `json:"payload"`
}

type GPSUpdate struct {
	DriverID uint     `json:"driver_id"`
	Location Location `json:"location"`
}
