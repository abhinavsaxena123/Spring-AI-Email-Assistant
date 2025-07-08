package com.email.writer.controller;

import com.email.writer.model.EmailRequest;
import com.email.writer.service.EmailGeneratorService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")    // Base path for this controller
@AllArgsConstructor           // Lombok automatically generates a constructor for this class that includes arguments for all fields that need to be set
@CrossOrigin(origins = "*")
public class EmailGeneratorController {

    private final EmailGeneratorService emailGeneratorService;

    @PostMapping("/generate")
    public ResponseEntity<String> generateEmail(@RequestBody EmailRequest emailRequest)
    {
        String response = emailGeneratorService.generateEmailReply(emailRequest);
        return ResponseEntity.ok(response);
    }
}
