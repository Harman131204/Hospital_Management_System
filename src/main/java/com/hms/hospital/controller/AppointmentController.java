package com.hms.hospital.controller;

import com.hms.hospital.entity.Appointment;
import com.hms.hospital.entity.Patient;
import com.hms.hospital.entity.Role;
import com.hms.hospital.entity.User;
import com.hms.hospital.repository.AppointmentRepository;
import com.hms.hospital.repository.UserRepository;
import com.hms.hospital.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/appointment")
public class AppointmentController {

    private final AppointmentRepository appointmentRepo;
    private final PatientService patientService;
    private final UserRepository userRepo;

    @GetMapping("/calendar")
    public String calendar(Model model, Principal principal) {

        User user = userRepo.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("role", user.getRole());

        String dashboardUrl;

        if (user.getRole() == Role.DOCTOR) {
            dashboardUrl = "/doctor/dashboard";
        } else if (user.getRole() == Role.ADMIN) {
            dashboardUrl = "/admin/dashboard";
        } else {
            dashboardUrl = "/patient/dashboard";
        }

        model.addAttribute("dashboardUrl", dashboardUrl);

        return "appointment/calendar";
    }

    @GetMapping("/book")
    public String bookForm(Model model) {

        List<User> doctors = userRepo.findAll()
                .stream()
                .filter(user -> user.getRole() == Role.DOCTOR)
                .toList();

        model.addAttribute("doctors", doctors);
        model.addAttribute("appointment", new Appointment());

        return "appointment/book";
    }

    @PostMapping("/book")
    public String book(@ModelAttribute Appointment appointment,
                       @RequestParam Long doctorId,
                       @RequestParam String date,
                       @RequestParam String time,
                       Principal principal,
                       RedirectAttributes ra) {

        Patient patient = patientService
                .findPatientByEmail(principal.getName())
                .orElse(null);

        if (patient == null) {
            ra.addFlashAttribute(
                    "error",
                    "Only registered patients can book appointments."
            );
            return "redirect:/appointment/book";
        }

        User doctor = userRepo.findById(doctorId)
                .orElseThrow(() ->
                        new RuntimeException("Doctor not found"));

        LocalDateTime start = LocalDateTime.parse(date + "T" + time);

        appointment.setStartTime(start);
        appointment.setEndTime(start.plusMinutes(30));
        appointment.setTitle("Appointment - " + patient.getName());
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setStatus("SCHEDULED");

        appointmentRepo.save(appointment);

        ra.addFlashAttribute(
                "success",
                "Appointment booked successfully!"
        );

        return "redirect:/appointment/calendar";
    }

    @GetMapping("/doctor/events")
    @ResponseBody
    public List<Map<String, Object>> doctorEvents(Principal principal) {

        User doctor = userRepo
                .findByEmail(principal.getName())
                .orElseThrow(() ->
                        new RuntimeException("Doctor not found"));

        return appointmentRepo
                .findByDoctorIdOrderByStartTimeAsc(doctor.getId())
                .stream()
                .map(a -> {
                    Map<String, Object> event = new HashMap<>();

                    event.put("title",
                            a.getPatient() != null
                                    ? a.getPatient().getName()
                                    : "Patient");

                    event.put("start", a.getStartTime());
                    event.put("end", a.getEndTime());
                    event.put("status", a.getStatus());

                    return event;
                })
                .toList();
    }

    @GetMapping("/events")
    @ResponseBody
    public List<Map<String, Object>> getEvents(Principal principal) {

        User user = userRepo
                .findByEmail(principal.getName())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        List<Appointment> appointments;

        if (user.getRole() == Role.PATIENT) {

            appointments = appointmentRepo
                    .findByPatientUserIdOrderByStartTimeAsc(
                            user.getId()
                    );

        } else if (user.getRole() == Role.DOCTOR) {

            appointments = appointmentRepo
                    .findByDoctorIdOrderByStartTimeAsc(
                            user.getId()
                    );

        } else {

            appointments = appointmentRepo.findAll();
        }

        return appointments.stream()
                .map(a -> {
                    Map<String, Object> event = new HashMap<>();

                    event.put("title", a.getTitle());
                    event.put("start", a.getStartTime());
                    event.put("end", a.getEndTime());
                    event.put("status", a.getStatus());

                    return event;
                })
                .toList();
    }
}