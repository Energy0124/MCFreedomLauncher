package net.minecraft.launcher.versions;

import java.util.Date;

public class PartialVersion implements Version
{
    private String id;
    private Date time;
    private Date releaseTime;
    private ReleaseType type;
    
    public PartialVersion() {
        super();
    }
    
    public PartialVersion(final String id, final Date releaseTime, final Date updateTime, final ReleaseType type) {
        super();
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }
        if (releaseTime == null) {
            throw new IllegalArgumentException("Release time cannot be null");
        }
        if (updateTime == null) {
            throw new IllegalArgumentException("Update time cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Release type cannot be null");
        }
        this.id = id;
        this.releaseTime = releaseTime;
        this.time = updateTime;
        this.type = type;
    }
    
    public PartialVersion(final Version version) {
        this(version.getId(), version.getReleaseTime(), version.getUpdatedTime(), version.getType());
    }
    
    @Override
    public String getId() {
        return this.id;
    }
    
    @Override
    public ReleaseType getType() {
        return this.type;
    }
    
    @Override
    public Date getUpdatedTime() {
        return this.time;
    }
    
    @Override
    public void setUpdatedTime(final Date time) {
        if (time == null) {
            throw new IllegalArgumentException("Time cannot be null");
        }
        this.time = time;
    }
    
    @Override
    public Date getReleaseTime() {
        return this.releaseTime;
    }
    
    @Override
    public void setReleaseTime(final Date time) {
        if (time == null) {
            throw new IllegalArgumentException("Time cannot be null");
        }
        this.releaseTime = time;
    }
    
    @Override
    public void setType(final ReleaseType type) {
        if (type == null) {
            throw new IllegalArgumentException("Release type cannot be null");
        }
        this.type = type;
    }
    
    @Override
    public String toString() {
        return "PartialVersion{id='" + this.id + '\'' + ", updateTime=" + this.time + ", releaseTime=" + this.releaseTime + ", type=" + this.type + '}';
    }
}
