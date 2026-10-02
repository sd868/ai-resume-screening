import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JobDescriptionParser {

    private final List<String> knownSkills = Arrays.asList(
            "java",
            "python",
            "c",
            "c++",
            "c#",
            "javascript",
            "typescript",

            "sql",
            "mysql",
            "postgresql",
            "mongodb",
            "oracle",
            "sqlite",

            "spring",
            "spring boot",
            "hibernate",

            "flask",
            "django",

            "rest",
            "rest api",
            "api",

            "docker",
            "kubernetes",

            "aws",
            "azure",
            "gcp",

            "git",
            "github",

            "html",
            "css",

            "react",
            "angular",
            "vue",

            "machine learning",
            "artificial intelligence",
            "ai",
            "nlp",
            "natural language processing",

            "tensorflow",
            "pytorch",
            "scikit-learn",

            "numpy",
            "pandas",

            "excel",
            "power bi"
    );

    public JobDescriptionParser() {
    }

    public List<String> extractSkills(String jobDescription) {

        List<String> skills = new ArrayList<>();

        if (jobDescription == null ||
                jobDescription.trim().isEmpty()) {
            return skills;
        }

        String text = normalize(jobDescription);

        for (String skill : knownSkills) {

            String normalizedSkill = normalize(skill);

            if (text.contains(normalizedSkill)) {

                if (!skills.contains(skill)) {
                    skills.add(skill);
                }
            }
        }

        return skills;
    }

    public int extractExperience(String jobDescription) {

        if (jobDescription == null) {
            return 0;
        }

        Pattern pattern = Pattern.compile(
                "(\\d+)\\s*\\+?\\s*(years?|yrs?)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher =
                pattern.matcher(jobDescription);

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

            } catch (NumberFormatException ignored) {
            }
        }

        return highest;
    }

    private String normalize(String text) {

        if (text == null) {
            return "";
        }

        return text
                .toLowerCase()
                .replaceAll("[^a-z0-9+#.]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}