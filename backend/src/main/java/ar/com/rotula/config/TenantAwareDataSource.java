package ar.com.rotula.config;

import ar.com.rotula.security.TenantContextHolder;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Envuelve el DataSource de Hikari para ejecutar SET app.current_tenant antes de
 * cada borrow de conexión. Esto garantiza que RLS filtre correctamente por tenant
 * en todos los accesos a la base de datos autenticados.
 *
 * Si no hay tenant en el contexto (ej: endpoints de auth), se establece el UUID nulo
 * como sentinel (sin filas visibles por RLS). El AuthService desactiva explícitamente
 * row_security para sus operaciones de autenticación.
 */
public class TenantAwareDataSource extends DelegatingDataSource implements DisposableBean {

    private static final String NULL_TENANT = "00000000-0000-0000-0000-000000000000";

    public TenantAwareDataSource(DataSource delegate) {
        super(delegate);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection conn = super.getConnection();
        applyTenantContext(conn);
        return conn;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection conn = super.getConnection(username, password);
        applyTenantContext(conn);
        return conn;
    }

    private void applyTenantContext(Connection conn) throws SQLException {
        UUID tenantId = TenantContextHolder.getTenantId();
        // UUID.toString() only contains hex chars and dashes — no injection risk
        String tenantValue = tenantId != null ? tenantId.toString() : NULL_TENANT;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET app.current_tenant = '" + tenantValue + "'");
        } catch (SQLException e) {
            conn.close();
            throw e;
        }
    }

    @Override
    public void destroy() throws Exception {
        DataSource target = getTargetDataSource();
        if (target instanceof AutoCloseable ac) {
            ac.close();
        }
    }
}
