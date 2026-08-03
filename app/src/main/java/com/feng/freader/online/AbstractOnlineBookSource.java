package com.feng.freader.online;

public abstract class AbstractOnlineBookSource implements OnlineBookSource {
    private final String id;
    private final String name;
    private final SourceType type;
    private final String baseUrl;
    private final String licenseNote;
    private boolean enabled;

    protected AbstractOnlineBookSource(String id, String name, SourceType type,
                                       String baseUrl, String licenseNote, boolean enabled) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.baseUrl = baseUrl;
        this.licenseNote = licenseNote;
        this.enabled = enabled;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public SourceType getType() {
        return type;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public String getLicenseNote() {
        return licenseNote;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
