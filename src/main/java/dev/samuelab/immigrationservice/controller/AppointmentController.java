package dev.samuelab.immigrationservice.controller;

import dev.samuelab.immigrationservice.dto.ResponseMessage;
import dev.samuelab.immigrationservice.model.Appointment;
import dev.samuelab.immigrationservice.services.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import static org.springframework.http.ResponseEntity.*;

@RestController
@RequestMapping("/api/v1")
public class AppointmentController {

    @Autowired
    AppointmentService appointmentService;


    @GetMapping("")
    private ResponseEntity<Iterable<Appointment>> getAllAppointments(){
        return ok(appointmentService.getAppointments());
    }

    @GetMapping("/{appointmentId}")
    private ResponseEntity<Appointment> findById(@PathVariable Integer appointmentId) {
        return appointmentService.findAppointmentById(appointmentId);
    }
    @GetMapping("/search")
    private ResponseEntity<List<Appointment>> findById(@RequestParam("firstName") String firstName,@RequestParam("firstName") String middleName,@RequestParam("firstName") String lastName) {
        return appointmentService.searchByAppointment(firstName,middleName,lastName);
    }

    @PostMapping
    private ResponseEntity<Void> createAppointment(@RequestBody Appointment newAppointmentRequest, UriComponentsBuilder ucb) {
       return appointmentService.createAppointment(newAppointmentRequest,ucb);
    }


    @PostMapping("/upload")
    private ResponseEntity<ResponseMessage> uploadCSV(@RequestParam("file") MultipartFile file) {
       return appointmentService.uploadFile(file);
    }
//    @PostMapping("/upload")
//    private ResponseEntity<ResponseMessage> uploadPDF(@RequestParam("file") MultipartFile file) {
//        return appointmentService.uploadFile(file);
//    }

}
