package net.minecraft.hopper;

public class SubmitResponse
        extends Response {
    private Report report;
    private Crash crash;
    private Problem problem;

    public Report getReport() {
        return this.report;
    }

    public Crash getCrash() {
        return this.crash;
    }

    public Problem getProblem() {
        return this.problem;
    }
}