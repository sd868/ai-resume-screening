const API_URL = "/screen";

const jobInput = document.getElementById("job");
const resumeInput = document.getElementById("resume");
const selectedFile = document.getElementById("selectedFiles");

const screenButton = document.getElementById("screenButton");

const loading = document.getElementById("loading");
const processingText = document.getElementById("processingText");
const progressBar = document.getElementById("progressBar");
const progressPercent = document.getElementById("progressPercent");

const result = document.getElementById("result");

const scoreElement = document.getElementById("score");
const candidateNameElement = document.getElementById("candidateName");
const experienceElement = document.getElementById("experience");
const educationElement = document.getElementById("education");
const decisionElement = document.getElementById("decision");

const matchingSkillsElement = document.getElementById("matchingSkills");
const missingSkillsElement = document.getElementById("missingSkills");
const summaryElement = document.getElementById("summary");


// ======================================================
// FILE SELECT
// ======================================================

resumeInput.addEventListener("change", function () {

    if (this.files.length === 0) {

        selectedFile.textContent =
            "No resume selected";

        return;
    }

    selectedFile.textContent =
        "✓ " + this.files[0].name;
});


// ======================================================
// SCREEN RESUME
// ======================================================

async function screenResume() {

    console.log("Screen button clicked");

    const job =
        jobInput.value.trim();

    if (!job) {

        alert("Please enter the Job Description.");

        return;
    }


    if (!resumeInput.files.length) {

        alert("Please select a resume file.");

        return;
    }


    const file =
        resumeInput.files[0];


    console.log(
        "Resume:",
        file.name
    );


    // --------------------------------------------------
    // DISABLE BUTTON
    // --------------------------------------------------

    screenButton.disabled = true;

    screenButton.style.opacity = "0.6";

    result.style.display = "none";

    loading.style.display = "block";


    // --------------------------------------------------
    // FORM DATA
    // --------------------------------------------------

    const formData =
        new FormData();

    formData.append(
        "job",
        job
    );

    formData.append(
        "resume",
        file
    );


    console.log(
        "Sending request to:",
        API_URL
    );


    // --------------------------------------------------
    // START 10 SECOND DISPLAY
    // --------------------------------------------------

    const messages = [

        "Uploading resume...",

        "Reading candidate information...",

        "Extracting resume content...",

        "Analyzing technical skills...",

        "Comparing job requirements...",

        "Checking candidate experience...",

        "Checking education...",

        "Calculating match score...",

        "Generating screening decision...",

        "Finalizing result..."

    ];


    let progress = 0;

    const startTime =
        Date.now();

    const tenSeconds = 10000;


    const timer =
        setInterval(function () {

            const elapsed =
                Date.now() - startTime;

            progress =
                Math.min(
                    100,
                    Math.floor(
                        elapsed /
                        tenSeconds *
                        100
                    )
                );


            progressBar.style.width =
                progress + "%";

            progressPercent.textContent =
                progress + "%";


            const index =
                Math.min(
                    messages.length - 1,
                    Math.floor(progress / 10)
                );


            processingText.textContent =
                messages[index];


        }, 100);


    // --------------------------------------------------
    // SEND TO JAVA SERVER
    // --------------------------------------------------

    let response;

    let data;


    try {

        response =
            await fetch(
                API_URL,
                {
                    method: "POST",
                    body: formData
                }
            );


        console.log(
            "Server status:",
            response.status
        );


        const responseText =
            await response.text();


        console.log(
            "Server response:",
            responseText
        );


        if (!response.ok) {

            throw new Error(
                "Server returned " +
                response.status +
                ": " +
                responseText
            );
        }


        try {

            data =
                JSON.parse(
                    responseText
                );

        } catch (error) {

            throw new Error(
                "Java server did not return valid JSON:\n" +
                responseText
            );
        }


    } catch (error) {

        clearInterval(timer);

        loading.style.display =
            "none";

        screenButton.disabled =
            false;

        screenButton.style.opacity =
            "1";


        console.error(
            "SCREENING ERROR:",
            error
        );


        alert(
            "Screening failed:\n\n" +
            error.message
        );

        return;
    }


    // --------------------------------------------------
    // WAIT FOR FULL 10 SECONDS
    // --------------------------------------------------

    const elapsed =
        Date.now() - startTime;

    const remaining =
        Math.max(
            0,
            tenSeconds - elapsed
        );


    await sleep(
        remaining
    );


    clearInterval(timer);


    progressBar.style.width =
        "100%";

    progressPercent.textContent =
        "100%";

    processingText.textContent =
        "Analysis completed.";


    await sleep(500);


    // --------------------------------------------------
    // HIDE PROCESSING
    // --------------------------------------------------

    loading.style.display =
        "none";


    // --------------------------------------------------
    // SHOW RESULT
    // --------------------------------------------------

    showResult(data);


    // --------------------------------------------------
    // ENABLE BUTTON
    // --------------------------------------------------

    screenButton.disabled =
        false;

    screenButton.style.opacity =
        "1";
}


// ======================================================
// SHOW RESULT
// ======================================================

function showResult(data) {

    console.log(
        "Displaying result:",
        data
    );


    result.style.display =
        "block";


    // --------------------------------------------------
    // NAME
    // --------------------------------------------------

    candidateNameElement.textContent =
        data.candidateName ||
        "Candidate";


    // --------------------------------------------------
    // SCORE
    // --------------------------------------------------

    let score =
        Number(data.score);


    if (isNaN(score)) {

        score = 0;
    }


    scoreElement.textContent =
        score.toFixed(1) +
        "%";


    // --------------------------------------------------
    // DECISION
    // --------------------------------------------------

    const decision =
        data.decision ||
        (
            score >= 50
                ? "SHORTLISTED"
                : "NOT SHORTLISTED"
        );


    decisionElement.textContent =
        decision;


    // --------------------------------------------------
    // GREEN SHORTLIST
    // --------------------------------------------------

    decisionElement.classList.remove(
        "shortlisted",
        "not-shortlisted"
    );


    if (
        decision
            .toUpperCase()
            .trim() ===
        "SHORTLISTED"
    ) {

        decisionElement.classList.add(
            "shortlisted"
        );

    } else {

        decisionElement.classList.add(
            "not-shortlisted"
        );
    }


    // --------------------------------------------------
    // EXPERIENCE
    // --------------------------------------------------

    if (data.experience) {

        experienceElement.textContent =
            data.experience;

    } else {

        experienceElement.textContent =
            "Not specified";
    }


    // --------------------------------------------------
    // EDUCATION
    // --------------------------------------------------

    educationElement.textContent =
        data.education ||
        "See resume";


    // --------------------------------------------------
    // MATCHING SKILLS
    // --------------------------------------------------

    matchingSkillsElement.innerHTML =
        "";


    const matching =
        data.matchingSkills ||
        data.matchedSkills ||
        [];


    if (matching.length === 0) {

        matchingSkillsElement.innerHTML =
            "<li>No matching skills found</li>";

    } else {

        matching.forEach(function (skill) {

            const li =
                document.createElement("li");

            li.textContent =
                skill;

            matchingSkillsElement.appendChild(
                li
            );
        });
    }


    // --------------------------------------------------
    // MISSING SKILLS
    // --------------------------------------------------

    missingSkillsElement.innerHTML =
        "";


    const missing =
        data.missingSkills ||
        [];


    if (missing.length === 0) {

        missingSkillsElement.innerHTML =
            "<li>No major missing skills</li>";

    } else {

        missing.forEach(function (skill) {

            const li =
                document.createElement("li");

            li.textContent =
                skill;

            missingSkillsElement.appendChild(
                li
            );
        });
    }


    // --------------------------------------------------
    // SUMMARY
    // --------------------------------------------------

    summaryElement.textContent =
        data.summary ||
        "Candidate screening completed successfully.";


    // --------------------------------------------------
    // SCROLL TO RESULT
    // --------------------------------------------------

    result.scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}


// ======================================================
// SLEEP
// ======================================================

function sleep(ms) {

    return new Promise(
        function (resolve) {

            setTimeout(
                resolve,
                ms
            );
        }
    );
}