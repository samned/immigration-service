package dev.samuelab.immigrationservice.repository;

import dev.samuelab.immigrationservice.model.Appointment;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends ListCrudRepository<Appointment,Integer> {
    List<Appointment> findAllByAppointmentIdEquals(String appointmentId);
    List<Appointment> findAppointmentByFirstNameContainingIgnoreCaseOrMiddleNameContainingOrLastNameContaining(String firstName, String middleName, String lastName);
}
