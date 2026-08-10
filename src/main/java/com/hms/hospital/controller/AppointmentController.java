package com.hms.hospital.controller;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hms.hospital.entity.Appointment;
import com.hms.hospital.entity.Patient;
import com.hms.hospital.entity.Role;
import com.hms.hospital.entity.User;
import com.hms.hospital.repository.AppointmentRepository;
import com.hms.hospital.repository.UserRepository;
import com.hms.hospital.service.PatientService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/appointment")
public class AppointmentController {

    private final AppointmentRepository appointmentRepo;
    private final PatientService patientService;
    private final UserRepository userRepo;

    @GetMapping("/calendar")
    public String calendar(Model model, Principal principal) {
        model.addAttribute("userEmail", principal.getName());
        return "appointment/calendar";
    }

    @GetMapping("/book")
    public String bookForm(Model model) {
        model.addAttribute("doctors", userRepo.findAll().stream()
                .filter(u -> u.getRole() == Role.DOCTOR)
                .toList());
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

        Patient patient = patientService.findPatientByEmail(principal.getName())
                .orElse(null);

        if (patient == null) {
            ra.addFlashAttribute("error", "Only registered patients can book appointments.");
            return "redirect:/appointment/book";
        }

        User doctor = userRepo.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        LocalDateTime start = LocalDateTime.parse(date + "T" + time);

        appointment.setStartTime(start);           // ← NOW WORKS!
        appointment.setEndTime(start.plusMinutes(30)); // ← NOW WORKS!
        appointment.setTitle("Checkup - " + patient.getName());
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setStatus("SCHEDULED");

        appointmentRepo.save(appointment);

        ra.addFlashAttribute("success", "Appointment booked successfully!");
        return "redirect:/appointment/calendar?reload=true";
    }

    @GetMapping("/doctor/events")
    @ResponseBody
    public List<Appointment> doctorEvents(Principal principal) {
        User doctor = userRepo.findByEmail(principal.getName()).orElseThrow();
        return appointmentRepo.findByDoctorIdOrderByStartTimeAsc(doctor.getId());
    }

    @GetMapping("/events")
    @ResponseBody
    public List<?> getEvents(Principal principal) {

        User user = userRepo.findByEmail(principal.getName())
                .orElseThrow();

        Long userId = user.getId();

        List<Appointment> list;

        if (user.getRole().name().equals("PATIENT")) {
            list = appointmentRepo.findByPatientUserIdOrderByStartTimeAsc(userId);
        } 
        else if (user.getRole().name().equals("DOCTOR")) {
            list = appointmentRepo.findByDoctorIdOrderByStartTimeAsc(userId);
        } 
        else {
            list = appointmentRepo.findAll();
        }

        return list.stream().map(a -> Map.of(
                "title", a.getTitle(),
                "start", a.getStartTime().toString(),
                "end", a.getEndTime().toString(),
                "status", a.getStatus(),
                "patientName", a.getPatient().getName()
        )).toList();
    }
}