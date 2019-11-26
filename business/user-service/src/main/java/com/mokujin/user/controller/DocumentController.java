package com.mokujin.user.controller;

import com.mokujin.user.model.User;
import com.mokujin.user.model.document.Document;
import com.mokujin.user.model.internal.DocumentDraft;
import com.mokujin.user.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.springframework.http.HttpStatus.OK;

@Slf4j
@RestController
@RequestMapping("/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/offer/dicom/{patientNumber}")
    public ResponseEntity<User> offerDicom(@PathVariable String patientNumber,
                                           @RequestParam("file") MultipartFile file,
                                           @RequestHeader("Public-Key") String publicKey,
                                           @RequestHeader("Private-Key") String privateKey) {
        log.info("'offerDicom' invoked with params '{}, {}, {}, {}'", patientNumber, file, publicKey, privateKey);

        User user = documentService.offerDicom(publicKey, privateKey, file, patientNumber);

        log.info("'offerDicom' returned '{}'", user);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/offer/{patientNumber}")
    public ResponseEntity<User> offer(@PathVariable String patientNumber,
                                      @RequestBody DocumentDraft documentDraft,
                                      @RequestHeader("Public-Key") String publicKey,
                                      @RequestHeader("Private-Key") String privateKey) {
        log.info("'send' invoked with params '{}, {}, {}, {}'", patientNumber, documentDraft, publicKey, privateKey);

        User user = documentService.offerCredential(publicKey, privateKey, documentDraft, patientNumber);

        log.info("'offer' returned '{}'", user);
        return ResponseEntity.ok(user);
    }


    @PostMapping("/accept")
    public ResponseEntity<User> accept(@RequestParam String patientNumber,
                                       @RequestParam String doctorNumber,
                                       @RequestBody Document document,
                                       @RequestHeader("Public-Key") String publicKey,
                                       @RequestHeader("Private-Key") String privateKey) {
        log.info("'accept' invoked with params '{}, {}, {}, {}, {}'", publicKey, privateKey, document, patientNumber, doctorNumber);

        User user = documentService.accept(publicKey, privateKey, document, patientNumber, doctorNumber);

        log.info("'accept' returned '{}'", user);
        return ResponseEntity.ok(user);
    }


    @PostMapping("/decline")
    public ResponseEntity decline(@RequestParam String patientNumber,
                                  @RequestParam String doctorNumber) {
        log.info("'decline' invoked with params '{}, {}'", patientNumber, doctorNumber);

        documentService.decline(patientNumber, doctorNumber);

        log.info("'decline' has executed successfully.");
        return new ResponseEntity(OK);
    }

    @PostMapping("/ask/{patientNumber}")
    public ResponseEntity<User> ask(@PathVariable String patientNumber,
                                    @RequestBody List<String> keywords,
                                    @RequestHeader("Public-Key") String publicKey,
                                    @RequestHeader("Private-Key") String privateKey) {
        log.info("'ask' invoked with params '{}, {}, {}, {}'", publicKey, privateKey, patientNumber, keywords);

        User user = documentService.askDocument(publicKey, privateKey, keywords, patientNumber);

        log.info("'ask' returned '{}'", user);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/present/{doctorNumber}")
    public ResponseEntity<User> present(@PathVariable String doctorNumber,
                                        @RequestBody Document document,
                                        @RequestHeader("Public-Key") String publicKey,
                                        @RequestHeader("Private-Key") String privateKey) {
        log.info("'present' invoked with params '{}, {}, {}, {}'", publicKey, privateKey, doctorNumber, document);

        User user = documentService.presentDocument(publicKey, privateKey, document, doctorNumber);

        log.info("'ask' returned '{}'", user);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/delete/notification")
    public ResponseEntity deleteNotification(@RequestParam String doctorNumber,
                                             @RequestParam String patientNumber) {
        log.info("'deleteNotification' invoked with params '{}, {}'", patientNumber, doctorNumber);

        documentService.deleteNotification(doctorNumber, patientNumber);

        log.info("'deleteNotification' has executed successfully.");
        return new ResponseEntity(OK);
    }
}
