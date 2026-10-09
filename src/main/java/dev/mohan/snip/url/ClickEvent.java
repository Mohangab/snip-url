package dev.mohan.snip.url;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "click_events")
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(length = 512)
    private String referrer;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt;

    protected ClickEvent() {
    }

    public ClickEvent(String code, String referrer, String userAgent, Instant clickedAt) {
        this.code = code;
        this.referrer = referrer;
        this.userAgent = userAgent;
        this.clickedAt = clickedAt;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getReferrer() { return referrer; }
    public String getUserAgent() { return userAgent; }
    public Instant getClickedAt() { return clickedAt; }
}
