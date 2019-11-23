package com.mokujin.user.model;

import com.mokujin.user.model.record.HealthRecord;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class User {

    private Role role;

    private String firstName;

    private String lastName;

    private String fatherName;

    private String nationalNumber;

    private String photo;

    private List<Credential> nationalCredentials = new ArrayList<>();

    private List<Contact> contacts = new ArrayList<>();

    private List<Credential> credentials = new ArrayList<>();

    private List<HealthRecord> records = new ArrayList<>();

    public enum Role {
        PATIENT,
        DOCTOR
    }

}
