package io.urbanflow.testcore.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Prepared-statement database client that opens an isolated connection per operation. */
public final class DatabaseClient {

  private final ConnectionFactory connections;

  /** Creates a database client from an injectable connection factory. */
  public DatabaseClient(ConnectionFactory connections) {
    this.connections = connections;
  }

  /** Executes a parameterized update and returns affected row count. */
  public int update(String sql, Object... parameters) {
    try (Connection connection = connections.open();
        PreparedStatement statement = prepare(connection, sql, parameters)) {
      return statement.executeUpdate();
    } catch (SQLException exception) {
      throw new DatabaseOperationException("database update failed", exception);
    }
  }

  /** Executes a query and maps every row before closing JDBC resources. */
  public <T> List<T> query(String sql, RowMapper<T> mapper, Object... parameters) {
    try (Connection connection = connections.open();
        PreparedStatement statement = prepare(connection, sql, parameters);
        ResultSet results = statement.executeQuery()) {
      List<T> values = new ArrayList<>();
      while (results.next()) {
        values.add(mapper.map(results));
      }
      return List.copyOf(values);
    } catch (SQLException exception) {
      throw new DatabaseOperationException("database query failed", exception);
    }
  }

  /** Returns zero or one row, failing if a uniqueness assumption is broken. */
  public <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... parameters) {
    List<T> values = query(sql, mapper, parameters);
    if (values.size() > 1) {
      throw new DatabaseOperationException("query returned more than one row");
    }
    return values.stream().findFirst();
  }

  /** Runs work within a transaction, rolling back on any failure. */
  public <T> T transaction(TransactionWork<T> work) {
    try (Connection connection = connections.open()) {
      boolean originalAutoCommit = connection.getAutoCommit();
      connection.setAutoCommit(false);
      try {
        T result = work.execute(connection);
        connection.commit();
        return result;
      } catch (Exception exception) {
        connection.rollback();
        throw new DatabaseOperationException("database transaction failed", exception);
      } finally {
        connection.setAutoCommit(originalAutoCommit);
      }
    } catch (SQLException exception) {
      throw new DatabaseOperationException("database transaction setup failed", exception);
    }
  }

  private static PreparedStatement prepare(Connection connection, String sql, Object[] parameters)
      throws SQLException {
    if (sql == null || sql.isBlank()) {
      throw new IllegalArgumentException("SQL must not be blank");
    }
    PreparedStatement statement = connection.prepareStatement(sql);
    for (int index = 0; index < parameters.length; index++) {
      statement.setObject(index + 1, parameters[index]);
    }
    return statement;
  }

  /** Opens a new JDBC connection owned by one client operation. */
  @FunctionalInterface
  public interface ConnectionFactory {

    /** Opens a connection. */
    Connection open() throws SQLException;
  }

  /** Maps the current JDBC result row. */
  @FunctionalInterface
  public interface RowMapper<T> {

    /** Maps a result row. */
    T map(ResultSet result) throws SQLException;
  }

  /** Executes work against one transactional connection. */
  @FunctionalInterface
  public interface TransactionWork<T> {

    /** Executes the transaction body. */
    T execute(Connection connection) throws Exception;
  }

  /** Normalizes checked JDBC failures. */
  public static class DatabaseOperationException extends RuntimeException {

    /** Creates a database operation failure. */
    public DatabaseOperationException(String message, Throwable cause) {
      super(message, cause);
    }

    /** Creates a database invariant failure. */
    public DatabaseOperationException(String message) {
      super(message);
    }
  }
}
