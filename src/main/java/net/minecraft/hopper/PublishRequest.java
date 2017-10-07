package net.minecraft.hopper;

public class PublishRequest {

    public PublishRequest(final Report report) {
        int report_id = report.getId();
        String token = report.getToken();
    }
}
