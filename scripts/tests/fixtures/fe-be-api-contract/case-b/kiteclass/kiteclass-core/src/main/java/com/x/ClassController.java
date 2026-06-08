package com.x;

import org.springframework.web.bind.annotation.*;

// Case B fixture — post-GAP-1069 fix: BE giờ CÓ flat list GET /api/v1/classes.
// → FE GET /api/v1/classes phải PASS (khớp method + path).
@RestController
public class ClassController {

    @GetMapping("/api/v1/classes")
    public String listClasses() {
        return "ok";
    }

    @GetMapping("/api/v1/classes/{classId}")
    public String getClass(@PathVariable String classId) {
        return "ok";
    }
}
