package io.urbanflow.ui.support;

import io.qameta.allure.Step;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import tools.jackson.databind.json.JsonMapper;

/** Small fluent foundation for page and component objects without PageFactory or implicit waits. */
public abstract class BasePage<T extends BasePage<T>> {

  protected final WebDriver driver;
  protected final UiWait wait;
  private final URI baseUrl;
  private final JsonMapper json = JsonMapper.builder().build();

  /** Creates a page bound to one thread-confined browser session. */
  protected BasePage(WebDriver driver, URI baseUrl, Duration timeout) {
    this.driver = driver;
    this.baseUrl = baseUrl;
    this.wait = new UiWait(driver, timeout);
  }

  /** Opens the page and clears browser-owned test state. */
  @Step("Open a fresh portal session")
  public T openFresh() {
    driver.get(baseUrl.toString());
    execute("window.localStorage.clear(); window.sessionStorage.clear();");
    driver.navigate().refresh();
    wait.visible(By.tagName("body"));
    return self();
  }

  /** Replaces one local-storage document and reloads the application. */
  @Step("Seed isolated portal state")
  public T seed(String storageKey, Object value) {
    execute(
        "window.localStorage.setItem(arguments[0], arguments[1]);",
        storageKey,
        json.writeValueAsString(value));
    driver.navigate().refresh();
    wait.visible(By.tagName("body"));
    return self();
  }

  /** Writes raw local-storage content to exercise browser-side recovery behavior. */
  public T rawState(String storageKey, String value) {
    execute("window.localStorage.setItem(arguments[0], arguments[1]);", storageKey, value);
    driver.navigate().refresh();
    wait.visible(By.tagName("body"));
    return self();
  }

  /** Returns the parsed local-storage document as text. */
  public String storage(String storageKey) {
    return (String) execute("return window.localStorage.getItem(arguments[0]);", storageKey);
  }

  /** Builds a locator from a stable public test identifier. */
  protected By testId(String value) {
    return By.cssSelector("[data-testid='" + value + "']");
  }

  /** Clicks a stable test identifier after it becomes interactive. */
  protected T click(String value) {
    wait.clickable(testId(value)).click();
    return self();
  }

  /** Replaces text in a stable test-identifier field. */
  protected T type(String value, String text) {
    WebElement element = wait.visible(testId(value));
    element.clear();
    element.sendKeys(text == null ? "" : text);
    return self();
  }

  /** Replaces text in an explicitly located field. */
  protected T type(By locator, String text) {
    WebElement element = wait.visible(locator);
    element.clear();
    element.sendKeys(text == null ? "" : text);
    return self();
  }

  /** Selects an option by its contract value. */
  protected T select(String value, String option) {
    new Select(wait.visible(testId(value))).selectByValue(option);
    return self();
  }

  /** Selects an option by its visible user label. */
  protected T selectText(String value, String option) {
    new Select(wait.visible(testId(value))).selectByVisibleText(option);
    return self();
  }

  /** Returns visible text from a stable test identifier. */
  public String text(String value) {
    return wait.visible(testId(value)).getText();
  }

  /** Returns visible text from an explicit locator. */
  public String text(By locator) {
    return wait.visible(locator).getText();
  }

  /** Waits until an element contains the expected text. */
  public T awaitText(String value, String expected) {
    wait.text(testId(value), expected);
    return self();
  }

  /** Returns the current value of a stable form control. */
  public String value(String value) {
    return wait.visible(testId(value)).getAttribute("value");
  }

  /** Returns an attribute from a stable test identifier. */
  public String attribute(String value, String attribute) {
    return wait.visible(testId(value)).getAttribute(attribute);
  }

  /** Returns whether an element exists and is visible. */
  public boolean displayed(String value) {
    List<WebElement> elements = driver.findElements(testId(value));
    return !elements.isEmpty() && elements.getFirst().isDisplayed();
  }

  /** Returns whether an element is enabled for user interaction. */
  public boolean enabled(String value) {
    return wait.visible(testId(value)).isEnabled();
  }

  /** Counts all current elements matching an explicit locator. */
  public int count(By locator) {
    return driver.findElements(locator).size();
  }

  /** Resizes the current browser viewport. */
  public void resize(int width, int height) {
    driver.manage().window().setSize(new org.openqa.selenium.Dimension(width, height));
  }

  /** Executes a page-scoped JavaScript operation. */
  protected Object execute(String script, Object... arguments) {
    return ((JavascriptExecutor) driver).executeScript(script, arguments);
  }

  /** Returns the concrete fluent page instance. */
  protected abstract T self();
}
