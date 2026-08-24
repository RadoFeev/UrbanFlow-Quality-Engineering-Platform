package io.urbanflow.warehouse.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditRecordTest {

  private static final Instant NOW = Instant.parse("2026-08-24T10:00:00Z");

  @Test
  void createsSanitizedRecord() {
    UUID actor = UUID.randomUUID();
    UUID entity = UUID.randomUUID();
    AuditRecord record =
        AuditRecord.create(
            UUID.randomUUID(), actor, " UPDATE ", "Warehouse", entity, "a\r\nb\tc", NOW);

    assertThat(record.actorId()).isEqualTo(actor);
    assertThat(record.action()).isEqualTo("UPDATE");
    assertThat(record.entityType()).isEqualTo("Warehouse");
    assertThat(record.entityId()).isEqualTo(entity);
    assertThat(record.details()).isEqualTo("a  b c");
    assertThat(record.occurredAt()).isEqualTo(NOW);
  }

  @Test
  void rejectsMissingActor() {
    assertThatThrownBy(
            () ->
                AuditRecord.create(
                    UUID.randomUUID(), null, "UPDATE", "Warehouse", UUID.randomUUID(), "x", NOW))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsBlankAction() {
    assertThatThrownBy(
            () ->
                AuditRecord.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    " ",
                    "Warehouse",
                    UUID.randomUUID(),
                    "x",
                    NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsMissingEntity() {
    assertThatThrownBy(
            () ->
                AuditRecord.create(
                    UUID.randomUUID(), UUID.randomUUID(), "UPDATE", "Warehouse", null, "x", NOW))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsOverlongDetails() {
    assertThatThrownBy(
            () ->
                AuditRecord.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "UPDATE",
                    "Warehouse",
                    UUID.randomUUID(),
                    "x".repeat(501),
                    NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
