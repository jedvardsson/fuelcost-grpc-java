package io.github.jedvardsson.fuelcost.db;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class DbClient {

    private final Jdbi jdbi;

    public DbClient(Jdbi jdbi, ObjectProvider<FlywayMigrationInitializer> flywayInitializer) {
        // conditional depends-on flyway if enabled
        FlywayMigrationInitializer ignore = flywayInitializer.getIfAvailable();
        this.jdbi = jdbi;
    }

    public <R> R withHandle(Function<Handle, ? extends R> action) {
        return jdbi.withHandle(action::apply);
    }
}