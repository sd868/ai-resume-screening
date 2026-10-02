import java.util.List;

public class ScreeningResult {

    private final String candidateName;
    private final double score;
    private final List<String> matchedSkills;
    private final List<String> missingSkills;
    private final int detectedExperience;
    private final int requiredExperience;
    private final String decision;

    public ScreeningResult(
            String candidateName,
            double score,
            List<String> matchedSkills,
            List<String> missingSkills,
            int detectedExperience,
            int requiredExperience,
            String decision) {

        this.candidateName = candidateName;
        this.score = score;
        this.matchedSkills = matchedSkills;
        this.missingSkills = missingSkills;
        this.detectedExperience = detectedExperience;
        this.requiredExperience = requiredExperience;
        this.decision = decision;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public double getScore() {
        return score;
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    public List<String> getMissingSkills() {
        return missingSkills;
    }

    public int getDetectedExperience() {
        return detectedExperience;
    }

    public int getRequiredExperience() {
        return requiredExperience;
    }

    public String getDecision() {
        return decision;
    }
}