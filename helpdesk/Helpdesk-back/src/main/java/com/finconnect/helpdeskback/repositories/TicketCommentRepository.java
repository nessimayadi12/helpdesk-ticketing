package com.finconnect.helpdeskback.repositories;

import com.finconnect.helpdeskback.entities.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {
    @Query("select c from TicketComment c where c.ticket.id = :ticketId order by c.createdAt asc")
    List<TicketComment> findByTicketIdOrderByCreatedAtAsc(@Param("ticketId") Long ticketId);

    @Query("select c.ticket.id, count(c) from TicketComment c where c.ticket.id in :ids group by c.ticket.id")
    List<Object[]> countByTicketIds(@Param("ids") List<Long> ids);
}
