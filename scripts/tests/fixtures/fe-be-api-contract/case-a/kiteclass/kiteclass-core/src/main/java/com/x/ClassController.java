package com.x;

import org.springframework.web.bind.annotation.*;

// Case A fixture — BE CHỈ expose GET /api/v1/classes/{id} (KHÔNG có flat list).
// → FE GET /api/v1/classes phải bị detector FLAG (GAP-1069 reproduction).
@RestController
public class ClassController {

    @GetMapping("/api/v1/classes/{classId}")
    public String getClass(@PathVariable String classId) {
        return "ok";
    }
}
