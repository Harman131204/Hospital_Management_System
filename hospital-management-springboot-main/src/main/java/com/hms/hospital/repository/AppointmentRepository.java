package com.hms.hospital.repository;

import com.hms.hospital.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    long countByPatientIdAndStartTimeAfter(Long patientId, LocalDateTime now);
    long countByPatientIdAndStartTimeBefore(Long patientId, LocalDateTime now);

    List<Appointment> findByDoctorId(Long doctorId);

    List<Appointment> findByPatientId(Long patientId);
    List<Appointment> findAllByOrderByStartTimeDesc();
    List<Appointment> findTop10ByOrderByStartTimeDesc();

    List<Appointment> findByDoctorIdAndStartTimeAfter(Long doctorId, LocalDateTime now);

    List<Appointment> findByPatientIdAndStartTimeAfter(Long patientId, LocalDateTime now);

    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND DATE(a.startTime) = :today")
    List<Appointment> findByDoctorIdAndStartDate(@Param("doctorId") Long doctorId,
                                                 @Param("today") LocalDate today);

    List<Appointment> findByDoctorIdOrderByStartTimeAsc(Long doctorId);
    List<Appointment> findByPatientIdOrderByStartTimeAsc(Long patientId);
	List<Appointment> findByPatientUserIdOrderByStartTimeAsc(Long userId);
}