package models

import (
	"encoding/json"
	"testing"
)

func TestWsMessageSerialization(t *testing.T) {
	loc := Location{Lat: 43.0, Lng: 76.0}
	gps := GPSUpdate{DriverID: 1, Location: loc}

	msg := WsMessage{
		Type: "gps_update",
		Payload: gps,
	}

	bytes, err := json.Marshal(msg)
	if err != nil {
		t.Fatalf("Failed to marshal WsMessage: %v", err)
	}

	var decoded WsMessage
	err = json.Unmarshal(bytes, &decoded)
	if err != nil {
		t.Fatalf("Failed to unmarshal WsMessage: %v", err)
	}

	if decoded.Type != "gps_update" {
		t.Errorf("Expected type 'gps_update', got '%s'", decoded.Type)
	}
}
