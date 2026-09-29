package com.hms.hospital.controller;

import com.hms.hospital.entity.Appointment;
import com.hms.hospital.entity.Patient;
import com.hms.hospital.entity.Role;
import com.hms.hospital.entity.User;
import com.hms.hospital.repository.AppointmentRepository;
import com.hms.hospital.repository.PatientRepository;
import com.hms.hospital.repository.UserRepository;
import com.hms.hospital.service.PatientService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final PatientRepository patientRepo;
    private final UserRepository userRepo;
    private final AppointmentRepository appointmentRepo;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("patient", new Patient());
        return "register";
    }

    @PostMapping("/register")
    public String registerPatient(
            @Valid @ModelAttribute("patient") Patient patient,
            BindingResult result,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes ra) {

        if (result.hasErrors()) {
            return "register";
        }

        if (userRepo.findByEmail(patient.getEmail()).isPresent()) {
            ra.addFlashAttribute("error", "Email already registered!");
            return "redirect:/register";
        }

        if (!password.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/register";
        }

        User user = new User();
        user.setName(patient.getName());
        user.setEmail(patient.getEmail());
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.PATIENT);

        userRepo.save(user);

        patient.setPatientId("PAT-" + String.format("%04d", patientRepo.count() + 1));
        patient.setUser(user);

        patientRepo.save(patient);

        ra.addFlashAttribute("msg", "Registration successful! Please login.");

        return "redirect:/login";
    }

    @GetMapping("/patient/dashboard")
    public String patientDashboard(
            Principal principal,
            Model model) {

        Patient patient =
                patientService.getPatientByEmail(principal.getName());

        model.addAttribute("patient", patient);

        List<Appointment> upcomingAppointments =
                appointmentRepo.findByPatientIdOrderByStartTimeAsc(patient.getId())
                        .stream()
                        .filter(a -> a.getStartTime().isAfter(LocalDateTime.now()))
                        .toList();

        model.addAttribute("upcomingAppointments", upcomingAppointments);

        return "patient/dashboard";
    }
}