package com.itson.notificarservice.modelo;

import com.itson.escuela.common.modelo.TipoAlerta;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "alerta_rendimiento", indexes = @Index(name = "idx_alerta_tutor", columnList = "idTutor"))
public class AlertaRendimiento extends PanacheEntityBase {

    @Id
    public String idAlerta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TipoAlerta tipo;

    @Column(nullable = false)
    public String matricula;

    @Column(nullable = false)
    public String idTutor;

    @Column(nullable = false, length = 500)
    public String mensaje;

    @Column(nullable = false)
    public Instant fechaEmision;

    public boolean leida;

    @Column(nullable = false)
    public String referencia;

    public static AlertaRendimiento nueva(TipoAlerta tipo, String matricula, String idTutor, String mensaje,
            String referencia) {
        AlertaRendimiento alerta = new AlertaRendimiento();
        alerta.idAlerta = "AL-" + UUID.randomUUID().toString().substring(0, 8);
        alerta.tipo = tipo;
        alerta.matricula = matricula;
        alerta.idTutor = idTutor;
        alerta.mensaje = mensaje;
        alerta.referencia = referencia;
        alerta.fechaEmision = Instant.now();
        alerta.leida = false;
        return alerta;
    }

    public static boolean existe(TipoAlerta tipo, String matricula, String idTutor, String referencia) {
        return count("tipo = ?1 and matricula = ?2 and idTutor = ?3 and referencia = ?4",
                tipo, matricula, idTutor, referencia) > 0;
    }

    public static List<AlertaRendimiento> buzon(String idTutor, String matricula, boolean soloNoLeidas) {
        StringBuilder consulta = new StringBuilder("idTutor = :idTutor");
        Parameters parametros = Parameters.with("idTutor", idTutor);
        if (matricula != null && !matricula.isBlank()) {
            consulta.append(" and matricula = :matricula");
            parametros.and("matricula", matricula);
        }
        if (soloNoLeidas) {
            consulta.append(" and leida = false");
        }
        return list(consulta.toString(), Sort.descending("fechaEmision", "idAlerta"), parametros);
    }

    public static long contarNoLeidas(String idTutor) {
        return count("idTutor = ?1 and leida = false", idTutor);
    }
}
