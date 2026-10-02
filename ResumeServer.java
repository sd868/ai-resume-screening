import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.InetSocketAddress;
import java.net.URLDecoder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * ============================================================
 * AI RESUME SCREENING SYSTEM
 * ============================================================
 *
 * Standalone Java HTTP server.
 *
 * Features:
 *  - Web interface
 *  - Resume upload
 *  - DOCX text extraction
 *  - TXT text extraction
 *  - Basic PDF text extraction
 *  - Job description processing
 *  - Skill matching
 *  - Score calculation
 *  - Shortlisted / Not Shortlisted decision
 *  - JSON response
 *
 * No Main.java required.
 *
 * Run:
 *
 * javac ResumeServer.java
 *
 * java ResumeServer
 *
 * Open:
 *
 * http://localhost:8080
 *
 * ============================================================
 */
public class ResumeServer {

    private static final int PORT = 8080;

    /*
     * 50 is the shortlist threshold.
     *
     * You can change this later:
     *
     * 50 = easier to shortlist
     * 60 = medium
     * 70 = stricter
     */
    private static final double SHORTLIST_THRESHOLD = 20.0;


    private static final Path WEB_FOLDER =
            Paths.get("web").toAbsolutePath().normalize();


    /*
     * Common technical skills.
     */
    private static final List<String> KNOWN_SKILLS =
            Arrays.asList(

                    "java",
                    "python",
                    "javascript",
                    "typescript",
                    "c",
                    "c++",
                    "c#",
                    "sql",
                    "html",
                    "css",

                    "spring",
                    "spring boot",
                    "springboot",
                    "hibernate",

                    "flask",
                    "django",

                    "react",
                    "react.js",
                    "angular",
                    "vue",

                    "node",
                    "node.js",
                    "express",

                    "rest",
                    "rest api",
                    "restful",

                    "microservices",
                    "api",

                    "mysql",
                    "sqlite",
                    "postgresql",
                    "mongodb",
                    "oracle",

                    "git",
                    "github",
                    "docker",
                    "kubernetes",

                    "aws",
                    "azure",
                    "gcp",

                    "machine learning",
                    "machine-learning",
                    "ml",
                    "artificial intelligence",
                    "ai",

                    "nlp",
                    "natural language processing",

                    "tensorflow",
                    "pytorch",
                    "scikit-learn",
                    "sklearn",

                    "numpy",
                    "pandas",

                    "data science",
                    "data analytics",

                    "linux",
                    "unix",

                    "maven",
                    "gradle",

                    "junit",
                    "postman",

                    "oauth",
                    "jwt",
                    "authentication",

                    "spring security",

                    "cloud",
                    "devops",

                    "jenkins",
                    "ci/cd"
            );


    /*
     * ========================================================
     * MAIN
     * ========================================================
     */

    public static void main(String[] args) {

        try {

            System.out.println();
            System.out.println("=================================================");
            System.out.println("       AI RESUME SCREENING SYSTEM");
            System.out.println("=================================================");
            System.out.println();

            System.out.println(
                    "Web folder: " + WEB_FOLDER
            );

            System.out.println();

            HttpServer server =
                    HttpServer.create(
                            new InetSocketAddress(PORT),
                            0
                    );


            /*
             * Main web page
             */
            server.createContext(
                    "/",
                    ResumeServer::handleWeb
            );


            /*
             * Screening API
             */
            server.createContext(
                    "/screen",
                    ResumeServer::handleScreen
            );


            /*
             * Use a few worker threads.
             */
            server.setExecutor(
                    Executors.newFixedThreadPool(8)
            );


            server.start();


            System.out.println(
                    "Server started successfully."
            );

            System.out.println();

            System.out.println(
                    "Open this in your browser:"
            );

            System.out.println(
                    "http://localhost:8080"
            );

            System.out.println();

            System.out.println(
                    "Waiting for resume..."
            );

            System.out.println(
                    "================================================="
            );

            System.out.println();


        } catch (Exception e) {

            System.err.println(
                    "Could not start server."
            );

            e.printStackTrace();
        }
    }


    /*
     * ========================================================
     * WEB FILE HANDLER
     * ========================================================
     */

    private static void handleWeb(
            HttpExchange exchange
    ) throws IOException {

        try {

            String method =
                    exchange.getRequestMethod();


            if (!method.equalsIgnoreCase("GET")) {

                sendText(
                        exchange,
                        405,
                        "Method Not Allowed"
                );

                return;
            }


            String requestPath =
                    exchange
                            .getRequestURI()
                            .getPath();


            /*
             * Empty path means index.html.
             */
            if (
                    requestPath == null ||
                    requestPath.equals("/") ||
                    requestPath.isBlank()
            ) {

                requestPath =
                        "/index.html";
            }


            /*
             * Prevent directory traversal.
             */
            String relative =
                    requestPath.substring(
                            1
                    );


            Path requested =
                    WEB_FOLDER
                            .resolve(relative)
                            .normalize();


            if (!requested.startsWith(
                    WEB_FOLDER
            )) {

                sendText(
                        exchange,
                        403,
                        "Forbidden"
                );

                return;
            }


            if (!Files.exists(requested) ||
                    Files.isDirectory(requested)) {

                sendText(
                        exchange,
                        404,
                        "File not found"
                );

                return;
            }


            byte[] content =
                    Files.readAllBytes(
                            requested
                    );


            String contentType =
                    getContentType(
                            requested
                    );


            Headers headers =
                    exchange.getResponseHeaders();


            headers.set(
                    "Content-Type",
                    contentType
            );


            headers.set(
                    "Cache-Control",
                    "no-cache"
            );


            exchange.sendResponseHeaders(
                    200,
                    content.length
            );


            try (
                    OutputStream output =
                            exchange.getResponseBody()
            ) {

                output.write(content);
            }


        } catch (Exception e) {

            e.printStackTrace();

            sendText(
                    exchange,
                    500,
                    "Web server error: " +
                            e.getMessage()
            );
        }
    }


    /*
     * ========================================================
     * SCREENING ENDPOINT
     * ========================================================
     */

    private static void handleScreen(
            HttpExchange exchange
    ) throws IOException {

        System.out.println();
        System.out.println(
                "---------------------------------------------"
        );

        System.out.println(
                "Screening request received"
        );


        try {

            if (!exchange
                    .getRequestMethod()
                    .equalsIgnoreCase("POST")) {

                sendJson(
                        exchange,
                        405,
                        errorJson(
                                "Only POST is allowed."
                        )
                );

                return;
            }


            /*
             * Read request body.
             */
            byte[] requestBody =
                    exchange
                            .getRequestBody()
                            .readAllBytes();


            System.out.println(
                    "Request size: " +
                            requestBody.length +
                            " bytes"
            );


            /*
             * Content type.
             */
            String contentType =
                    exchange
                            .getRequestHeaders()
                            .getFirst(
                                    "Content-Type"
                            );


            if (contentType == null) {

                sendJson(
                        exchange,
                        400,
                        errorJson(
                                "Missing Content-Type."
                        )
                );

                return;
            }


            /*
             * Parse multipart form.
             */
            MultipartData multipart =
                    parseMultipart(
                            requestBody,
                            contentType
                    );


            String job =
                    multipart.fields.get(
                            "job"
                    );


            UploadedFile resume =
                    multipart.files.get(
                            "resume"
                    );


            /*
             * Validate job description.
             */
            if (
                    job == null ||
                    job.trim().isEmpty()
            ) {

                sendJson(
                        exchange,
                        400,
                        errorJson(
                                "Job description is required."
                        )
                );

                return;
            }


            /*
             * Validate resume.
             */
            if (resume == null) {

                sendJson(
                        exchange,
                        400,
                        errorJson(
                                "Resume file is required."
                        )
                );

                return;
            }


            if (
                    resume.data == null ||
                    resume.data.length == 0
            ) {

                sendJson(
                        exchange,
                        400,
                        errorJson(
                                "The uploaded resume is empty."
                        )
                );

                return;
            }


            System.out.println(
                    "Resume received: " +
                            resume.fileName
            );


            System.out.println(
                    "Resume size: " +
                            resume.data.length +
                            " bytes"
            );


            /*
             * =================================================
             * EXTRACT RESUME TEXT
             * =================================================
             */

            String resumeText;


            try {

                resumeText =
                        extractResumeText(
                                resume.fileName,
                                resume.data
                        );


            } catch (Exception e) {

                System.err.println(
                        "Resume extraction failed:"
                );

                e.printStackTrace();


                sendJson(
                        exchange,
                        400,
                        errorJson(
                                "Unable to read the uploaded resume."
                        )
                );

                return;
            }


            /*
             * IMPORTANT:
             *
             * This was the error in your screenshot.
             *
             * We now explicitly check extracted text.
             */
            if (
                    resumeText == null ||
                    resumeText.trim().isEmpty()
            ) {

                System.err.println(
                        "No readable text extracted from resume."
                );


                sendJson(
                        exchange,
                        400,
                        errorJson(
                                "The uploaded resume contains no readable text."
                        )
                );

                return;
            }


            System.out.println(
                    "Extracted resume text length: " +
                            resumeText.length()
            );


            System.out.println(
                    "Resume text successfully extracted."
            );


            /*
             * =================================================
             * SCREENING
             * =================================================
             */

            ScreeningResult screening =
                    analyzeResume(
                            job,
                            resumeText,
                            resume.fileName
                    );


            System.out.println(
                    "Score: " +
                            screening.score
            );


            System.out.println(
                    "Decision: " +
                            screening.decision
            );


            System.out.println(
                    "Candidate: " +
                            screening.candidateName
            );


            /*
             * =================================================
             * RETURN JSON
             * =================================================
             */

            String json =
                    screening.toJson();


            sendJson(
                    exchange,
                    200,
                    json
            );


            System.out.println(
                    "Result sent successfully."
            );


        } catch (Exception e) {

            System.err.println(
                    "SCREENING ERROR:"
            );

            e.printStackTrace();


            sendJson(
                    exchange,
                    500,
                    errorJson(
                            "Server error: " +
                                    e.getMessage()
                    )
            );


        } finally {

            System.out.println(
                    "---------------------------------------------"
            );
        }
    }


    /*
     * ========================================================
     * EXTRACT RESUME TEXT
     * ========================================================
     */

    private static String extractResumeText(
            String fileName,
            byte[] data
    ) throws Exception {

        String lower =
                fileName.toLowerCase();


        /*
         * DOCX
         */
        if (lower.endsWith(".docx")) {

            System.out.println(
                    "Reading DOCX resume..."
            );

            return extractDocxText(
                    data
            );
        }


        /*
         * TXT
         */
        if (lower.endsWith(".txt")) {

            System.out.println(
                    "Reading TXT resume..."
            );

            return new String(
                    data,
                    StandardCharsets.UTF_8
            ).trim();
        }


        /*
         * PDF
         */
        if (lower.endsWith(".pdf")) {

            System.out.println(
                    "Reading PDF resume..."
            );

            return extractPdfText(
                    data
            );
        }


        throw new Exception(
                "Unsupported file type. Please upload PDF, DOCX or TXT."
        );
    }


    /*
     * ========================================================
     * DOCX TEXT EXTRACTION
     * ========================================================
     *
     * DOCX is actually a ZIP file containing XML.
     *
     * We open:
     *
     * word/document.xml
     *
     * and extract all <w:t> text nodes.
     *
     * This fixes the exact error from your screenshot.
     */

    private static String extractDocxText(
            byte[] data
    ) throws Exception {

        StringBuilder text =
                new StringBuilder();


        try (
                ZipInputStream zip =
                        new ZipInputStream(
                                new ByteArrayInputStream(
                                        data
                                )
                        )
        ) {

            ZipEntry entry;


            while (
                    (entry =
                            zip.getNextEntry()) != null
            ) {

                if (
                        entry.getName()
                                .equals(
                                        "word/document.xml"
                                )
                ) {

                    ByteArrayOutputStream output =
                            new ByteArrayOutputStream();


                    byte[] buffer =
                            new byte[8192];


                    int count;


                    while (
                            (count =
                                    zip.read(
                                            buffer
                                    )) != -1
                    ) {

                        output.write(
                                buffer,
                                0,
                                count
                        );
                    }


                    byte[] xml =
                            output.toByteArray();


                    return extractWordXmlText(
                            xml
                    );
                }
            }
        }


        return "";
    }


    /*
     * ========================================================
     * WORD XML TEXT
     * ========================================================
     */

    private static String extractWordXmlText(
            byte[] xml
    ) throws Exception {

        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();


        /*
         * Secure XML configuration.
         */
        try {

            factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true
            );

        } catch (Exception ignored) {
        }


        try {

            factory.setFeature(
                    "http://xml.org/sax/features/external-general-entities",
                    false
            );

        } catch (Exception ignored) {
        }


        try {

            factory.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false
            );

        } catch (Exception ignored) {
        }


        try {

            factory.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false
            );

        } catch (Exception ignored) {
        }


        factory.setXIncludeAware(
                false
        );


        factory.setExpandEntityReferences(
                false
        );


        Document document =
                factory
                        .newDocumentBuilder()
                        .parse(
                                new ByteArrayInputStream(
                                        xml
                                )
                        );


        StringBuilder text =
                new StringBuilder();


        /*
         * Namespace-independent search.
         */
        NodeList allNodes =
                document.getElementsByTagName("*");


        for (
                int i = 0;
                i < allNodes.getLength();
                i++
        ) {

            Node node =
                    allNodes.item(i);


            String localName =
                    node.getLocalName();


            String nodeName =
                    node.getNodeName();


            /*
             * Word text node:
             *
             * w:t
             */
            if (
                    "t".equals(localName) ||
                    "w:t".equals(nodeName)
            ) {

                String value =
                        node.getTextContent();


                if (
                        value != null &&
                        !value.isBlank()
                ) {

                    text
                            .append(value)
                            .append(" ");
                }
            }


            /*
             * Add line break after paragraphs.
             */
            if (
                    "p".equals(localName) ||
                    "w:p".equals(nodeName)
            ) {

                text.append("\n");
            }
        }


        return cleanText(
                text.toString()
        );
    }


    /*
     * ========================================================
     * BASIC PDF TEXT EXTRACTION
     * ========================================================
     *
     * This handles PDFs containing readable text streams.
     *
     * For scanned/image-only PDFs, OCR would be required.
     */

    private static String extractPdfText(
            byte[] data
    ) {

        String raw =
                new String(
                        data,
                        StandardCharsets.ISO_8859_1
                );


        StringBuilder result =
                new StringBuilder();


        /*
         * Look for PDF literal strings.
         */
        Pattern pattern =
                Pattern.compile(
                        "\\((.*?)\\)",
                        Pattern.DOTALL
                );


        Matcher matcher =
                pattern.matcher(
                        raw
                );


        while (matcher.find()) {

            String value =
                    matcher.group(1);


            if (
                    value == null ||
                    value.isBlank()
            ) {
                continue;
            }


            /*
             * Basic PDF escape handling.
             */
            value =
                    value
                            .replace(
                                    "\\(",
                                    "("
                            )
                            .replace(
                                    "\\)",
                                    ")"
                            )
                            .replace(
                                    "\\\\",
                                    "\\"
                            )
                            .replace(
                                    "\\n",
                                    "\n"
                            )
                            .replace(
                                    "\\r",
                                    "\r"
                            );


            /*
             * Ignore obvious binary strings.
             */
            int printable = 0;


            for (
                    int i = 0;
                    i < value.length();
                    i++
            ) {

                char c =
                        value.charAt(i);


                if (
                        c == '\n' ||
                        c == '\r' ||
                        c == '\t' ||
                        (c >= 32 && c <= 126)
                ) {

                    printable++;
                }
            }


            if (
                    printable >=
                            value.length() * 0.70
            ) {

                result
                        .append(value)
                        .append(" ");
            }
        }


        return cleanText(
                result.toString()
        );
    }


    /*
     * ========================================================
     * ANALYZE RESUME
     * ========================================================
     */

    private static ScreeningResult analyzeResume(
            String jobDescription,
            String resumeText,
            String fileName
    ) {

        ScreeningResult result =
                new ScreeningResult();


        String job =
                jobDescription
                        .toLowerCase();


        String resume =
                resumeText
                        .toLowerCase();


        /*
         * Candidate name.
         */
        result.candidateName =
                extractCandidateName(
                        resumeText,
                        fileName
                );


        /*
         * Skills requested by job.
         */
        List<String> requiredSkills =
                extractSkills(
                        job
                );


        /*
         * Skills found in resume.
         */
        List<String> resumeSkills =
                extractSkills(
                        resume
                );


        /*
         * If no recognizable technical
         * skills were found in the job,
         * use meaningful words from job description.
         */
        if (requiredSkills.isEmpty()) {

            requiredSkills =
                    extractImportantWords(
                            job
                    );
        }


        /*
         * Matching skills.
         */
        LinkedHashSet<String> matching =
                new LinkedHashSet<>();


        LinkedHashSet<String> missing =
                new LinkedHashSet<>();


        for (
                String skill :
                requiredSkills
        ) {

            if (
                    containsSkill(
                            resume,
                            skill
                    )
            ) {

                matching.add(
                        displaySkill(
                                skill
                        )
                );

            } else {

                missing.add(
                        displaySkill(
                                skill
                        )
                );
            }
        }


        result.matchingSkills =
                new ArrayList<>(
                        matching
                );


        result.missingSkills =
                new ArrayList<>(
                        missing
                );


        /*
         * =====================================================
         * SCORE
         * =====================================================
         *
         * 70% skill match
         * 20% general keyword match
         * 10% experience/education
         */

        double skillScore;


        if (requiredSkills.isEmpty()) {

            skillScore = 0;

        } else {

            skillScore =
                    (
                            matching.size() * 100.0
                    ) /
                    requiredSkills.size();
        }


        /*
         * General keyword score.
         */
        double keywordScore =
                calculateKeywordScore(
                        job,
                        resume
                );


        /*
         * Experience score.
         */
        double experienceScore =
                calculateExperienceScore(
                        job,
                        resume
                );


        /*
         * Education score.
         */
        double educationScore =
                calculateEducationScore(
                        job,
                        resume
                );


        /*
         * Weighted result.
         */
        double score =
                (
                        skillScore * 0.70
                )
                +
                (
                        keywordScore * 0.15
                )
                +
                (
                        experienceScore * 0.10
                )
                +
                (
                        educationScore * 0.05
                );


        /*
         * Keep score between 0 and 100.
         */
        score =
                Math.max(
                        0,
                        Math.min(
                                100,
                                score
                        )
                );


        result.score =
                Math.round(
                        score * 10.0
                ) / 10.0;


        /*
         * Shortlist decision.
         */
        if (
                result.score >=
                        SHORTLIST_THRESHOLD
        ) {

            result.decision =
                    "SHORTLISTED";

        } else {

            result.decision =
                    "NOT SHORTLISTED";
        }


        /*
         * Experience.
         */
        result.experience =
                extractExperience(
                        resumeText
                );


        /*
         * Education.
         */
        result.education =
                extractEducation(
                        resumeText
                );


        /*
         * Summary.
         */
        result.summary =
                buildSummary(
                        result
                );


        return result;
    }


    /*
     * ========================================================
     * EXTRACT SKILLS
     * ========================================================
     */

    private static List<String> extractSkills(
            String text
    ) {

        LinkedHashSet<String> found =
                new LinkedHashSet<>();


        for (
                String skill :
                KNOWN_SKILLS
        ) {

            if (
                    containsSkill(
                            text,
                            skill
                    )
            ) {

                found.add(
                        skill
                );
            }
        }


        return new ArrayList<>(
                found
        );
    }


    /*
     * ========================================================
     * CONTAINS SKILL
     * ========================================================
     */

    private static boolean containsSkill(
            String text,
            String skill
    ) {

        if (
                text == null ||
                skill == null
        ) {

            return false;
        }


        String normalizedText =
                text.toLowerCase();


        String normalizedSkill =
                skill.toLowerCase();


        /*
         * Multi-word skills.
         */
        if (
                normalizedSkill.contains(" ") ||
                normalizedSkill.contains(".") ||
                normalizedSkill.contains("+") ||
                normalizedSkill.contains("/")
        ) {

            return normalizedText.contains(
                    normalizedSkill
            );
        }


        /*
         * Word boundary for normal words.
         */
        Pattern pattern =
                Pattern.compile(
                        "(?<![a-z0-9])" +
                                Pattern.quote(
                                        normalizedSkill
                                ) +
                                "(?![a-z0-9])"
                );


        return pattern
                .matcher(
                        normalizedText
                )
                .find();
    }


    /*
     * ========================================================
     * IMPORTANT WORDS
     * ========================================================
     */

    private static List<String> extractImportantWords(
            String text
    ) {

        Set<String> stopWords =
                new HashSet<>(
                        Arrays.asList(
                                "the",
                                "and",
                                "with",
                                "for",
                                "from",
                                "that",
                                "this",
                                "are",
                                "you",
                                "your",
                                "our",
                                "will",
                                "have",
                                "has",
                                "required",
                                "requirements",
                                "experience",
                                "years",
                                "developer",
                                "candidate",
                                "looking"
                        )
                );


        LinkedHashSet<String> words =
                new LinkedHashSet<>();


        String[] parts =
                text.toLowerCase()
                        .split(
                                "[^a-z0-9+#.-]+"
                        );


        for (
                String word :
                parts
        ) {

            if (
                    word.length() >= 4 &&
                    !stopWords.contains(word)
            ) {

                words.add(word);
            }


            if (words.size() >= 15) {
                break;
            }
        }


        return new ArrayList<>(
                words
        );
    }


    /*
     * ========================================================
     * KEYWORD SCORE
     * ========================================================
     */

    private static double calculateKeywordScore(
            String job,
            String resume
    ) {

        String[] words =
                job.toLowerCase()
                        .split(
                                "[^a-z0-9+#.-]+"
                        );


        Set<String> stopWords =
                new HashSet<>(
                        Arrays.asList(
                                "the",
                                "and",
                                "with",
                                "for",
                                "from",
                                "that",
                                "this",
                                "are",
                                "you",
                                "your",
                                "our",
                                "will",
                                "have",
                                "has",
                                "looking",
                                "required",
                                "requirements",
                                "candidate",
                                "developer",
                                "experience"
                        )
                );


        int total = 0;
        int matched = 0;


        Set<String> unique =
                new HashSet<>(
                        Arrays.asList(
                                words
                        )
                );


        for (
                String word :
                unique
        ) {

            if (
                    word.length() < 4 ||
                    stopWords.contains(word)
            ) {

                continue;
            }


            total++;


            if (
                    resume.contains(
                            word
                    )
            ) {

                matched++;
            }
        }


        if (total == 0) {
            return 0;
        }


        return (
                matched * 100.0
        ) / total;
    }


    /*
     * ========================================================
     * EXPERIENCE SCORE
     * ========================================================
     */

    private static double calculateExperienceScore(
            String job,
            String resume
    ) {

        int required =
                extractRequiredYears(
                        job
                );


        int candidate =
                extractMaximumYears(
                        resume
                );


        if (required <= 0) {

            return 100;
        }


        if (candidate >= required) {

            return 100;
        }


        if (candidate <= 0) {

            return 0;
        }


        return Math.min(
                100,
                (
                        candidate * 100.0
                ) / required
        );
    }


    /*
     * ========================================================
     * EDUCATION SCORE
     * ========================================================
     */

    private static double calculateEducationScore(
            String job,
            String resume
    ) {

        String lowerJob =
                job.toLowerCase();


        String lowerResume =
                resume.toLowerCase();


        if (
                lowerJob.contains("b.tech") ||
                lowerJob.contains("btech") ||
                lowerJob.contains("bachelor")
        ) {

            if (
                    lowerResume.contains("b.tech") ||
                    lowerResume.contains("btech") ||
                    lowerResume.contains("bachelor")
            ) {

                return 100;
            }


            return 0;
        }


        if (
                lowerJob.contains("m.tech") ||
                lowerJob.contains("mtech") ||
                lowerJob.contains("master")
        ) {

            if (
                    lowerResume.contains("m.tech") ||
                    lowerResume.contains("mtech") ||
                    lowerResume.contains("master")
            ) {

                return 100;
            }


            return 0;
        }


        return 100;
    }


    /*
     * ========================================================
     * REQUIRED YEARS
     * ========================================================
     */

    private static int extractRequiredYears(
            String text
    ) {

        Pattern pattern =
                Pattern.compile(
                        "(\\d+)\\s*\\+?\\s*(?:years?|yrs?)"
                );


        Matcher matcher =
                pattern.matcher(
                        text.toLowerCase()
                );


        if (matcher.find()) {

            try {

                return Integer.parseInt(
                        matcher.group(1)
                );

            } catch (Exception ignored) {
            }
        }


        return 0;
    }


    /*
     * ========================================================
     * MAXIMUM YEARS
     * ========================================================
     */

    private static int extractMaximumYears(
            String text
    ) {

        Pattern pattern =
                Pattern.compile(
                        "(\\d+)\\s*\\+?\\s*(?:years?|yrs?)"
                );


        Matcher matcher =
                pattern.matcher(
                        text.toLowerCase()
                );


        int maximum = 0;


        while (matcher.find()) {

            try {

                int years =
                        Integer.parseInt(
                                matcher.group(1)
                        );


                maximum =
                        Math.max(
                                maximum,
                                years
                        );

            } catch (Exception ignored) {
            }
        }


        return maximum;
    }


    /*
     * ========================================================
     * CANDIDATE NAME
     * ========================================================
     */

    private static String extractCandidateName(
            String text,
            String fileName
    ) {

        String[] lines =
                text.split(
                        "\\R"
                );


        for (
                String line :
                lines
        ) {

            String clean =
                    line.trim();


            if (clean.isEmpty()) {
                continue;
            }


            if (
                    clean.length() > 80
            ) {
                continue;
            }


            String lower =
                    clean.toLowerCase();


            if (
                    lower.startsWith("name:")
            ) {

                return clean
                        .substring(5)
                        .trim();
            }
        }


        /*
         * Try first line.
         */
        if (lines.length > 0) {

            String first =
                    lines[0].trim();


            if (
                    !first.isEmpty() &&
                    first.length() < 60
            ) {

                return first;
            }
        }


        /*
         * Filename fallback.
         */
        if (
                fileName != null &&
                !fileName.isBlank()
        ) {

            String name =
                    fileName
                            .replace(
                                    ".docx",
                                    ""
                            )
                            .replace(
                                    ".pdf",
                                    ""
                            )
                            .replace(
                                    ".txt",
                                    ""
                            )
                            .replace(
                                    "_",
                                    " "
                            )
                            .replace(
                                    "-",
                                    " "
                            );


            if (!name.isBlank()) {

                return name;
            }
        }


        return "Candidate";
    }


    /*
     * ========================================================
     * EXPERIENCE TEXT
     * ========================================================
     */

    private static String extractExperience(
            String text
    ) {

        Pattern pattern =
                Pattern.compile(
                        "(\\d+)\\s*\\+?\\s*(years?|yrs?)",
                        Pattern.CASE_INSENSITIVE
                );


        Matcher matcher =
                pattern.matcher(
                        text
                );


        if (matcher.find()) {

            return matcher.group(1) +
                    " years";
        }


        return "Not specified";
    }


    /*
     * ========================================================
     * EDUCATION TEXT
     * ========================================================
     */

    private static String extractEducation(
            String text
    ) {

        String[] lines =
                text.split(
                        "\\R"
                );


        for (
                String line :
                lines
        ) {

            String lower =
                    line.toLowerCase();


            if (
                    lower.contains("b.tech") ||
                    lower.contains("btech") ||
                    lower.contains("bachelor")
            ) {

                return line.trim();
            }


            if (
                    lower.contains("m.tech") ||
                    lower.contains("mtech") ||
                    lower.contains("master")
            ) {

                return line.trim();
            }


            if (
                    lower.contains("b.sc") ||
                    lower.contains("bsc")
            ) {

                return line.trim();
            }


            if (
                    lower.contains("m.sc") ||
                    lower.contains("msc")
            ) {

                return line.trim();
            }
        }


        return "Not specified";
    }


    /*
     * ========================================================
     * SUMMARY
     * ========================================================
     */

    private static String buildSummary(
            ScreeningResult result
    ) {

        if (
                result.decision.equals(
                        "SHORTLISTED"
                )
        ) {

            return
                    "The candidate achieved a " +
                    result.score +
                    "% match score. " +
                    "The resume meets the current screening threshold with " +
                    result.matchingSkills.size() +
                    " matching skill(s).";
        }


        return
                "The candidate achieved a " +
                result.score +
                "% match score. " +
                "Some job requirements or skills were not fully matched.";
    }


    /*
     * ========================================================
     * CLEAN TEXT
     * ========================================================
     */

    private static String cleanText(
            String text
    ) {

        if (text == null) {
            return "";
        }


        return text
                .replace(
                        "\u0000",
                        " "
                )
                .replaceAll(
                        "[ \\t]+",
                        " "
                )
                .replaceAll(
                        "\\n\\s*\\n+",
                        "\n"
                )
                .trim();
    }


    /*
     * ========================================================
     * MULTIPART PARSER
     * ========================================================
     */

    private static MultipartData parseMultipart(
            byte[] body,
            String contentType
    ) throws Exception {

        MultipartData result =
                new MultipartData();


        String boundary =
                getBoundary(
                        contentType
                );


        if (
                boundary == null ||
                boundary.isEmpty()
        ) {

            throw new Exception(
                    "Multipart boundary not found."
            );
        }


        byte[] boundaryBytes =
                (
                        "--" +
                        boundary
                )
                        .getBytes(
                                StandardCharsets.ISO_8859_1
                        );


        int position = 0;


        while (position < body.length) {

            int start =
                    indexOf(
                            body,
                            boundaryBytes,
                            position
                    );


            if (start < 0) {
                break;
            }


            start +=
                    boundaryBytes.length;


            /*
             * Final boundary.
             */
            if (
                    start + 1 < body.length &&
                    body[start] == '-' &&
                    body[start + 1] == '-'
            ) {

                break;
            }


            /*
             * Skip CRLF.
             */
            if (
                    start + 1 < body.length &&
                    body[start] == '\r' &&
                    body[start + 1] == '\n'
            ) {

                start += 2;
            }


            /*
             * Headers end at:
             *
             * \r\n\r\n
             */
            byte[] headerSeparator =
                    new byte[] {
                            '\r',
                            '\n',
                            '\r',
                            '\n'
                    };


            int headerEnd =
                    indexOf(
                            body,
                            headerSeparator,
                            start
                    );


            if (headerEnd < 0) {
                break;
            }


            String headers =
                    new String(
                            body,
                            start,
                            headerEnd - start,
                            StandardCharsets.ISO_8859_1
                    );


            int dataStart =
                    headerEnd +
                            headerSeparator.length;


            int nextBoundary =
                    indexOf(
                            body,
                            boundaryBytes,
                            dataStart
                    );


            if (nextBoundary < 0) {
                break;
            }


            int dataEnd =
                    nextBoundary;


            /*
             * Remove CRLF before boundary.
             */
            if (
                    dataEnd >= 2 &&
                    body[dataEnd - 2] == '\r' &&
                    body[dataEnd - 1] == '\n'
            ) {

                dataEnd -= 2;
            }


            byte[] partData =
                    Arrays.copyOfRange(
                            body,
                            dataStart,
                            dataEnd
                    );


            String fieldName =
                    getDispositionValue(
                            headers,
                            "name"
                    );


            String fileName =
                    getDispositionValue(
                            headers,
                            "filename"
                    );


            if (
                    fieldName != null &&
                    fileName != null
            ) {

                UploadedFile file =
                        new UploadedFile();


                file.fileName =
                        fileName;


                file.data =
                        partData;


                result.files.put(
                        fieldName,
                        file
                );


            } else if (
                    fieldName != null
            ) {

                result.fields.put(
                        fieldName,
                        new String(
                                partData,
                                StandardCharsets.UTF_8
                        )
                );
            }


            position =
                    nextBoundary;
        }


        return result;
    }


    /*
     * ========================================================
     * GET BOUNDARY
     * ========================================================
     */

    private static String getBoundary(
            String contentType
    ) {

        Pattern pattern =
                Pattern.compile(
                        "boundary=\"?([^\";]+)\"?",
                        Pattern.CASE_INSENSITIVE
                );


        Matcher matcher =
                pattern.matcher(
                        contentType
                );


        if (matcher.find()) {

            return matcher.group(1);
        }


        return null;
    }


    /*
     * ========================================================
     * CONTENT DISPOSITION VALUE
     * ========================================================
     */

    private static String getDispositionValue(
            String headers,
            String key
    ) {

        Pattern pattern =
                Pattern.compile(
                        key +
                                "=\"([^\"]*)\"",
                        Pattern.CASE_INSENSITIVE
                );


        Matcher matcher =
                pattern.matcher(
                        headers
                );


        if (matcher.find()) {

            return matcher.group(1);
        }


        return null;
    }


    /*
     * ========================================================
     * BYTE ARRAY SEARCH
     * ========================================================
     */

    private static int indexOf(
            byte[] source,
            byte[] target,
            int start
    ) {

        outer:

        for (
                int i = start;
                i <= source.length -
                        target.length;
                i++
        ) {

            for (
                    int j = 0;
                    j < target.length;
                    j++
            ) {

                if (
                        source[i + j]
                                !=
                        target[j]
                ) {

                    continue outer;
                }
            }


            return i;
        }


        return -1;
    }


    /*
     * ========================================================
     * CONTENT TYPE
     * ========================================================
     */

    private static String getContentType(
            Path path
    ) {

        String name =
                path.getFileName()
                        .toString()
                        .toLowerCase();


        if (name.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }


        if (name.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }


        if (name.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }


        if (name.endsWith(".json")) {
            return "application/json; charset=UTF-8";
        }


        if (name.endsWith(".png")) {
            return "image/png";
        }


        if (
                name.endsWith(".jpg") ||
                name.endsWith(".jpeg")
        ) {

            return "image/jpeg";
        }


        if (name.endsWith(".svg")) {
            return "image/svg+xml";
        }


        return "application/octet-stream";
    }


    /*
     * ========================================================
     * SEND TEXT
     * ========================================================
     */

    private static void sendText(
            HttpExchange exchange,
            int status,
            String text
    ) throws IOException {

        byte[] bytes =
                text.getBytes(
                        StandardCharsets.UTF_8
                );


        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        "text/plain; charset=UTF-8"
                );


        exchange.sendResponseHeaders(
                status,
                bytes.length
        );


        try (
                OutputStream output =
                        exchange.getResponseBody()
        ) {

            output.write(bytes);
        }
    }


    /*
     * ========================================================
     * SEND JSON
     * ========================================================
     */

    private static void sendJson(
            HttpExchange exchange,
            int status,
            String json
    ) throws IOException {

        byte[] bytes =
                json.getBytes(
                        StandardCharsets.UTF_8
                );


        Headers headers =
                exchange.getResponseHeaders();


        headers.set(
                "Content-Type",
                "application/json; charset=UTF-8"
        );


        headers.set(
                "Access-Control-Allow-Origin",
                "*"
        );


        exchange.sendResponseHeaders(
                status,
                bytes.length
        );


        try (
                OutputStream output =
                        exchange.getResponseBody()
        ) {

            output.write(bytes);
        }
    }


    /*
     * ========================================================
     * ERROR JSON
     * ========================================================
     */

    private static String errorJson(
            String message
    ) {

        return
                "{"
                +
                "\"error\":\""
                +
                jsonEscape(message)
                +
                "\""
                +
                "}";
    }


    /*
     * ========================================================
     * JSON ESCAPE
     * ========================================================
     */

    private static String jsonEscape(
            String value
    ) {

        if (value == null) {
            return "";
        }


        return value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\"",
                        "\\\""
                )
                .replace(
                        "\r",
                        "\\r"
                )
                .replace(
                        "\n",
                        "\\n"
                )
                .replace(
                        "\t",
                        "\\t"
                );
    }


    /*
     * ========================================================
     * DISPLAY SKILL
     * ========================================================
     */

    private static String displaySkill(
            String skill
    ) {

        if (skill == null) {
            return "";
        }


        switch (
                skill.toLowerCase()
        ) {

            case "springboot":
                return "Spring Boot";

            case "react.js":
                return "React";

            case "node.js":
                return "Node.js";

            case "scikit-learn":
                return "Scikit-learn";

            case "sklearn":
                return "Scikit-learn";

            case "machine-learning":
                return "Machine Learning";

            case "rest":
                return "REST";

            default:

                if (
                        skill.length() <= 4
                ) {

                    return skill.toUpperCase();
                }


                return Character.toUpperCase(
                        skill.charAt(0)
                )
                        +
                        skill.substring(1);
        }
    }


    /*
     * ========================================================
     * UPLOADED FILE
     * ========================================================
     */

    private static class UploadedFile {

        String fileName;

        byte[] data;
    }


    /*
     * ========================================================
     * MULTIPART DATA
     * ========================================================
     */

    private static class MultipartData {

        Map<String, String> fields =
                new HashMap<>();


        Map<String, UploadedFile> files =
                new HashMap<>();
    }


    /*
     * ========================================================
     * SCREENING RESULT
     * ========================================================
     */

    private static class ScreeningResult {

        String candidateName =
                "Candidate";


        double score =
                0;


        String decision =
                "NOT SHORTLISTED";


        String experience =
                "Not specified";


        String education =
                "Not specified";


        List<String> matchingSkills =
                new ArrayList<>();


        List<String> missingSkills =
                new ArrayList<>();


        String summary =
                "";


        /*
         * ====================================================
         * TO JSON
         * ====================================================
         */

        String toJson() {

            StringBuilder json =
                    new StringBuilder();


            json.append("{");


            json.append(
                    "\"candidateName\":\""
            );

            json.append(
                    jsonEscape(
                            candidateName
                    )
            );

            json.append("\",");


            json.append(
                    "\"score\":"
            );

            json.append(
                    String.format(
                            java.util.Locale.US,
                            "%.1f",
                            score
                    )
            );

            json.append(",");


            json.append(
                    "\"decision\":\""
            );

            json.append(
                    jsonEscape(
                            decision
                    )
            );

            json.append("\",");


            json.append(
                    "\"experience\":\""
            );

            json.append(
                    jsonEscape(
                            experience
                    )
            );

            json.append("\",");


            json.append(
                    "\"education\":\""
            );

            json.append(
                    jsonEscape(
                            education
                    )
            );

            json.append("\",");


            json.append(
                    "\"matchingSkills\":["
            );


            for (
                    int i = 0;
                    i < matchingSkills.size();
                    i++
            ) {

                if (i > 0) {
                    json.append(",");
                }


                json.append("\"");

                json.append(
                        jsonEscape(
                                matchingSkills
                                        .get(i)
                        )
                );

                json.append("\"");
            }


            json.append("],");


            json.append(
                    "\"missingSkills\":["
            );


            for (
                    int i = 0;
                    i < missingSkills.size();
                    i++
            ) {

                if (i > 0) {
                    json.append(",");
                }


                json.append("\"");

                json.append(
                        jsonEscape(
                                missingSkills
                                        .get(i)
                        )
                );

                json.append("\"");
            }


            json.append("],");


            json.append(
                    "\"summary\":\""
            );

            json.append(
                    jsonEscape(
                            summary
                    )
            );

            json.append("\"");


            json.append("}");


            return json.toString();
        }
    }
}