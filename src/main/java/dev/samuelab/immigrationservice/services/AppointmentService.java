package dev.samuelab.immigrationservice.services;

import dev.samuelab.immigrationservice.dto.ResponseMessage;
import dev.samuelab.immigrationservice.helper.CSVHelper;
import dev.samuelab.immigrationservice.model.Appointment;
import dev.samuelab.immigrationservice.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.ResponseEntity.*;

@Service
public class AppointmentService {
    @Autowired
    AppointmentRepository appointmentRepository;
    @Autowired
    CSVService fileService;
    public List<Appointment> getAppointments(){
        return appointmentRepository.findAll();
    }
    public ResponseEntity<Appointment> findAppointmentById(Integer appointmentId){
        Optional<Appointment> appointmentOptional = appointmentRepository.findById(appointmentId);
        return appointmentOptional.map(ResponseEntity::ok).orElseGet(() -> notFound().build());
    }
    public ResponseEntity<List<Appointment>> searchByAppointment(String firstName,String middleName,String lastName){
        List<Appointment> appointmentOptional = appointmentRepository.findAppointmentByFirstNameContainingIgnoreCaseOrMiddleNameContainingOrLastNameContaining(firstName,middleName,lastName);
        if(appointmentOptional.isEmpty()){
            return notFound().build();
        }
        return ok(appointmentOptional);
    }
    public ResponseEntity<Void> createAppointment(Appointment newAppointmentRequest, UriComponentsBuilder ucb){
        Appointment appointment = appointmentRepository.save(newAppointmentRequest);
        URI locationOfNewAppointment = ucb
                .path("api/v1/{id}")
                .buildAndExpand(appointment.getId())
                .toUri();
        return created(locationOfNewAppointment).build();
    }

    public ResponseEntity<ResponseMessage> uploadFile(@RequestParam("file") MultipartFile file) {
        String message = "";

        if (CSVHelper.hasCSVFormat(file)) {
            try {
                fileService.save(file);

                message = "Uploaded the file successfully: " + file.getOriginalFilename();
                return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
            } catch (Exception e) {
                message = "Could not upload the file: " + file.getOriginalFilename() + "!";
                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
            }
        }

        message = "Please upload a csv file!";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseMessage(message));
    }


    public ResponseEntity<ResponseMessage> uploadPDF(@RequestParam("file") MultipartFile file) {
        String message = "";

        if (CSVHelper.hasCSVFormat(file)) {
            try {
                fileService.save(file);

                message = "Uploaded the file successfully: " + file.getOriginalFilename();
                return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
            } catch (Exception e) {
                message = "Could not upload the file: " + file.getOriginalFilename() + "!";
                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
            }
        }

        message = "Please upload a csv file!";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseMessage(message));
    }
}
