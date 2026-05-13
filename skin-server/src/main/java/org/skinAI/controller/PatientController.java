package org.skinAI.controller;

import org.skinAI.pojo.Result;
import org.skinAI.pojo.medical.Patient;
import org.skinAI.services.PatientService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    public Result<Patient> create(@RequestBody Patient patient) {
        return Result.success(patientService.create(patient));
    }

    @PutMapping("/{id}")
    public Result<Patient> update(@PathVariable("id") Long id, @RequestBody Patient patient) {
        patient.setId(id);
        Patient updated = patientService.update(patient);
        if (updated == null) {
            return Result.error("patient not found");
        }
        return Result.success(updated);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable("id") Long id) {
        int affected = patientService.deleteById(id);
        if (affected > 0) {
            return Result.success();
        }
        return Result.error("patient not found");
    }

    @GetMapping("/{id}")
    public Result<Patient> getById(@PathVariable("id") Long id) {
        Patient patient = patientService.getById(id);
        if (patient == null) {
            return Result.error("patient not found");
        }
        return Result.success(patient);
    }

    @GetMapping
    public Result<List<Patient>> list(@RequestParam(value = "keyword", required = false) String keyword) {
        return Result.success(patientService.list(keyword));
    }
}
