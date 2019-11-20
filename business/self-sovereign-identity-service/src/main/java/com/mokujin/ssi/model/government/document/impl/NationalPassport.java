package com.mokujin.ssi.model.government.document.impl;

import com.mokujin.ssi.model.government.document.NationalDocument;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NationalPassport extends NationalDocument {

    private String number;

    private String firstName;

    private String lastName;

    private String fatherName;

    private Long dateOfBirth;

    private String placeOfBirth;

    private String image;

    private String sex;

    private String issuer;

    private Long dateOfIssue;

    public NationalPassport(String number, String firstName, String lastName, String fatherName,
                            Long dateOfBirth, String placeOfBirth, String image, String sex, String issuer, Long dateOfIssue) {
        super(Type.passport.name());
        this.number = number;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fatherName = fatherName;
        this.dateOfBirth = dateOfBirth;
        this.placeOfBirth = placeOfBirth;
        this.image = image;
        this.sex = sex;
        this.issuer = issuer;
        this.dateOfIssue = dateOfIssue;
    }


    public NationalPassport() {
        super(Type.passport.name());
    }

}
