package dev.mohan.snip.url;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    @Query("select coalesce(c.referrer, 'direct'), count(c) from ClickEvent c where c.code = :code "
            + "group by c.referrer order by count(c) desc")
    List<Object[]> countByReferrer(String code);

    @Query("select coalesce(c.userAgent, 'unknown'), count(c) from ClickEvent c where c.code = :code "
            + "group by c.userAgent order by count(c) desc")
    List<Object[]> countByUserAgent(String code);
}
