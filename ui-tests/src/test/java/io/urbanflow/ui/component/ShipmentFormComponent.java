package io.urbanflow.ui.component;

import io.qameta.allure.Step;
import io.urbanflow.ui.support.UiWait;
import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/** Component object for the customer shipment form and its validation output. */
public final class ShipmentFormComponent {

  private final WebDriver driver;
  private final UiWait wait;

  /** Creates a shipment-form component. */
  public ShipmentFormComponent(WebDriver driver, Duration timeout) {
    this.driver = driver;
    this.wait = new UiWait(driver, timeout);
  }

  /** Fills all shipment inputs without submitting. */
  @Step("Fill shipment form")
  public ShipmentFormComponent fill(ShipmentInput input) {
    replace("pickup-address", input.pickup());
    replace("delivery-address", input.delivery());
    replace("recipient-phone", input.phone());
    new Select(element("delivery-type")).selectByValue(input.deliveryType());
    replace("parcel-weight", input.weight());
    replace("parcel-length", input.length());
    replace("parcel-width", input.width());
    replace("parcel-height", input.height());
    replace("parcel-description", input.description());
    return this;
  }

  /** Submits the form once. */
  @Step("Submit shipment form")
  public ShipmentFormComponent submit() {
    wait.clickable(testId("create-shipment-submit")).click();
    return this;
  }

  /** Returns the current validation or success message. */
  public String message() {
    return element("shipment-message").getText();
  }

  /** Returns the computed delivery estimate. */
  public String estimate() {
    return element("delivery-estimate").getText();
  }

  /** Changes delivery type and waits until an estimate is rendered. */
  public ShipmentFormComponent deliveryType(String type) {
    new Select(element("delivery-type")).selectByValue(type);
    wait.text(testId("delivery-estimate"), "Estimated delivery:");
    return this;
  }

  /** Returns a field's DOM value after browser-native length constraints. */
  public String value(String testId) {
    return element(testId).getAttribute("value");
  }

  /** Returns the browser-native constraint validation message for an input. */
  public String validationMessage(String testId) {
    return element(testId).getAttribute("validationMessage");
  }

  private void replace(String id, String value) {
    WebElement element = element(id);
    element.clear();
    element.sendKeys(value == null ? "" : value);
  }

  private WebElement element(String id) {
    return wait.visible(testId(id));
  }

  private By testId(String id) {
    return By.cssSelector("[data-testid='" + id + "']");
  }

  /** Immutable input boundary for data-driven shipment tests. */
  public record ShipmentInput(
      String pickup,
      String delivery,
      String phone,
      String deliveryType,
      String weight,
      String length,
      String width,
      String height,
      String description) {

    /** Returns a representative valid shipment. */
    public static ShipmentInput valid() {
      return new ShipmentInput(
          "1 Test Street, Sofia",
          "2 Quality Avenue, Varna",
          "+359888123456",
          "STANDARD",
          "2.5",
          "30",
          "20",
          "15",
          "Books and test fixtures");
    }
  }
}
