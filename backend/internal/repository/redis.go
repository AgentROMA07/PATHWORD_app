package repository

import (
	"context"
	"fmt"
	"pathword/backend/internal/models"

	"github.com/redis/go-redis/v9"
)

type RedisRepo struct {
	client *redis.Client
}

func NewRedisRepo(client *redis.Client) *RedisRepo {
	return &RedisRepo{client: client}
}

func (r *RedisRepo) UpdateDriverLocation(ctx context.Context, driverID uint, loc models.Location) error {
	return r.client.GeoAdd(ctx, "drivers_locations", &redis.GeoLocation{
		Name:      fmt.Sprintf("%d", driverID),
		Longitude: loc.Lng,
		Latitude:  loc.Lat,
	}).Err()
}

func (r *RedisRepo) FindNearbyDrivers(ctx context.Context, loc models.Location, radiusKm float64) ([]string, error) {
	res, err := r.client.GeoRadius(ctx, "drivers_locations", loc.Lng, loc.Lat, &redis.GeoRadiusQuery{
		Radius: radiusKm,
		Unit:   "km",
	}).Result()

	if err != nil {
		return nil, err
	}

	var drivers []string
	for _, l := range res {
		drivers = append(drivers, l.Name)
	}
	return drivers, nil
}
