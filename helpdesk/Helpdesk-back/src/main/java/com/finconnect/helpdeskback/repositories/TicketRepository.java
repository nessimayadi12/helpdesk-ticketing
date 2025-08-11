package com.finconnect.helpdeskback.repositories;

import com.finconnect.helpdeskback.entities.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
	List<Ticket> findByUser_Username(String username);
}
