package com.hms.hospital;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println("vikram@123  = " + encoder.encode("vikram@123"));
        System.out.println("aarav@123   = " + encoder.encode("aarav@123"));
        System.out.println("sneha@123   = " + encoder.encode("sneha@123"));
        System.out.println("pooja@123   = " + encoder.encode("pooja@123"));
    }
}