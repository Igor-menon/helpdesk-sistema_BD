package br.com.helpdesk.repository;

import br.com.helpdesk.model.Chamado;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

    @Query("""
            SELECT c
            FROM Chamado c
            JOIN FETCH c.solicitante
            ORDER BY c.id DESC
            """)
    List<Chamado> listarComSolicitante();

    @Query(value = """
            SELECT status, COUNT(*) AS quantidade
            FROM chamados
            GROUP BY status
            ORDER BY status
            """, nativeQuery = true)
    List<ResumoStatus> contarPorStatus();

    interface ResumoStatus {
        String getStatus();
        Long getQuantidade();
    }
}