package com.logistics.repositories;

import com.logistics.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {

    // Найти транспорт по статусу
    List<Vehicle> findByStatus(String status);

    // Найти транспорт по номерному знаку
    Optional<Vehicle> findByLicensePlate(String licensePlate);

    // Найти транспорт с грузоподъемностью больше указанной
    List<Vehicle> findByCapacityTonGreaterThanEqual(Double minCapacity);

    // Найти транспорт по модели
    List<Vehicle> findByModelContainingIgnoreCase(String model);

    // Кастомный запрос: найти свободный транспорт с достаточной грузоподъемностью
    @Query("SELECT v FROM Vehicle v WHERE v.status = 'свободен' AND v.capacityTon >= :requiredCapacity ORDER BY v.capacityTon DESC")
    List<Vehicle> findAvailableVehiclesWithCapacity(@Param("requiredCapacity") Double requiredCapacity);

    // Кастомный запрос: подсчитать количество транспорта по статусам
    @Query("SELECT v.status, COUNT(v) FROM Vehicle v GROUP BY v.status")
    List<Object[]> countVehiclesByStatus();

    // Кастомный запрос: найти транспорт, который используется в заказах с определенным статусом
    @Query("SELECT DISTINCT v FROM Vehicle v JOIN v.orders o WHERE o.status = :orderStatus")
    List<Vehicle> findVehiclesByOrderStatus(@Param("orderStatus") String orderStatus);

    // Кастомный запрос: найти транспорт по диапазону грузоподъемности
    @Query("SELECT v FROM Vehicle v WHERE v.capacityTon BETWEEN :minCapacity AND :maxCapacity")
    List<Vehicle> findByCapacityTonBetween(@Param("minCapacity") Double minCapacity, @Param("maxCapacity") Double maxCapacity);

    // Проверить существование транспортного средства по номерному знаку
    boolean existsByLicensePlate(String licensePlate);

    // Найти все транспортные средства с сортировкой по грузоподъемности
    List<Vehicle> findAllByOrderByCapacityTonDesc();
}