package dev.samuelab.immigrationservice.services;

import dev.samuelab.immigrationservice.model.Appointment;
import dev.samuelab.immigrationservice.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import dev.samuelab.immigrationservice.helper.CSVHelper;

import java.io.IOException;
import java.util.List;

@Service
public class CSVService {
    @Autowired
    AppointmentRepository repository;

    public void save(MultipartFile file) {
        try {
            List<Appointment> appointments = CSVHelper.csvToAppointments(file.getInputStream());
            repository.saveAll(appointments);
        } catch (IOException e) {
            throw new RuntimeException("fail to store csv data: " + e.getMessage());
        }
    }

    public List<Appointment> getAllTutorials() {
        return repository.findAll();
    }
}