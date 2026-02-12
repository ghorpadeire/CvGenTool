/**
 * DownloadController.java
 *
 * Controller for downloading generated CV files (PDF and LaTeX).
 * Serves binary file content with appropriate headers.
 *
 * @author Pranav Ghorpade
 */
package com.pranav.cvgenerator.controller;

import com.pranav.cvgenerator.model.GeneratedCv;
import com.pranav.cvgenerator.repository.GeneratedCvRepository;
import com.pranav.cvgenerator.service.CvGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for file downloads.
 *
 * Endpoints:
 * - GET /api/download/{id}/pdf → Download PDF file
 * - GET /api/download/{id}/tex → Download LaTeX source
 */
@RestController
@RequestMapping("/api/download")
@Slf4j
public class DownloadController {

    /**
     * Repository for accessing generated CVs.
     */
    private final GeneratedCvRepository repository;

    /**
     * Service for generating filenames.
     */
    private final CvGenerationService generationService;

    /**
     * Constructor with dependency injection.
     */
    public DownloadController(
            GeneratedCvRepository repository,
            CvGenerationService generationService) {
        this.repository = repository;
        this.generationService = generationService;
    }

    /**
     * Downloads the generated PDF file.
     *
     * Response headers:
     * - Content-Type: application/pdf
     * - Content-Disposition: attachment; filename="PranavGhorpadeCvCompanyName.pdf"
     * - Content-Length: [file size]
     *
     * @param id The CV UUID
     * @return PDF file bytes with appropriate headers
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String id) {
        log.info("PDF download request for ID: {}", id);

        try {
            UUID uuid = UUID.fromString(id);
            Optional<GeneratedCv> cvOpt = repository.findById(uuid);

            // Check if CV exists
            if (cvOpt.isEmpty()) {
                log.warn("CV not found: {}", id);
                return ResponseEntity.notFound().build();
            }

            GeneratedCv cv = cvOpt.get();

            // Check if generation is complete
            if (!cv.isCompleted()) {
                log.warn("CV not completed: {}", id);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(null);
            }

            // Check if PDF content exists
            byte[] pdfContent = cv.getPdfContent();
            if (pdfContent == null || pdfContent.length == 0) {
                log.error("PDF content is empty for CV: {}", id);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(null);
            }

            // Generate filename: PranavGhorpadeCvCompanyName.pdf
            String filename = generationService.generatePdfFilename(cv.getCompanyName());

            // Build response with headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename(filename)
                            .build());
            headers.setContentLength(pdfContent.length);

            // Cache control - allow caching for 1 hour
            headers.setCacheControl(CacheControl.maxAge(java.time.Duration.ofHours(1)));

            log.info("Serving PDF: {}, size: {} bytes", filename, pdfContent.length);

            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID: {}", id);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Downloads the LaTeX source file.
     *
     * Response headers:
     * - Content-Type: application/x-tex
     * - Content-Disposition: attachment; filename="PranavGhorpadeCvCompanyName.tex"
     *
     * @param id The CV UUID
     * @return LaTeX source bytes with appropriate headers
     */
    @GetMapping("/{id}/tex")
    public ResponseEntity<byte[]> downloadTex(@PathVariable String id) {
        log.info("LaTeX download request for ID: {}", id);

        try {
            UUID uuid = UUID.fromString(id);
            Optional<GeneratedCv> cvOpt = repository.findById(uuid);

            // Check if CV exists
            if (cvOpt.isEmpty()) {
                log.warn("CV not found: {}", id);
                return ResponseEntity.notFound().build();
            }

            GeneratedCv cv = cvOpt.get();

            // Check if generation is complete
            if (!cv.isCompleted()) {
                log.warn("CV not completed: {}", id);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(null);
            }

            // Check if LaTeX content exists
            String latexContent = cv.getLatexContent();
            if (latexContent == null || latexContent.isBlank()) {
                log.error("LaTeX content is empty for CV: {}", id);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(null);
            }

            // Convert to bytes with UTF-8 encoding
            byte[] texBytes = latexContent.getBytes(StandardCharsets.UTF_8);

            // Generate filename: PranavGhorpadeCvCompanyName.tex
            String filename = generationService.generateTexFilename(cv.getCompanyName());

            // Build response with headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("application", "x-tex", StandardCharsets.UTF_8));
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename(filename)
                            .build());
            headers.setContentLength(texBytes.length);

            // Cache control
            headers.setCacheControl(CacheControl.maxAge(java.time.Duration.ofHours(1)));

            log.info("Serving LaTeX: {}, size: {} bytes", filename, texBytes.length);

            return new ResponseEntity<>(texBytes, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID: {}", id);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Previews the PDF inline in the browser.
     *
     * Same as downloadPdf but uses "inline" disposition so the browser
     * displays the PDF instead of downloading it.
     *
     * @param id The CV UUID
     * @return PDF bytes for inline display
     */
    @GetMapping("/{id}/preview")
    public ResponseEntity<byte[]> previewPdf(@PathVariable String id) {
        log.info("PDF preview request for ID: {}", id);

        try {
            UUID uuid = UUID.fromString(id);
            Optional<GeneratedCv> cvOpt = repository.findById(uuid);

            if (cvOpt.isEmpty() || !cvOpt.get().isCompleted()) {
                return ResponseEntity.notFound().build();
            }

            GeneratedCv cv = cvOpt.get();
            byte[] pdfContent = cv.getPdfContent();

            if (pdfContent == null || pdfContent.length == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            // Use "inline" disposition for in-browser display
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.inline()
                            .filename(generationService.generatePdfFilename(cv.getCompanyName()))
                            .build());
            headers.setContentLength(pdfContent.length);

            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID: {}", id);
            return ResponseEntity.badRequest().build();
        }
    }
}
