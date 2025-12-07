package com.example.dynamicquery.controller;

import com.example.dynamicquery.dto.GenericRequest;
import com.example.dynamicquery.service.GenericQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.Map;

@RestController
@RequestMapping("/dynamic")
public class DynamicQueryController {
    @Autowired
    private GenericQueryService genericQueryService;

    @PostMapping("/fetch")
    public Map<String, Object> fetch(@RequestBody GenericRequest request) throws Exception {
        return genericQueryService.fetchData(request);
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> download(@RequestParam String filePath) throws IOException {
        File f = new File(filePath);
        if (!f.exists()) return ResponseEntity.notFound().build();
        InputStreamResource resource = new InputStreamResource(new FileInputStream(f));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + f.getName() + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(f.length())
                .body(resource);
    }
}
