package uv.naloge.prvaNaloga;

public record Reservation(String participantName, String classType, String classTime) {

    @Override
    public String toString() {
        return participantName + "; " + classType + "; " + classTime;
    }
}

