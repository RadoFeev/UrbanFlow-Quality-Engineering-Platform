package io.urbanflow.testcore.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class DatabaseClientTest {

  @Test
  void executesParameterizedUpdate() throws Exception {
    Connection connection = mock(Connection.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    when(connection.prepareStatement("DELETE FROM test_items WHERE id = ?")).thenReturn(statement);
    when(statement.executeUpdate()).thenReturn(1);

    int count =
        new DatabaseClient(() -> connection).update("DELETE FROM test_items WHERE id = ?", 42);

    assertThat(count).isEqualTo(1);
    verify(statement).setObject(1, 42);
    verify(statement).close();
    verify(connection).close();
  }

  @Test
  void mapsImmutableQueryResults() throws Exception {
    Connection connection = mock(Connection.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    ResultSet results = mock(ResultSet.class);
    when(connection.prepareStatement("SELECT name FROM test_items")).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(results);
    when(results.next()).thenReturn(true, true, false);
    when(results.getString("name")).thenReturn("one", "two");

    List<String> values =
        new DatabaseClient(() -> connection)
            .query("SELECT name FROM test_items", row -> row.getString("name"));

    assertThat(values).containsExactly("one", "two");
    assertThatThrownBy(() -> values.add("three")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void commitsSuccessfulTransactionAndRestoresAutoCommit() throws Exception {
    Connection connection = mock(Connection.class);
    when(connection.getAutoCommit()).thenReturn(true);

    String value = new DatabaseClient(() -> connection).transaction(ignored -> "committed");

    assertThat(value).isEqualTo("committed");
    verify(connection).setAutoCommit(false);
    verify(connection).commit();
    verify(connection).setAutoCommit(true);
  }

  @Test
  void rollsBackFailedTransaction() throws Exception {
    Connection connection = mock(Connection.class);
    when(connection.getAutoCommit()).thenReturn(true);

    assertThatThrownBy(
            () ->
                new DatabaseClient(() -> connection)
                    .transaction(
                        ignored -> {
                          throw new IllegalStateException("boom");
                        }))
        .isInstanceOf(DatabaseClient.DatabaseOperationException.class)
        .hasMessageContaining("transaction failed");
    verify(connection).rollback();
  }

  @Test
  void detectsBrokenUniquenessAssumption() throws Exception {
    Connection connection = mock(Connection.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    ResultSet results = mock(ResultSet.class);
    when(connection.prepareStatement("SELECT id FROM items")).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(results);
    when(results.next()).thenReturn(true, true, false);
    when(results.getInt("id")).thenReturn(1, 2);

    assertThatThrownBy(
            () ->
                new DatabaseClient(() -> connection)
                    .queryOne("SELECT id FROM items", row -> row.getInt("id")))
        .isInstanceOf(DatabaseClient.DatabaseOperationException.class)
        .hasMessageContaining("more than one row");
  }
}
