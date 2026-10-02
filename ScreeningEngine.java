import java.util.ArrayList;
import java.util.List;

public class ScreeningEngine {


    public ScreeningResult screen(
            String candidateName,
            String resumeText,
            List<String> requiredSkills,
            int requiredExperience) {


        String normalizedResume =
                normalize(resumeText);


        List<String> matchedSkills =
                new ArrayList<>();


        List<String> missingSkills =
                new ArrayList<>();


        // =====================================================
        // CHECK SKILLS
        // =====================================================

        for (String skill :
                requiredSkills) {


            if (normalizedResume.contains(
                    normalize(skill))) {

                matchedSkills.add(skill);

            } else {

                missingSkills.add(skill);
            }
        }


        // =====================================================
        // SKILL SCORE
        // =====================================================

        double skillScore = 0;


        if (!requiredSkills.isEmpty()) {

            skillScore =
                    ((double) matchedSkills.size()
                            / requiredSkills.size())
                            * 100;
        }


        // =====================================================
        // EXPERIENCE
        // =====================================================

        int detectedExperience =
                detectExperience(
                        normalizedResume
                );


        double experienceScore;


        if (requiredExperience == 0) {

            experienceScore = 100;

        } else if (
                detectedExperience >= requiredExperience
        ) {

            experienceScore = 100;

        } else {

            experienceScore =
                    ((double) detectedExperience
                            / requiredExperience)
                            * 100;
        }


        // =====================================================
        // FINAL SCORE
        // =====================================================

        double finalScore =
                (skillScore * 0.70)
                        +
                (experienceScore * 0.30);


        // =====================================================
        // DECISION
        // =====================================================

        String decision;


        /*
         * Example threshold:
         *
         * 70 or above = SHORTLISTED
         * below 70    = NOT SHORTLISTED
         */

        if (finalScore >= 29) {

            decision = "SHORTLISTED";

        } else {

            decision = "NOT SHORTLISTED";
        }


        return new ScreeningResult(
                candidateName,
                finalScore,
                matchedSkills,
                missingSkills,
                detectedExperience,
                requiredExperience,
                decision
        );
    }


    // =========================================================
    // EXPERIENCE DETECTION
    // =========================================================

    private int detectExperience(
            String text) {


        java.util.regex.Pattern pattern =
                java.util.regex.Pattern.compile(
                        "(\\d+)\\s*\\+?\\s*(years?|yrs?)"
                );


        java.util.regex.Matcher matcher =
                pattern.matcher(text);


        int highest = 0;


        while (matcher.find()) {

            try {

                int years =
                        Integer.parseInt(
                                matcher.group(1)
                        );


                if (years > highest) {

                    highest = years;
                }

            } catch (Exception ignored) {
            }
        }


        return highest;
    }


    private String normalize(String text) {

        return text
                .toLowerCase()
                .replaceAll(
                        "[^a-z0-9+#.]+",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }
}
