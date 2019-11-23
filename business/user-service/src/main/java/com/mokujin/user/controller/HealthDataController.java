package com.mokujin.user.controller;

import com.mokujin.user.model.User;
import com.mokujin.user.model.record.HealthRecord;
import com.mokujin.user.service.HealthDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthDataController {

    private final HealthDataService healthDataService;

    @PostMapping("/save")
    public ResponseEntity<List<HealthRecord>> save(@RequestBody HealthRecord record,
                                                   @RequestHeader("Public-Key") String publicKey,
                                                   @RequestHeader("Private-Key") String privateKey) {
        log.info("'save' invoked with params '{}, {}, {}'", record, publicKey, privateKey);

        List<HealthRecord> records = healthDataService.save(publicKey, privateKey, record);

        log.info("'save' returned '{}'", records);
        return ResponseEntity.ok(records);
    }

    @PostMapping("/send/{connectionNumber}")
    public ResponseEntity<User> send(@PathVariable String connectionNumber,
                                     @RequestBody HealthRecord record,
                                     @RequestHeader("Public-Key") String publicKey,
                                     @RequestHeader("Private-Key") String privateKey) {
        log.info("'send' invoked with params '{}, {}, {}, {}'", connectionNumber, record, publicKey, privateKey);

        User user = healthDataService.send(publicKey, privateKey, record, connectionNumber);

        log.info("'send' returned '{}'", user);
        return ResponseEntity.ok(user);
    }

}
